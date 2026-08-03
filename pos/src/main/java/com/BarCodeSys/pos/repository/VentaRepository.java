/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Repositorio de persistencia encargado de consultar y administrar datos de
 *              venta mediante Spring Data.
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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.BarCodeSys.pos.models.Venta;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de VentaRepository.

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    
    // Método para traer ventas por empresa con límite (paginación)
    Page<Venta> findByEmpresaId(Long empresaId, Pageable pageable);

    // 🔒 BLINDAJE: Buscar una venta específica asegurando que pertenezca a la empresa
    Optional<Venta> findByIdAndEmpresaId(Long id, Long empresaId);
}

// Fin de la definición principal de VentaRepository.
