/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Repositorio de persistencia encargado de consultar y administrar datos de
 *              sugerencia cross selling mediante Spring Data.
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

import com.BarCodeSys.pos.models.SugerenciaCrossSelling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de SugerenciaCrossSellingRepository.

@Repository
public interface SugerenciaCrossSellingRepository extends JpaRepository<SugerenciaCrossSelling, Long> {
    
    Optional<SugerenciaCrossSelling> findFirstByProductoBaseIdAndActivoTrue(Long productoBaseId);
    
 // 🔒 BLINDAJE: Buscar una sugerencia específica asegurando que la empresa dueña del producto base sea la del usuario actual
    Optional<SugerenciaCrossSelling> findByIdAndProductoBase_Empresa_Id(Long id, Long idEmpresa);

    // 🔒 BLINDAJE: Buscar sugerencias de un producto base asegurando la tenencia
    List<SugerenciaCrossSelling> findByProductoBaseIdAndActivoTrueAndProductoBase_Empresa_IdOrderByOrigenDesc(Long productoBaseId, Long idEmpresa);

    List<SugerenciaCrossSelling> findByProductoBaseIdAndActivoTrueOrderByOrigenDesc(Long productoBaseId);

    List<SugerenciaCrossSelling> findByActivoTrueAndProductoBase_Empresa_Id(Long idEmpresa);

    List<SugerenciaCrossSelling> findByProductoBase_Empresa_Id(Long idEmpresa);

    boolean existsByProductoBaseIdAndProductoSugeridoIdAndActivoTrue(Long productoBaseId, Long productoSugeridoId);
    
    boolean existsByProductoBaseIdAndProductoSugeridoId(Long productoBaseId, Long productoSugeridoId);

    @Modifying
    @Transactional
    @Query("DELETE FROM SugerenciaCrossSelling s WHERE s.origen = :origen AND s.productoBase.empresa.id = :idEmpresa")
    void deleteByOrigenAndEmpresaId(@Param("origen") String origen, @Param("idEmpresa") Long idEmpresa);
}

// Fin de la definición principal de SugerenciaCrossSellingRepository.
