/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Pagos
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de webhook dentro del sistema POS.
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

import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.repository.EmpresaRepository;
import com.BarCodeSys.pos.service.MercadoPagoServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de WebhookController.

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    @Autowired
    private MercadoPagoServiceImpl mercadoPagoService;

    // 1. Inyectamos el repositorio para buscar el token de la empresa
    @Autowired
    private EmpresaRepository empresaRepository;

    // Mercado Pago enviará un POST a esta ruta
    @PostMapping("/mercadopago")
    public ResponseEntity<String> recibirNotificacionMP(
            @RequestParam(name = "empresaId") Long empresaId, // 2. Capturamos la empresa desde la URL
            @RequestParam(name = "data.id", required = false) String paymentId,
            @RequestParam(name = "type", required = false) String type,
            @RequestBody(required = false) Map<String, Object> payload) {

        System.out.println("🔔 ¡Notificación de Mercado Pago recibida para la Empresa ID: " + empresaId + "!");

        // Solo nos interesan las notificaciones de tipo "payment" (pago)
        if ("payment".equals(type) && paymentId != null) {
            try {
                // 3. Buscamos la empresa en la base de datos
                Empresa empresa = empresaRepository.findById(empresaId)
                        .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

                // 4. Extraemos el token
                String token = empresa.getMpAccessToken();

                if (token == null || token.trim().isEmpty()) {
                    System.err.println("La empresa " + empresaId + " no tiene un Access Token configurado.");
                    return ResponseEntity.badRequest().body("Token no configurado");
                }

                // 5. Mandamos el ID del pago y el token correcto al servicio
                mercadoPagoService.actualizarEstadoVenta(Long.parseLong(paymentId), token);

            } catch (Exception e) {
                System.err.println("Error procesando pago: " + e.getMessage());
            }
        }
        
        // SIEMPRE debes responder 200 OK rápido, o Mercado Pago creerá que fallaste y reenviará el mensaje
        return ResponseEntity.ok("OK"); 
    }
}

// Fin de la definición principal de WebhookController.
