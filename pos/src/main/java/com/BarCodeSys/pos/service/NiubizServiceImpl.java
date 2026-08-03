/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Pagos
 * DESCRIPCIÓN: Implementación de servicio responsable de ejecutar reglas, validaciones e
 *              integraciones de niubiz.
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

import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.models.Venta;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de NiubizServiceImpl.

@Service
public class NiubizServiceImpl implements PasarelaPagoService {

    private static final Logger log = LoggerFactory.getLogger(NiubizServiceImpl.class);
    private static final DateTimeFormatter FORMATO_FECHA_NIUBIZ = DateTimeFormatter.ofPattern("ddMMyyyy");

    @Value("${niubiz.url.seguridad}")
    private String urlSeguridad;

    @Value("${niubiz.url.qr}")
    private String urlQr;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate(); // ← instancia única reutilizada

    @Override
    public String obtenerNombreProveedor() {
        return "NIUBIZ";
    }

    @Override
    public Map<String, Object> procesarPago(Venta venta, String tokenIgnorado) {
        Map<String, Object> respuesta = new HashMap<>();
        Empresa empresa = venta.getEmpresa();

        try {
            String merchantId = limpiar(empresa.getNiubizMerchantId());
            String apiUser = limpiar(empresa.getNiubizApiUser());
            String apiPassword = limpiar(empresa.getNiubizApiPassword());

            if (merchantId == null || apiUser == null || apiPassword == null) {
                throw new RuntimeException("Credenciales de Niubiz incompletas en la configuracion de la empresa.");
            }

            String tokenNiubiz = obtenerTokenNiubiz(apiUser, apiPassword);
            Map<String, Object> qrGenerado = generarQrNiubiz(tokenNiubiz, merchantId, venta.getTotal(), venta.getId());

            respuesta.put("estado", "PENDIENTE");
            respuesta.put("mensaje", "QR generado con exito, esperando pago.");
            respuesta.putAll(qrGenerado);
            log.info("QR Niubiz generado para la venta #{}", venta.getId());
        } catch (Exception e) {
            respuesta.put("estado", "ERROR");
            respuesta.put("mensaje", "Error con Niubiz: " + e.getMessage());
            log.error("Error procesando pago Niubiz: {}", e.getMessage(), e);
        }

        return respuesta;
    }

    private String obtenerTokenNiubiz(String apiUser, String apiPassword) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(apiUser, apiPassword);
        headers.setAccept(Collections.singletonList(MediaType.TEXT_PLAIN));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlSeguridad,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            String body = response.getBody();
            if (body == null || body.trim().isEmpty()) {
                throw new RuntimeException("Niubiz no devolvio token de seguridad.");
            }

            return body;
        } catch (Exception e) {
            throw new RuntimeException("Fallo la autenticacion con Niubiz. " + e.getMessage());
        }
    }

    private Map<String, Object> generarQrNiubiz(String token, String merchantId, BigDecimal monto, Long ventaId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", token.replace("\"", "").trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.ALL));
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

        Map<String, Object> body = new HashMap<>();
        body.put("enabled", true);
        body.put("tagType", "DYNAMIC");
        body.put("validityDate", LocalDate.now().plusDays(1).format(FORMATO_FECHA_NIUBIZ));
        body.put("param", List.of(
                parametroNiubiz("merchantId", merchantId),
                parametroNiubiz("transactionCurrency", "604"),
                parametroNiubiz("transactionAmount", monto.setScale(2, RoundingMode.HALF_UP).toPlainString()),
                parametroNiubiz("additionalData", "VENTA-" + ventaId)
        ));

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    urlQr,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            String responseBody = response.getBody();
            if (responseBody == null || responseBody.trim().isEmpty()) {
                throw new RuntimeException("La API de Niubiz respondio vacio al pedir el QR.");
            }

            return extraerQrDesdeRespuesta(responseBody);
        } catch (HttpStatusCodeException e) {
            log.error("Niubiz rechazo el QR: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Niubiz rechazo el QR: " + e.getStatusCode());
        } catch (Exception e) {
            throw new RuntimeException("Fallo al generar el QR Niubiz: " + e.getMessage());
        }
    }

    private Map<String, Object> extraerQrDesdeRespuesta(String responseBody) {
        Map<String, Object> resultado = new HashMap<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            validarRespuestaNiubiz(root);

            copiarSiExiste(root, resultado, "tagId", "tag_id");
            copiarSiExiste(root, resultado, "message", "mensaje_niubiz");

            if (root.hasNonNull("tagImg")) {
                resultado.put("qr_image", root.get("tagImg").asText());
                return resultado;
            }

            String textoQr = obtenerTextoQr(root);
            if (textoQr != null) {
                resultado.put("qr_data", textoQr);
                String qrBase64 = generarQrBase64(textoQr);
                if (qrBase64 != null) {
                    resultado.put("qr_image", qrBase64);
                }
                return resultado;
            }
        } catch (Exception ignored) {
            // Algunas respuestas del endpoint ASCII no son JSON; se devuelven como texto imprimible.
        }

        resultado.put("qr_data", responseBody);
        return resultado;
    }

    private String obtenerTextoQr(JsonNode root) {
        if (root.hasNonNull("qrCode")) {
            return root.get("qrCode").asText();
        }
        if (root.hasNonNull("qrData")) {
            return root.get("qrData").asText();
        }
        if (root.hasNonNull("ascii")) {
            return root.get("ascii").asText();
        }
        return null;
    }

    private String generarQrBase64(String textoQr) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(textoQr, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            log.warn("No se pudo convertir el QR Niubiz a imagen Base64: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, String> parametroNiubiz(String nombre, String valor) {
        Map<String, String> parametro = new HashMap<>();
        parametro.put("name", nombre);
        parametro.put("value", valor);
        return parametro;
    }

    private void validarRespuestaNiubiz(JsonNode root) {
        if (!root.has("codeResponse") && !root.has("codResponse")) {
            return;
        }

        String codigo = root.has("codeResponse")
                ? root.get("codeResponse").asText()
                : root.get("codResponse").asText();

        if (!"0".equals(codigo) && !"00".equals(codigo) && !"0000".equals(codigo)) {
            String mensaje = root.has("message") ? root.get("message").asText() : "Respuesta no exitosa";
            throw new RuntimeException("Niubiz respondio codigo " + codigo + ": " + mensaje);
        }
    }

    private void copiarSiExiste(JsonNode root, Map<String, Object> destino, String campoOrigen, String campoDestino) {
        if (root.hasNonNull(campoOrigen)) {
            destino.put(campoDestino, root.get(campoOrigen).asText());
        }
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return null;
        }

        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}

// Fin de la definición principal de NiubizServiceImpl.
