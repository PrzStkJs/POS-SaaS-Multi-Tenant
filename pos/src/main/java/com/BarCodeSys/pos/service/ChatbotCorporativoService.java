/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de chatbot corporativo.
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

import com.BarCodeSys.pos.dto.TopProductoDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

// Importaciones para Logging profesional
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de ChatbotCorporativoService.

@Service
public class ChatbotCorporativoService {

    // PRO-TIP 1: Logger profesional en lugar de System.out / System.err
    private static final Logger log = LoggerFactory.getLogger(ChatbotCorporativoService.class);

    // PRO-TIP 2: Dependencias inmutables con 'final'
    private final GeminiClient geminiClient;
    private final DashboardService dashboardService;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    // PRO-TIP 3: Inyección por constructor. 
    // Spring Boot inyecta automáticamente el ObjectMapper global optimizado.
    public ChatbotCorporativoService(GeminiClient geminiClient, 
                                     DashboardService dashboardService, 
                                     ObjectMapper mapper) {
        this.geminiClient = geminiClient;
        this.dashboardService = dashboardService;
        this.mapper = mapper;
        // RestTemplate lo instanciamos una sola vez al construir el servicio, no en cada petición
        this.restTemplate = new RestTemplate(); 
    }

    public String ejecutarAsistenteFlujoCompleto(String systemPrompt, String preguntaUsuario) {
        
        if (!geminiClient.isIaDisponibleGlobal()) {
            log.warn("Intento de uso de IA bloqueado: Cuota global agotada.");
            return "ERROR_CUOTA_AGOTADA";
        }

        String url = geminiClient.construirUrlGemini();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ArrayNode contents = mapper.createArrayNode();
            String promptCompleto = systemPrompt + "\n\nPregunta actual del Gerente: " + preguntaUsuario;
            
            ObjectNode userTurn = mapper.createObjectNode()
                    .put("role", "user")
                    .set("parts", mapper.createArrayNode().add(mapper.createObjectNode().put("text", promptCompleto)));
            contents.add(userTurn);

            int loopCount = 0;
            int maxLoops = 2; 
            String respuestaFinalTexto = "";

            while (loopCount < maxLoops) {
                ObjectNode requestBody = mapper.createObjectNode();
                requestBody.set("contents", contents);
                requestBody.set("tools", construirHerramientas(mapper));

                HttpEntity<String> entity = new HttpEntity<>(mapper.writeValueAsString(requestBody), headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                JsonNode root = mapper.readTree(response.getBody());
                JsonNode parts = root.path("candidates").get(0).path("content").path("parts");

                ArrayNode modelPartsForHistory = mapper.createArrayNode();
                ArrayNode functionPartsForHistory = mapper.createArrayNode();
                boolean modeloPidioHerramientas = false;
                StringBuilder textoTurnoActual = new StringBuilder();

                for (JsonNode part : parts) {
                    if (part.has("text")) {
                        textoTurnoActual.append(part.get("text").asText());
                        modelPartsForHistory.add(part.deepCopy());
                    }
                    if (part.has("functionCall")) {
                        modeloPidioHerramientas = true;
                        modelPartsForHistory.add(part.deepCopy());

                        JsonNode functionCall = part.get("functionCall");
                        String nombreFuncion = functionCall.get("name").asText();
                        JsonNode argumentos = functionCall.get("args");

                        String datosBackend = ejecutarHerramientaBackend(nombreFuncion, argumentos);

                        ObjectNode responseNode = mapper.createObjectNode().put("datos", datosBackend);
                        ObjectNode functionPart = mapper.createObjectNode().put("name", nombreFuncion).set("response", responseNode);
                        functionPartsForHistory.add(mapper.createObjectNode().set("functionResponse", functionPart));
                    }
                }

                contents.add(mapper.createObjectNode().put("role", "model").set("parts", modelPartsForHistory));

                if (!modeloPidioHerramientas) {
                    respuestaFinalTexto = textoTurnoActual.toString().trim();
                    break;
                } else {
                    contents.add(mapper.createObjectNode().put("role", "function").set("parts", functionPartsForHistory));
                    loopCount++;
                    
                    // Uso del logger en lugar de System.out
                    log.info("⏳ Procesando herramientas de IA... Pausa táctica de 2s");
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt(); // Buena práctica al silenciar un InterruptedException
                    }
                }
            }

            if (respuestaFinalTexto.isEmpty()) {
                return "He consolidado la información. ¿Podríamos enfocarnos en analizar un periodo específico?";
            }
            return respuestaFinalTexto;

        } catch (Exception e) {
            // Uso del logger para errores (guarda el stacktrace real en los logs del servidor)
            log.error("❌ Error en bucle dinámico de Gemini: {}", e.getMessage(), e);
            geminiClient.analizarErrorGlobal(e);
            return !geminiClient.isIaDisponibleGlobal() ? "ERROR_CUOTA_AGOTADA" : "ERROR_LIMITE_API";
        }
    }

    private String ejecutarHerramientaBackend(String nombreFuncion, JsonNode argumentos) {
        try {
            if (nombreFuncion.equals("obtenerVentasPorMesAnio")) {
                int mes1 = argumentos.get("mes1").asInt();
                int anio1 = argumentos.get("anio1").asInt();
                BigDecimal ventasDb1 = dashboardService.obtenerVentasPorMesAnio(mes1, anio1);
                String resultado = "Ventas " + mes1 + "/" + anio1 + ": S/ " + ventasDb1;

                if (argumentos.has("mes2") && argumentos.has("anio2")) {
                    int mes2 = argumentos.get("mes2").asInt();
                    int anio2 = argumentos.get("anio2").asInt();
                    BigDecimal ventasDb2 = dashboardService.obtenerVentasPorMesAnio(mes2, anio2);
                    resultado += " | Ventas " + mes2 + "/" + anio2 + ": S/ " + ventasDb2;
                }
                return resultado;

            } else if (nombreFuncion.equals("obtenerDesgloseDiarioPorMes")) {
                int mes = argumentos.get("mes").asInt();
                int anio = argumentos.get("anio").asInt();
                return dashboardService.obtenerDesgloseDiarioPorMes(mes, anio);

            } else if (nombreFuncion.equals("obtenerTopVentasPorMesAnio")) {
                int mes = argumentos.get("mes").asInt();
                int anio = argumentos.get("anio").asInt();
                int limite = argumentos.has("limite") ? argumentos.get("limite").asInt() : 3;

                List<TopProductoDTO> topPasado = dashboardService.obtenerTopVentasPorMesAnio(mes, anio, limite);
                
                if (topPasado.isEmpty()) {
                    return "Sin ventas registradas en " + mes + "/" + anio;
                } else {
                    StringBuilder sb = new StringBuilder("Top " + mes + "/" + anio + ": ");
                    for (TopProductoDTO p : topPasado) {
                        sb.append("[").append(p.nombre()).append(" - Qty: ").append(p.cantidadVendida()).append("] ");
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            log.error("Error parseando argumentos del backend en la función {}: {}", nombreFuncion, e.getMessage());
        }
        return "No hay datos registrados.";
    }

    private ArrayNode construirHerramientas(ObjectMapper mapper) {
        // ... (Tu código de construirHerramientas se mantiene exactamente igual, la lógica JSON está perfecta) ...
        ObjectNode paramsTotales = mapper.createObjectNode();
        ObjectNode propsTotales = mapper.createObjectNode();
        propsTotales.set("mes1", mapper.createObjectNode().put("type", "INTEGER").put("description", "Mes principal (1-12)"));
        propsTotales.set("anio1", mapper.createObjectNode().put("type", "INTEGER").put("description", "Año principal"));
        propsTotales.set("mes2", mapper.createObjectNode().put("type", "INTEGER").put("description", "Segundo mes (Opcional)"));
        propsTotales.set("anio2", mapper.createObjectNode().put("type", "INTEGER").put("description", "Segundo año (Opcional)"));
        paramsTotales.put("type", "OBJECT");
        paramsTotales.set("properties", propsTotales);
        paramsTotales.set("required", mapper.createArrayNode().add("mes1").add("anio1"));
        ObjectNode funcTotales = mapper.createObjectNode().put("name", "obtenerVentasPorMesAnio").put("description", "Consulta ventas totales de un mes y año. Útil para comparar.");
        funcTotales.set("parameters", paramsTotales);

        ObjectNode paramsDiarios = mapper.createObjectNode();
        ObjectNode propsDiarios = mapper.createObjectNode();
        propsDiarios.set("mes", mapper.createObjectNode().put("type", "INTEGER").put("description", "Mes a consultar (1-12)"));
        propsDiarios.set("anio", mapper.createObjectNode().put("type", "INTEGER").put("description", "Año a consultar"));
        paramsDiarios.put("type", "OBJECT");
        paramsDiarios.set("properties", propsDiarios);
        paramsDiarios.set("required", mapper.createArrayNode().add("mes").add("anio"));
        ObjectNode funcDiarios = mapper.createObjectNode().put("name", "obtenerDesgloseDiarioPorMes").put("description", "Consulta ventas día a día de un mes.");
        funcDiarios.set("parameters", paramsDiarios);

        ObjectNode paramsTop = mapper.createObjectNode();
        ObjectNode propsTop = mapper.createObjectNode();
        propsTop.set("mes", mapper.createObjectNode().put("type", "INTEGER").put("description", "Mes a consultar (1-12)"));
        propsTop.set("anio", mapper.createObjectNode().put("type", "INTEGER").put("description", "Año a consultar"));
        propsTop.set("limite", mapper.createObjectNode().put("type", "INTEGER").put("description", "Cantidad del top (ej: 3)"));
        paramsTop.put("type", "OBJECT");
        paramsTop.set("properties", propsTop);
        paramsTop.set("required", mapper.createArrayNode().add("mes").add("anio"));
        ObjectNode funcTop = mapper.createObjectNode().put("name", "obtenerTopVentasPorMesAnio").put("description", "Consulta productos más vendidos de un mes.");
        funcTop.set("parameters", paramsTop);

        return mapper.createArrayNode().add(mapper.createObjectNode().set("functionDeclarations", mapper.createArrayNode().add(funcTotales).add(funcDiarios).add(funcTop)));
    }
}

// Fin de la definición principal de ChatbotCorporativoService.
