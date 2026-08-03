/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Analítica y Dashboard
 * DESCRIPCIÓN: Objeto de transferencia de datos utilizado para transportar información de
 *              grafico response entre capas.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.dto;

import java.math.BigDecimal;
import java.util.List;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de GraficoResponseDTO.

public class GraficoResponseDTO {
    private List<String> etiquetas;
    private List<BigDecimal> valores;

    public GraficoResponseDTO(List<String> etiquetas, List<BigDecimal> valores) {
        this.etiquetas = etiquetas;
        this.valores = valores;
    }

    // Getters y Setters
    public List<String> getEtiquetas() { return etiquetas; }
    public void setEtiquetas(List<String> etiquetas) { this.etiquetas = etiquetas; }
    public List<BigDecimal> getValores() { return valores; }
    public void setValores(List<BigDecimal> valores) { this.valores = valores; }
}

// Fin de la definición principal de GraficoResponseDTO.
