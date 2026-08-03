/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de factura ocr dentro del sistema POS.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.BarCodeSys.pos.service.FacturaOCRService;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de FacturaOCRController.

@RestController
@RequestMapping("/api/ia/ocr")
@PreAuthorize("hasRole('ADMIN')")
public class FacturaOCRController {

    @Autowired
    private FacturaOCRService ocrService;

    @PostMapping("/procesar")
    public ResponseEntity<?> procesarFactura(@RequestParam("archivo") MultipartFile archivo) {
        try {
            String resultadoJson = ocrService.analizarDocumentoConGemini(archivo);
            // Si todo sale bien, devolvemos el JSON puro
            return ResponseEntity.ok(resultadoJson);
            
        } catch (IllegalArgumentException e) {
            // Error de archivo vacío
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
            
        } catch (RuntimeException e) {
            // Si la cuota de OCR se agotó, se responde con 403 y el mensaje de negocio.
            if (e.getMessage().contains("ERROR_CUOTA_AGOTADA")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                     .body("{\"error\": \"" + e.getMessage() + "\"}");
            }
            // Otros errores de negocio
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
            
        } catch (Exception e) {
            // Error grave del servidor
            return ResponseEntity.internalServerError().body("{\"error\": \"Error interno al leer el documento\"}");
        }
    }
}

// Fin de la definición principal de FacturaOCRController.
