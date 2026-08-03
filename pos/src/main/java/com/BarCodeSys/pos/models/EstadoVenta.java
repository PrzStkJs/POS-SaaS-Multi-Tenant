/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Modelo de dominio que representa la estructura, relaciones y reglas base de
 *              estado venta.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.models;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de EstadoVenta.

public enum EstadoVenta {
    PENDIENTE_PAGO,
    COMPLETADA,
    ANULADA
}

// Fin de la definición principal de EstadoVenta.
