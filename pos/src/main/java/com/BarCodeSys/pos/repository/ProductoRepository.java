/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Repositorio de persistencia encargado de consultar y administrar datos de
 *              producto mediante Spring Data.
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

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.BarCodeSys.pos.models.Producto;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de ProductoRepository.

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigoBarrasAndEmpresaId(String codigoBarras, Long empresaId);
    
    Optional<Producto> findByIdAndEmpresaId(Long id, Long empresaId);

    @EntityGraph(attributePaths = {"categoria"})
    List<Producto> findByEmpresaId(Long empresaId);

    @EntityGraph(attributePaths = {"categoria"})
    List<Producto> findByEmpresaIdAndActivoTrue(Long empresaId);
}

// Fin de la definición principal de ProductoRepository.
