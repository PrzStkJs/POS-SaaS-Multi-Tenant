/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Repositorio de persistencia encargado de consultar y administrar datos de
 *              categoria mediante Spring Data.
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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.BarCodeSys.pos.models.Categoria;

import java.util.List;
import java.util.Optional;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de CategoriaRepository.

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    
    // Lista solo las de la empresa
    List<Categoria> findByEmpresaId(Long empresaId);
    
    // Busca una categoría específica asegurando que sea de la empresa
    Optional<Categoria> findByIdAndEmpresaId(Long id, Long empresaId);
    
    // Verifica si existe la categoría en esa empresa (útil para el delete)
    boolean existsByIdAndEmpresaId(Long id, Long empresaId);
}

// Fin de la definición principal de CategoriaRepository.
