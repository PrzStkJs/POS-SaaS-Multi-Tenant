/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Repositorio de persistencia encargado de consultar y administrar datos de
 *              movimiento inventario mediante Spring Data.
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

import com.BarCodeSys.pos.models.MovimientoInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de MovimientoInventarioRepository.

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {
    
    // Cambiado de Iterable a List para mayor flexibilidad y compatibilidad con los DTOs
    List<MovimientoInventario> findByEmpresaIdOrderByFechaHoraDesc(Long empresaId);
}

// Fin de la definición principal de MovimientoInventarioRepository.
