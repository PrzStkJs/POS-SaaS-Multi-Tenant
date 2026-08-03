/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Repositorio de persistencia encargado de consultar y administrar datos de
 *              usuario mediante Spring Data.
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

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.BarCodeSys.pos.models.Usuario;
import java.util.List;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de UsuarioRepository.

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    
    List<Usuario> findByEmpresaId(Long idEmpresa);
    
    long countByEmpresaIdAndActivoTrue(Long idEmpresa);
    long countByEmpresaId(Long idEmpresa);
}

// Fin de la definición principal de UsuarioRepository.
