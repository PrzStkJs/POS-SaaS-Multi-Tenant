/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Pagos
 * DESCRIPCIÓN: Implementación de servicio responsable de ejecutar reglas, validaciones e
 *              integraciones de pasarela pago factory.
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de PasarelaPagoFactory.

@Component
public class PasarelaPagoFactory {

    private final Map<String, PasarelaPagoService> pasarelas;

    @Autowired
    public PasarelaPagoFactory(List<PasarelaPagoService> pasarelasList) {
        this.pasarelas = pasarelasList.stream()
                .collect(Collectors.toMap(PasarelaPagoService::obtenerNombreProveedor, pasarela -> pasarela));
    }

    public PasarelaPagoService obtenerPasarela(String nombreProveedor) {
        String key = nombreProveedor != null ? nombreProveedor.toUpperCase() : "MANUAL";
        return pasarelas.getOrDefault(key, pasarelas.get("MANUAL"));
    }
}

// Fin de la definición principal de PasarelaPagoFactory.
