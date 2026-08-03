/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Repositorio de persistencia encargado de consultar y administrar datos de
 *              detalle venta mediante Spring Data.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.repository;

import com.BarCodeSys.pos.models.DetalleVenta;
import com.BarCodeSys.pos.models.EstadoVenta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de DetalleVentaRepository.

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @EntityGraph(attributePaths = {"producto"}) 
    List<DetalleVenta> findByVentaId(Long ventaId);
    
    List<DetalleVenta> findByProductoEmpresaIdAndVentaFechaHoraAfterAndVentaEstado(
            Long empresaId, 
            LocalDateTime fechaLimite, 
            EstadoVenta estadoVenta
    );
}

// Fin de la definición principal de DetalleVentaRepository.
