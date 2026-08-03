/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Pagos
 * DESCRIPCIÓN: Implementación de servicio responsable de ejecutar reglas, validaciones e
 *              integraciones de mercado pago.
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.mercadopago.core.MPRequestOptions;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de MercadoPagoServiceImpl.

@Service
public class MercadoPagoServiceImpl implements PasarelaPagoService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoServiceImpl.class);
    
    // Los identificadores de sucursal y caja se generan dinámicamente por empresa.

    @Autowired
    @Lazy
    private VentaService ventaService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String obtenerNombreProveedor() {
        return "MERCADOPAGO";
    }

    @Override
    public Map<String, Object> procesarPago(Venta venta, String token) {
        Map<String, Object> respuesta = new HashMap<>();

        if (token == null || token.trim().isEmpty()) {
            respuesta.put("estado", "ERROR");
            respuesta.put("mensaje", "La empresa no tiene configurado un Access Token.");
            return respuesta;
        }

        try {
            HttpHeaders headers = crearHeadersAutenticados(token);
            Long collectorId = obtenerCollectorId(headers);
            ResponseEntity<Map> response = crearOrdenQrPresencial(token, headers, collectorId, venta);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String qrData = (String) response.getBody().get("qr_data");
                respuesta.put("estado", "PENDIENTE");
                respuesta.put("qr_data", qrData);

                String qrImageBase64 = generarQrBase64(qrData);
                if (qrImageBase64 != null) {
                    respuesta.put("qr_image", qrImageBase64);
                }

                respuesta.put("mensaje", "QR generado con exito.");
                log.info("QR Mercado Pago generado para la venta #{}", venta.getId());
            } else {
                respuesta.put("estado", "ERROR");
                respuesta.put("mensaje", "No se pudo generar el QR presencial.");
            }
        } catch (Exception e) {
            log.error("Error en pasarela Mercado Pago: {}", e.getMessage(), e);
            respuesta.put("estado", "ERROR");
            respuesta.put("mensaje", "Error: " + e.getMessage());
        }

        return respuesta;
    }

    private HttpHeaders crearHeadersAutenticados(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    private Long obtenerCollectorId(HttpHeaders headers) {
        ResponseEntity<Map> userResponse = restTemplate.exchange(
                "https://api.mercadopago.com/users/me",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        return ((Number) userResponse.getBody().get("id")).longValue();
    }

    private ResponseEntity<Map> crearOrdenQrPresencial(
            String token,
            HttpHeaders headers,
            Long collectorId,
            Venta venta) {

        // 1. Genera identificadores dinámicos por empresa.
        String storeIdDinamico = "SUCURSAL_EMP_" + venta.getEmpresa().getId();
        String posIdDinamico = "CAJA_EMP_" + venta.getEmpresa().getId();

        String urlQr = "https://api.mercadopago.com/instore/orders/qr/seller/collectors/"
                + collectorId + "/pos/" + posIdDinamico + "/qrs";

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(crearCuerpoOrden(venta), headers);

        try {
            return restTemplate.exchange(urlQr, HttpMethod.PUT, request, Map.class);
        } catch (HttpClientErrorException.BadRequest | HttpClientErrorException.NotFound e) {
            log.info("Caja Mercado Pago no disponible. Se intentara configurar sucursal y caja para la Empresa ID: {}", venta.getEmpresa().getId());
            
            // 2. Configura Mercado Pago usando los identificadores dinámicos.
            String internalStoreId = crearUObtenerSucursal(token, collectorId, storeIdDinamico);

            if (internalStoreId == null) {
                throw new RuntimeException("No se pudo enlazar la sucursal.");
            }

            crearCajaMercadoPago(token, posIdDinamico, internalStoreId);
            esperarIndexacionMercadoPago();
            return restTemplate.exchange(urlQr, HttpMethod.PUT, request, Map.class);
        }
    }

    private Map<String, Object> crearCuerpoOrden(Venta venta) {
        Map<String, Object> body = new HashMap<>();
        body.put("external_reference", String.valueOf(venta.getId()));
        body.put("title", "Consumo POS - Ticket #" + venta.getId());
        body.put("description", "Compra en tienda fisica");
        body.put("total_amount", venta.getTotal());

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("sku_number", "TKT-" + venta.getId());
        item.put("category", "marketplace");
        item.put("title", "Productos varios");
        item.put("description", "Venta POS");
        item.put("unit_price", venta.getTotal());
        item.put("quantity", 1);
        item.put("unit_measure", "unit");
        item.put("total_amount", venta.getTotal());
        items.add(item);

        body.put("items", items);
        return body;
    }

    private String crearUObtenerSucursal(String token, Long collectorId, String storeId) {
        try {
            HttpHeaders headers = crearHeadersAutenticados(token);

            Map<String, Object> location = new HashMap<>();
            location.put("street_number", "123");
            location.put("street_name", "Av. Principal");
            location.put("city_name", "Lima");
            location.put("state_name", "Lima Metropolitana");
            location.put("latitude", -12.05);
            location.put("longitude", -77.12);
            location.put("reference", "Local Central");

            Map<String, Object> storeBody = new HashMap<>();
            storeBody.put("name", "Sucursal Central POS");
            storeBody.put("location", location);
            storeBody.put("external_id", storeId); // Usa el storeId que recibe por parámetro

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(storeBody, headers);
            String url = "https://api.mercadopago.com/users/" + collectorId + "/stores";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            log.info("Sucursal Mercado Pago registrada.");
            return response.getBody().get("id").toString();
        } catch (HttpClientErrorException e) {
            log.info("Sucursal Mercado Pago existente. Recuperando ID interno.");
            return buscarSucursalPorExternalId(token, collectorId, storeId);
        } catch (Exception e) {
            log.error("Error configurando sucursal Mercado Pago: {}", e.getMessage(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String buscarSucursalPorExternalId(String token, Long collectorId, String storeId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            String url = "https://api.mercadopago.com/users/" + collectorId + "/stores/search?external_id=" + storeId;

            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            if (response.getBody() == null) {
                return null;
            }

            List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("results");
            if (results != null && !results.isEmpty()) {
                String internalId = results.get(0).get("id").toString();
                log.info("Sucursal Mercado Pago recuperada con ID interno {}.", internalId);
                return internalId;
            }
        } catch (Exception e) {
            log.error("Error buscando sucursal Mercado Pago: {}", e.getMessage(), e);
        }

        return null;
    }

    private void crearCajaMercadoPago(String token, String posId, String internalStoreId) {
        try {
            HttpHeaders headers = crearHeadersAutenticados(token);

            Map<String, Object> posBody = new HashMap<>();
            posBody.put("name", "Caja Principal POS");
            posBody.put("fixed_amount", true);
            posBody.put("store_id", Long.parseLong(internalStoreId));
            posBody.put("external_id", posId); // Usa el posId que recibe por parámetro

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(posBody, headers);
            restTemplate.postForEntity("https://api.mercadopago.com/pos", request, Map.class);
            log.info("Caja Mercado Pago registrada.");
        } catch (HttpClientErrorException e) {
            log.info("Caja Mercado Pago existente.");
        } catch (Exception e) {
            log.error("Error configurando caja Mercado Pago: {}", e.getMessage(), e);
        }
    }

    public void actualizarEstadoVenta(Long paymentId, String accessToken) throws Exception {
        PaymentClient client = new PaymentClient();
        
        // 1. Configuramos las opciones con el token de la empresa correspondiente
        MPRequestOptions requestOptions = MPRequestOptions.builder()
                .accessToken(accessToken)
                .build();

        // 2. Buscamos el pago inyectando las credenciales
        Payment payment = client.get(paymentId, requestOptions);

        if ("approved".equals(payment.getStatus()) && payment.getExternalReference() != null) {
            Long idVenta = Long.parseLong(payment.getExternalReference());
            ventaService.confirmarVentaDesdeWebhook(idVenta);
            log.info("Webhook Mercado Pago procesado. Venta #{} confirmada.", idVenta);
        }
    }

    private void esperarIndexacionMercadoPago() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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
            log.warn("No se pudo convertir el QR Mercado Pago a Base64: {}", e.getMessage());
            return null;
        }
    }
}

// Fin de la definición principal de MercadoPagoServiceImpl.
