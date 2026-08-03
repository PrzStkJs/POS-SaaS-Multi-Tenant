/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Punto de Venta
 * DESCRIPCIÓN: Objeto de transferencia de datos utilizado para transportar información de
 *              proyeccion demanda entre capas.
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

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de ProyeccionDemandaDTO.

public record ProyeccionDemandaDTO(
    String nombreProducto,
    Integer stockActual,
    Double consumoDiario,
    Integer diasEstimados,
    String estado
) {}

// Fin de la definición principal de ProyeccionDemandaDTO.
