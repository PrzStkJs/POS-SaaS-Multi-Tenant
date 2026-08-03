/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Analítica y Dashboard
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de dashboard.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.service;

import com.BarCodeSys.pos.dto.DashboardDTO;
import com.BarCodeSys.pos.dto.GraficoResponseDTO;
import com.BarCodeSys.pos.dto.StockCriticoDTO;
import com.BarCodeSys.pos.dto.TopProductoDTO;
import com.BarCodeSys.pos.models.QDetalleVenta;
import com.BarCodeSys.pos.models.QProducto;
import com.BarCodeSys.pos.models.QVenta;
import com.BarCodeSys.pos.models.EstadoVenta;
import com.BarCodeSys.pos.security.SecurityUtils;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.core.Tuple;

// Importaciones profesionales
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de DashboardService.

@Service
@Transactional(readOnly = true) // PRO-TIP: Optimiza las consultas de solo lectura en la BD
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    
    // PRO-TIP: Cubrimos ambas variaciones para evitar que un error de tipeo/tilde arruine el reporte
    private static final String COMODIN_CON_TILDE = "VENTA RÁPIDA";
    private static final String COMODIN_SIN_TILDE = "VENTA RAPIDA";

    // Dependencia inmutable
    private final JPAQueryFactory queryFactory;

    // Inyección por constructor
    public DashboardService(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    // =================================================================================
    // 1. KPIs PRINCIPALES
    // =================================================================================
    public DashboardDTO obtenerKpis(LocalDateTime inicio, LocalDateTime fin) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        QVenta qVenta = QVenta.venta;
        QProducto qProducto = QProducto.producto;

        BigDecimal totalVentas = queryFactory
                .select(qVenta.total.sum())
                .from(qVenta)
                .where(qVenta.empresa.id.eq(empresaId)
                        .and(qVenta.fechaHora.between(inicio, fin))
                        .and(qVenta.estado.eq(EstadoVenta.COMPLETADA)))
                .fetchOne();
        totalVentas = (totalVentas != null) ? totalVentas : BigDecimal.ZERO;

        Long transacciones = queryFactory
                .select(qVenta.count())
                .from(qVenta)
                .where(qVenta.empresa.id.eq(empresaId)
                        .and(qVenta.fechaHora.between(inicio, fin))
                        .and(qVenta.estado.eq(EstadoVenta.COMPLETADA)))
                .fetchOne();
        transacciones = (transacciones != null) ? transacciones : 0L;

        BigDecimal ticketPromedio = BigDecimal.ZERO;
        if (transacciones > 0) {
            ticketPromedio = totalVentas.divide(new BigDecimal(transacciones), 2, RoundingMode.HALF_UP);
        }

        Long stockCritico = queryFactory
                .select(qProducto.count())
                .from(qProducto)
                .where(qProducto.empresa.id.eq(empresaId)
                        .and(qProducto.activo.isTrue())
                        .and(qProducto.stockActual.loe(10))
                        .and(qProducto.nombre.containsIgnoreCase(COMODIN_CON_TILDE).not())
                        .and(qProducto.nombre.containsIgnoreCase(COMODIN_SIN_TILDE).not()))
                .fetchOne();
        stockCritico = (stockCritico != null) ? stockCritico : 0L;

        return new DashboardDTO(totalVentas, transacciones.intValue(), ticketPromedio, stockCritico.intValue());
    }

    // =================================================================================
    // 2. GRÁFICO DE VENTAS 
    // =================================================================================
    public GraficoResponseDTO obtenerDatosGrafico(LocalDateTime inicioDia, LocalDateTime finDia, LocalDate fechaInicio, LocalDate fechaFin) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        QVenta qVenta = QVenta.venta;
        boolean esUnSoloDia = fechaInicio.equals(fechaFin);

        List<String> etiquetas = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();

        LocalDateTime inicioRango = fechaInicio.atStartOfDay();
        LocalDateTime finRango = fechaFin.atTime(LocalTime.MAX);

        List<Tuple> ventasDelRango = queryFactory
                .select(qVenta.fechaHora, qVenta.total)
                .from(qVenta)
                .where(qVenta.empresa.id.eq(empresaId)
                        .and(qVenta.fechaHora.between(inicioRango, finRango))
                        .and(qVenta.estado.eq(EstadoVenta.COMPLETADA)))
                .fetch();

        if (esUnSoloDia) {
            Map<Integer, BigDecimal> sumaPorHora = new HashMap<>();
            for (Tuple fila : ventasDelRango) {
                int hora = fila.get(qVenta.fechaHora).getHour();
                BigDecimal total = fila.get(qVenta.total);
                if (total == null) total = BigDecimal.ZERO;
                sumaPorHora.put(hora, sumaPorHora.getOrDefault(hora, BigDecimal.ZERO).add(total));
            }
            for (int hora = 0; hora < 24; hora++) {
                etiquetas.add(String.format("%02d:00", hora));
                valores.add(sumaPorHora.getOrDefault(hora, BigDecimal.ZERO));
            }
        } else {
            Map<LocalDate, BigDecimal> sumaPorDia = new HashMap<>();
            for (Tuple fila : ventasDelRango) {
                LocalDate fecha = fila.get(qVenta.fechaHora).toLocalDate();
                BigDecimal total = fila.get(qVenta.total);
                if (total == null) total = BigDecimal.ZERO;
                sumaPorDia.put(fecha, sumaPorDia.getOrDefault(fecha, BigDecimal.ZERO).add(total));
            }
            LocalDate fechaCursor = fechaInicio;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM");
            while (!fechaCursor.isAfter(fechaFin)) {
                etiquetas.add(fechaCursor.format(fmt));
                valores.add(sumaPorDia.getOrDefault(fechaCursor, BigDecimal.ZERO));
                fechaCursor = fechaCursor.plusDays(1);
            }
        }

        return new GraficoResponseDTO(etiquetas, valores);
    }

    // =================================================================================
    // 3. TOP PRODUCTOS MÁS VENDIDOS
    // =================================================================================
    public List<TopProductoDTO> obtenerTopVentas(LocalDateTime inicio, LocalDateTime fin, int limite) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        QDetalleVenta qDetalle = QDetalleVenta.detalleVenta;

        return queryFactory
                .select(Projections.constructor(TopProductoDTO.class,
                        qDetalle.producto.nombre,
                        qDetalle.producto.categoria.nombre,
                        qDetalle.cantidad.sum()
                ))
                .from(qDetalle)
                .join(qDetalle.venta)
                .where(qDetalle.venta.empresa.id.eq(empresaId)
                        .and(qDetalle.venta.fechaHora.between(inicio, fin))
                        .and(qDetalle.venta.estado.eq(EstadoVenta.COMPLETADA))
                        // EXCLUSIÓN QUERYDSL: Ocultar del ranking
                        .and(qDetalle.producto.nombre.containsIgnoreCase(COMODIN_CON_TILDE).not())
                        .and(qDetalle.producto.nombre.containsIgnoreCase(COMODIN_SIN_TILDE).not()))
                .groupBy(qDetalle.producto.id,
                        qDetalle.producto.nombre,
                        qDetalle.producto.categoria.nombre)
                .orderBy(qDetalle.cantidad.sum().desc())
                .limit(limite)
                .fetch();
    }

    // =================================================================================
    // 4. ALERTAS DE STOCK CRÍTICO
    // =================================================================================
    public List<StockCriticoDTO> obtenerStockCritico() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        QProducto qProducto = QProducto.producto;

        return queryFactory
                .select(Projections.constructor(StockCriticoDTO.class,
                        qProducto.nombre,
                        qProducto.stockActual,
                        Expressions.constant(10)
                ))
                .from(qProducto)
                .where(qProducto.empresa.id.eq(empresaId)
                        .and(qProducto.activo.isTrue())
                        .and(qProducto.stockActual.loe(10))
                        .and(qProducto.nombre.containsIgnoreCase(COMODIN_CON_TILDE).not())
                        .and(qProducto.nombre.containsIgnoreCase(COMODIN_SIN_TILDE).not()))
                .orderBy(qProducto.stockActual.asc())
                .limit(10)
                .fetch();
    }

    // =================================================================================
    // 5. VENTAS POR MES/AÑO
    // =================================================================================
    public BigDecimal obtenerVentasPorMesAnio(int mes, int anio) {
        try {
            java.time.YearMonth ym = java.time.YearMonth.of(anio, mes);
            LocalDateTime inicio = ym.atDay(1).atStartOfDay();
            LocalDateTime fin = ym.atEndOfMonth().atTime(23, 59, 59);
            
            DashboardDTO dto = this.obtenerKpis(inicio, fin);
            return (dto != null && dto.getTotalVentas() != null) ? dto.getTotalVentas() : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error al calcular ventas por mes/año ({}/{}): {}", mes, anio, e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // =================================================================================
    // 6. DESGLOSE DIARIO POR MES
    // =================================================================================
    public String obtenerDesgloseDiarioPorMes(int mes, int anio) {
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        QVenta qVenta = QVenta.venta;
        java.time.YearMonth ym = java.time.YearMonth.of(anio, mes);
        LocalDateTime inicioMes = ym.atDay(1).atStartOfDay();
        LocalDateTime finMes = ym.atEndOfMonth().atTime(23, 59, 59);

        List<Tuple> ventasDelMes = queryFactory
                .select(qVenta.fechaHora, qVenta.total)
                .from(qVenta)
                .where(qVenta.empresa.id.eq(idEmpresa)
                        .and(qVenta.fechaHora.between(inicioMes, finMes))
                        .and(qVenta.estado.eq(EstadoVenta.COMPLETADA)))
                .fetch();

        Map<Integer, BigDecimal> ventasPorDia = new HashMap<>();
        for (Tuple fila : ventasDelMes) {
            int dia = fila.get(qVenta.fechaHora).getDayOfMonth();
            BigDecimal total = fila.get(qVenta.total);
            if (total == null) total = BigDecimal.ZERO;
            ventasPorDia.put(dia, ventasPorDia.getOrDefault(dia, BigDecimal.ZERO).add(total));
        }

        StringBuilder reporte = new StringBuilder("Desglose diario " + mes + "/" + anio + ":\n");
        for (int i = 1; i <= ym.lengthOfMonth(); i++) {
            BigDecimal totalDia = ventasPorDia.getOrDefault(i, BigDecimal.ZERO);
            reporte.append("Día ").append(i).append(": S/ ").append(totalDia).append("\n");
        }

        return reporte.toString();
    }

    // =================================================================================
    // 7. TOP VENTAS HISTÓRICO POR MES/AÑO
    // =================================================================================
    public List<TopProductoDTO> obtenerTopVentasPorMesAnio(int mes, int anio, int limite) {
        java.time.YearMonth ym = java.time.YearMonth.of(anio, mes);
        LocalDateTime inicioMes = ym.atDay(1).atStartOfDay();
        LocalDateTime finMes = ym.atEndOfMonth().atTime(23, 59, 59);

        return this.obtenerTopVentas(inicioMes, finMes, limite);
    }
    
    // =================================================================================
    // 8. PRODUCTOS SIN ROTACIÓN (ESTANCADOS)
    // =================================================================================
    public List<String> obtenerProductosSinRotacion(int dias) {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        QProducto qProducto = QProducto.producto;
        QDetalleVenta qDetalle = QDetalleVenta.detalleVenta;
        QVenta qVenta = QVenta.venta;

        LocalDateTime limite = LocalDateTime.now().minusDays(dias);

        var productosVendidosSubquery = com.querydsl.jpa.JPAExpressions
                .select(qDetalle.producto.id)
                .from(qDetalle)
                .join(qDetalle.venta, qVenta)
                .where(qVenta.empresa.id.eq(empresaId)
                        .and(qVenta.fechaHora.after(limite))
                        .and(qVenta.estado.eq(EstadoVenta.COMPLETADA)));

        return queryFactory
                .select(qProducto.nombre)
                .from(qProducto)
                .where(qProducto.empresa.id.eq(empresaId)
                        .and(qProducto.activo.isTrue())
                        .and(qProducto.stockActual.gt(0))
                        .and(qProducto.id.notIn(productosVendidosSubquery))
                        .and(qProducto.nombre.containsIgnoreCase(COMODIN_CON_TILDE).not())
                        .and(qProducto.nombre.containsIgnoreCase(COMODIN_SIN_TILDE).not()))
                .limit(10)
                .fetch();
    }
}

// Fin de la definición principal de DashboardService.
