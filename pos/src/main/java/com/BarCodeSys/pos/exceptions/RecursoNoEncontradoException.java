/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Punto de Venta
 * DESCRIPCIÓN: Excepción personalizada utilizada para expresar errores controlados dentro
 *              del flujo de negocio.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de RecursoNoEncontradoException.

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNoEncontradoException extends RuntimeException {
    
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}

// Fin de la definición principal de RecursoNoEncontradoException.
