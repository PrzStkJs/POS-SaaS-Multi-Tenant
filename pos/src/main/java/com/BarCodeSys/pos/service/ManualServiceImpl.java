/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Pagos
 * DESCRIPCIÓN: Implementación de servicio responsable de ejecutar reglas, validaciones e
 *              integraciones de manual.
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
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de ManualServiceImpl.

@Service
public class ManualServiceImpl implements PasarelaPagoService {

    @Override
    public String obtenerNombreProveedor() {
        return "MANUAL";
    }

    @Override
    public Map<String, Object> procesarPago(Venta venta, String token) {
        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("estado", "COMPLETADO");
        respuesta.put("metodo", "EFECTIVO_O_TRANSFERENCIA_DIRECTA");
        respuesta.put("mensaje", "Pago registrado manualmente sin pasarela externa.");
        return respuesta;
    }
}

// Fin de la definición principal de ManualServiceImpl.
