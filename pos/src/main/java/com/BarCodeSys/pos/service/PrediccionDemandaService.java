/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de prediccion demanda.
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

import com.BarCodeSys.pos.dto.ProyeccionDemandaDTO;
import com.BarCodeSys.pos.models.DetalleVenta;
import com.BarCodeSys.pos.models.Producto;
import com.BarCodeSys.pos.repository.DetalleVentaRepository;
import com.BarCodeSys.pos.repository.ProductoRepository;
import com.BarCodeSys.pos.security.SecurityUtils; // Agregamos tu utilidad de seguridad
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.BarCodeSys.pos.models.EstadoVenta;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de PrediccionDemandaService.

@Service
public class PrediccionDemandaService {

    private final ProductoRepository productoRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    public PrediccionDemandaService(ProductoRepository productoRepository, 
                                    DetalleVentaRepository detalleVentaRepository) {
        this.productoRepository = productoRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    // BLINDADO: Ya no necesitamos recibir idEmpresa como parámetro
    public List<ProyeccionDemandaDTO> obtenerProyeccionInventario() {
        
        // 1. Obtenemos el ID de la empresa de forma segura e invisible
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        
        // 2. Blindaje: Evitamos NullPointerExceptions si no hay usuario logueado
        if (idEmpresa == null) {
            throw new RuntimeException("Acceso denegado: No se pudo identificar la empresa del usuario actual.");
        }

        // 3. El resto de tu lógica intacta (¡que por cierto está muy bien pensada!)
        List<Producto> productosActivos = productoRepository.findByEmpresaIdAndActivoTrue(idEmpresa);

        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);
        List<DetalleVenta> ventasRecientes = detalleVentaRepository
                .findByProductoEmpresaIdAndVentaFechaHoraAfterAndVentaEstado(idEmpresa, hace30Dias, EstadoVenta.COMPLETADA);

        Map<Long, Integer> totalVendidoPorProducto = ventasRecientes.stream()
            .collect(Collectors.groupingBy(
                detalle -> detalle.getProducto().getId(),
                Collectors.summingInt(DetalleVenta::getCantidad)
            ));

        List<ProyeccionDemandaDTO> proyecciones = new ArrayList<>();

        for (Producto producto : productosActivos) {
            String nombre = producto.getNombre();
            
            int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;

            int totalVendido = totalVendidoPorProducto.getOrDefault(producto.getId(), 0);

            double consumoDiario = Math.round((totalVendido / 30.0) * 100.0) / 100.0;

            int diasEstimados;
            String estado;

            if (consumoDiario == 0.0) {
                diasEstimados = 999;
                estado = "Sin rotación";
            } else {
                diasEstimados = (int) Math.ceil(stockActual / consumoDiario);

                if (diasEstimados <= 7) {
                    estado = "Comprar Urgente";
                } else if (diasEstimados <= 15) {
                    estado = "Stock Crítico";
                } else if (diasEstimados <= 30) {
                    estado = "Revisar Pronto";
                } else {
                    estado = "Stock Sano";
                }
            }

            proyecciones.add(new ProyeccionDemandaDTO(nombre, stockActual, consumoDiario, diasEstimados, estado));
        }

        return proyecciones;
    }
}

// Fin de la definición principal de PrediccionDemandaService.
