/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Analítica y Dashboard
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de reporte ejecutivo.
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

// 1. Importaciones para Logging profesional
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de ReporteEjecutivoService.

@Service
public class ReporteEjecutivoService {

    // 2. Logger instanciado
    private static final Logger log = LoggerFactory.getLogger(ReporteEjecutivoService.class);

    // 3. Dependencias inmutables
    private final GeminiClient geminiClient;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    // 4. Inyección por constructor (Optimiza la memoria)
    @Autowired
    public ReporteEjecutivoService(GeminiClient geminiClient, ObjectMapper mapper) {
        this.geminiClient = geminiClient;
        this.mapper = mapper;
        this.restTemplate = new RestTemplate(); // Se crea una sola vez al arrancar el servidor
    }

    // ========== CONSULTA A LA API DE GEMINI ==========
    public String consultarInteligenciaArtificial(String systemPrompt) {
        if (!geminiClient.isIaDisponibleGlobal()) return "ERROR_CUOTA_AGOTADA";
        
        for(int i = 0; i < 2; i++) {
            try {
                String url = geminiClient.construirUrlGemini();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                
                Map<String, Object> requestBody = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", systemPrompt)))));
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
                
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
                JsonNode root = mapper.readTree(response.getBody());
                
                return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
                
            } catch (HttpStatusCodeException e) {
                if(e.getStatusCode().value() == 429 && i == 0) {
                    // 5. Reemplazo de System.out por log.warn
                    log.warn("⏳ Google pide pausa por HTTP 429. Esperando 4 segundos de forma invisible...");
                    try { Thread.sleep(4000); } catch (Exception ignored) {} 
                    continue; 
                }
                geminiClient.analizarErrorGlobal(e);
                return "ERROR_LIMITE_API";
            } catch (Exception e) {
                geminiClient.analizarErrorGlobal(e);
                return "ERROR_LIMITE_API";
            }
        }
        return "ERROR_LIMITE_API";
    }

    // ========== GENERACIÓN DEL REPORTE EJECUTIVO ==========
    public String generarReporteEjecutivo(com.BarCodeSys.pos.dto.DashboardDTO kpisActuales, 
                                          com.BarCodeSys.pos.dto.DashboardDTO kpisAnteriores, 
                                          List<com.BarCodeSys.pos.dto.StockCriticoDTO> stockCritico, 
                                          List<com.BarCodeSys.pos.dto.TopProductoDTO> topVentas, 
                                          List<String> productosEstancados,
                                          String historial) {
        
        // ---------- CONTEXTOS ----------
        String mesActualNombre = java.time.LocalDate.now()
                .getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "ES"));

        StringBuilder contextoStock = new StringBuilder();
        if (stockCritico.isEmpty()) {
            contextoStock.append("Niveles óptimos.");
        } else {
            stockCritico.forEach(sc -> contextoStock.append("- ")
                    .append(sc.nombre()).append(" (Stock: ").append(sc.stockActual()).append(")\n"));
        }

        // ---------- FILTRO DE PRODUCTOS NO RELEVANTES (Doble candado defensivo) ----------
        List<com.BarCodeSys.pos.dto.TopProductoDTO> topVentasLimpio = topVentas.stream()
                .filter(tv -> !tv.nombre().toUpperCase().contains("VENTA RÁPIDA") && 
                              !tv.nombre().toUpperCase().contains("GENERICO"))
                .collect(Collectors.toList());

        StringBuilder contextoTop = new StringBuilder();
        if (topVentasLimpio.isEmpty()) {
            contextoTop.append("Sin ventas registradas.");
        } else {
            topVentasLimpio.forEach(tv -> contextoTop.append("- ")
                    .append(tv.nombre()).append(" (").append(tv.cantidadVendida()).append(" unds)\n"));
        }

        String strProductosEstancados = productosEstancados.isEmpty() 
                ? "Ninguno, inventario 100% sano." 
                : String.join(", ", productosEstancados);

        // ---------- PROMPT PARA LA IA ----------
        String systemPrompt = 
            "Eres el Director Financiero de la empresa. Escribe un Reporte Ejecutivo Mensual de alto nivel.\n" +
            "REGLAS TÉCNICAS ESTRICTAS:\n" +
            "1. NO uses Markdown. Devuelve ÚNICAMENTE código HTML crudo y válido.\n" +
            "2. DEBES usar componentes de Bootstrap 5 para el diseño (cards, row, col, badges, list-group).\n\n" +
            "ESTRUCTURA OBLIGATORIA DEL HTML:\n" +
            "1. RESUMEN: Párrafo introductorio <p class='lead text-muted'> comparando el mes actual con el pasado.\n" +
            "2. KPIs FINANCIEROS: <div class='row mb-4'> con 3 columnas. Cada columna una card text-center con Ventas, Transacciones y Ticket.\n" +
            "3. ANÁLISIS CRUZADO: <div class='row mb-4'> con 3 columnas: Top Ventas, Stock Crítico y Sin Rotación.\n" +
            "4. PLAN DE ACCIÓN: <div class='alert alert-success mt-4'> con viñetas.\n\n" +
            "DATOS:\n" +
            "- Ventas Actuales: S/" + kpisActuales.getTotalVentas() + " (Pasado: S/" + kpisAnteriores.getTotalVentas() + ")\n" +
            "- Transacciones: " + kpisActuales.getTotalTransacciones() + " (Pasado: " + kpisAnteriores.getTotalTransacciones() + ")\n" +
            "- Ticket Promedio: S/" + kpisActuales.getTicketPromedio() + "\n" +
            "- Top Ventas:\n" + contextoTop.toString() + "\n" +
            "- Alertas Stock Crítico:\n" + contextoStock.toString() + "\n" +
            "- Productos SIN ROTACIÓN (30 días):\n" + strProductosEstancados;

        // ---------- LLAMADA Y POST-PROCESAMIENTO ----------
        String respuestaIA = consultarInteligenciaArtificial(systemPrompt);
        
        if (respuestaIA != null && !respuestaIA.contains("ERROR_")) {
            respuestaIA = respuestaIA.replace("```html", "").replace("```", "").trim();
        } else {
            return "<div class='alert alert-warning text-center mt-4'>" +
                   "<h4><i class='bi bi-hourglass-split'></i> Servidor IA Ocupado</h4>" +
                   "<p>Google Gemini está procesando demasiadas peticiones. Cierra esto e intenta en 10 segundos.</p>" +
                   "</div>";
        }
        return respuestaIA;
    }
}

// Fin de la definición principal de ReporteEjecutivoService.
