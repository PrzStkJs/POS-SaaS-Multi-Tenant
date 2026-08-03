/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Analítica y Dashboard
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de dashboard dentro del sistema POS.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.controller;

import com.BarCodeSys.pos.dto.DashboardDTO;
import com.BarCodeSys.pos.dto.GraficoResponseDTO;
import com.BarCodeSys.pos.dto.StockCriticoDTO;
import com.BarCodeSys.pos.dto.TopProductoDTO;
import com.BarCodeSys.pos.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
// Importaciones necesarias para exponer métricas del dashboard.
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de DashboardController.

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // ====================================================================
    // 1. KPIs (Tarjetas superiores)
    // ====================================================================
    @GetMapping("/kpis")
    public ResponseEntity<DashboardDTO> getKpis(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        
        // El servicio resuelve la empresa actual desde el contexto de seguridad.

        if (fechaInicio == null) fechaInicio = LocalDate.now();
        if (fechaFin == null) fechaFin = LocalDate.now();

        LocalDateTime inicioDia = fechaInicio.atStartOfDay();
        LocalDateTime finDia = fechaFin.atTime(LocalTime.MAX);

        // La consulta se delega al servicio para centralizar el contexto de empresa.
        DashboardDTO kpis = dashboardService.obtenerKpis(inicioDia, finDia);
        return ResponseEntity.ok(kpis);
    }

    // ====================================================================
    // 2. Gráfico de ventas (por hora o por día)
    // ====================================================================
    @GetMapping("/grafico")
    public ResponseEntity<GraficoResponseDTO> getDatosGrafico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        if (fechaInicio == null) fechaInicio = LocalDate.now();
        if (fechaFin == null) fechaFin = LocalDate.now();

        LocalDateTime inicioDia = fechaInicio.atStartOfDay();
        LocalDateTime finDia = fechaFin.atTime(LocalTime.MAX);

        // La consulta se delega al servicio para centralizar la lógica del reporte.
        GraficoResponseDTO graficoDatos = dashboardService.obtenerDatosGrafico(
                inicioDia, finDia, fechaInicio, fechaFin);
        return ResponseEntity.ok(graficoDatos);
    }

    // ====================================================================
    // 3. Top productos más vendidos (NUEVO)
    // ====================================================================
    @GetMapping("/top-ventas")
    public ResponseEntity<List<TopProductoDTO>> getTopVentas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            @RequestParam(value = "limite", defaultValue = "5") int limite) {

        if (fechaInicio == null) fechaInicio = LocalDate.now();
        if (fechaFin == null) fechaFin = LocalDate.now();

        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        // La consulta se delega al servicio para centralizar la lógica del reporte.
        List<TopProductoDTO> top = dashboardService.obtenerTopVentas(inicio, fin, limite);
        return ResponseEntity.ok(top);
    }

    // ====================================================================
    // 4. Stock crítico (NUEVO)
    // ====================================================================
    @GetMapping("/stock-critico")
    public ResponseEntity<List<StockCriticoDTO>> getStockCritico() {
        // El servicio aplica el filtro de empresa antes de devolver el stock crítico.
        List<StockCriticoDTO> stockCritico = dashboardService.obtenerStockCritico();
        return ResponseEntity.ok(stockCritico);
    }
}

// Fin de la definición principal de DashboardController.
