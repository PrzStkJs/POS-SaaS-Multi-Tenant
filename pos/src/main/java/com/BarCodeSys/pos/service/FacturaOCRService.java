/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de factura ocr.
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

import com.BarCodeSys.pos.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de FacturaOCRService.

@Service
public class FacturaOCRService {

    private static final Logger log = LoggerFactory.getLogger(FacturaOCRService.class);

    private final GeminiClient geminiClient;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    public FacturaOCRService(GeminiClient geminiClient, ObjectMapper mapper) {
        this.geminiClient = geminiClient;
        this.mapper = mapper;
        this.restTemplate = new RestTemplate(); 
    }

    // Removido el "throws Exception" genérico para un manejo de errores más limpio
    public String analizarDocumentoConGemini(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o no fue enviado.");
        }

        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();

        if (!geminiClient.tieneCuotaDisponible(idEmpresa)) {
            log.warn("Empresa ID: {} ha excedido su cuota de OCR.", idEmpresa);
            throw new RuntimeException("ERROR_CUOTA_AGOTADA: Has alcanzado el límite de escaneos de IA permitidos.");
        }

        log.info("📄 Enviando factura a Gemini IA para la empresa ID: {}", idEmpresa);

        try {
            String base64Image = Base64.getEncoder().encodeToString(archivo.getBytes());
            String mimeType = archivo.getContentType();

            String prompt = "Eres un asistente de inventario experto. Analiza esta imagen de una factura o boleta de compras. " +
                    "Extrae los productos que se compraron y devuelve ÚNICAMENTE un arreglo JSON con este formato exacto: " +
                    "[{\"descripcion\": \"NOMBRE DEL PRODUCTO\", \"cantidad\": 1}] " +
                    "Si la factura tiene cantidades, úsalas. Si no, pon 1 por defecto. " +
                    "No uses formato markdown, no uses etiquetas ```json, devuelve SOLO el texto del arreglo JSON puro.";

            // Construcción nativa y eficiente del JSON
            ObjectNode requestBodyNode = mapper.createObjectNode();
            ArrayNode contentsArray = requestBodyNode.putArray("contents");
            ObjectNode contentNode = contentsArray.addObject();
            ArrayNode partsArray = contentNode.putArray("parts");

            partsArray.addObject().put("text", prompt);

            ObjectNode imagePart = partsArray.addObject();
            ObjectNode inlineData = imagePart.putObject("inline_data");
            inlineData.put("mime_type", mimeType);
            inlineData.put("data", base64Image);

            String requestBody = mapper.writeValueAsString(requestBodyNode);
            String url = geminiClient.construirUrlOcr();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            JsonNode rootNode = mapper.readTree(response.getBody());
            String textoIA = rootNode.path("candidates").get(0)
                    .path("content")
                    .path("parts").get(0)
                    .path("text").asText();

            textoIA = textoIA.replace("```json", "").replace("```", "").trim();
            log.debug("Respuesta limpia de la IA: {}", textoIA);

            geminiClient.registrarUsoExitoso(idEmpresa);

            return textoIA;

        } catch (Exception e) {
            log.error("❌ Error al comunicarse con Gemini para empresa {}: {}", idEmpresa, e.getMessage(), e);
            throw new RuntimeException("Error al procesar el documento con la Inteligencia Artificial.");
        }
    }
}

// Fin de la definición principal de FacturaOCRService.
