/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Pagos
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de pasarela pago.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.service;

import com.BarCodeSys.pos.models.Venta;

import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de PasarelaPagoService.

public interface PasarelaPagoService {

    /**
     * Retorna el codigo interno del proveedor usado por la fabrica de pagos.
     */
    String obtenerNombreProveedor();

    /**
     * Procesa una venta o genera una intencion de pago pendiente.
     *
     * @param venta venta registrada en el POS
     * @param token credencial de la empresa cuando el proveedor la requiere
     * @return mapa normalizado con estado, mensaje y datos del pago
     */
    Map<String, Object> procesarPago(Venta venta, String token);
}

// Fin de la definición principal de PasarelaPagoService.
