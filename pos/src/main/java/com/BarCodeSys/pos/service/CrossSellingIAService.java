/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de cross selling ia.
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

import com.BarCodeSys.pos.models.DetalleVenta;
import com.BarCodeSys.pos.models.Producto;
import com.BarCodeSys.pos.models.SugerenciaCrossSelling;
import com.BarCodeSys.pos.models.Venta;
import com.BarCodeSys.pos.repository.DetalleVentaRepository;
import com.BarCodeSys.pos.repository.ProductoRepository;
import com.BarCodeSys.pos.repository.SugerenciaCrossSellingRepository;
import com.BarCodeSys.pos.repository.VentaRepository;
import com.BarCodeSys.pos.security.SecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de CrossSellingIAService.

@Service
public class CrossSellingIAService {

    private static final Logger log = LoggerFactory.getLogger(CrossSellingIAService.class);
    private static final int MAX_SCRIPT_LENGTH = 255;

    private final GeminiClient geminiClient;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final SugerenciaCrossSellingRepository sugerenciaRepository;
    private final ObjectMapper mapper;
    private final RestTemplate restTemplate;

    private final Map<Long, AtomicBoolean> banderasCancelacion = new ConcurrentHashMap<>();

    public CrossSellingIAService(
            GeminiClient geminiClient,
            VentaRepository ventaRepository,
            DetalleVentaRepository detalleVentaRepository,
            ProductoRepository productoRepository,
            SugerenciaCrossSellingRepository sugerenciaRepository,
            ObjectMapper mapper) {
        this.geminiClient = geminiClient;
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoRepository = productoRepository;
        this.sugerenciaRepository = sugerenciaRepository;
        this.mapper = mapper;
        this.restTemplate = new RestTemplate();
    }

    public void cancelarProceso() {
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        banderasCancelacion.computeIfAbsent(idEmpresa, k -> new AtomicBoolean(false)).set(true);
        log.info("Generacion de promociones cancelada para empresa {}", idEmpresa);
    }

    public Map<String, Object> generarPromocionesAutomaticas(int cantidad) {
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        Map<String, Object> respuesta = new HashMap<>();

        if (!geminiClient.tieneCuotaDisponible(idEmpresa)) {
            respuesta.put("status", "error");
            respuesta.put("message", "Limite de consultas de IA alcanzado por hoy.");
            return respuesta;
        }

        AtomicBoolean procesoCancelado = banderasCancelacion.computeIfAbsent(idEmpresa, k -> new AtomicBoolean(false));
        procesoCancelado.set(false);

        try {
            Pageable limite = PageRequest.of(0, 500, Sort.by(Sort.Direction.DESC, "id"));
            List<Venta> ventas = ventaRepository.findByEmpresaId(idEmpresa, limite).getContent();

            if (ventas.isEmpty()) {
                respuesta.put("status", "error");
                respuesta.put("message", "No hay suficientes ventas para analizar.");
                return respuesta;
            }

            String transacciones = construirTransacciones(ventas);
            if (transacciones.isBlank()) {
                respuesta.put("status", "error");
                respuesta.put("message", "Las ventas no tienen productos suficientes para generar promociones.");
                return respuesta;
            }

            String exclusiones = construirExclusionesManuales(idEmpresa);
            String prompt = construirPrompt(cantidad, transacciones, exclusiones);
            String jsonResponse = consultarGemini(prompt);

            if (procesoCancelado.get()) {
                respuesta.put("status", "cancelled");
                respuesta.put("message", "Proceso detenido. No se guardaron promociones.");
                return respuesta;
            }

            int procesados = procesarYGuardarSugerencias(jsonResponse, idEmpresa);
            geminiClient.registrarUsoExitoso(idEmpresa);

            respuesta.put("status", "success");
            respuesta.put("message", "Analisis completado. Se generaron " + procesados + " nuevas reglas.");
        } catch (Exception e) {
            log.error("Error generando promociones IA: {}", e.getMessage(), e);
            geminiClient.analizarErrorGlobal(e);
            respuesta.put("status", "error");
            respuesta.put("message", "Error al procesar la IA: " + e.getMessage());
        }

        return respuesta;
    }

    @Transactional
    public int procesarYGuardarSugerencias(String jsonResponse, Long idEmpresa) {
        JsonNode sugerenciasNode = extraerSugerencias(jsonResponse);
        if (!sugerenciasNode.isArray()) {
            throw new RuntimeException("La IA no devolvio una lista JSON valida.");
        }

        sugerenciaRepository.deleteByOrigenAndEmpresaId("IA", idEmpresa);

        int contador = 0;
        for (JsonNode node : sugerenciasNode) {
            if (guardarSugerenciaSiEsValida(node, idEmpresa)) {
                contador++;
            }
        }

        return contador;
    }

    private String construirTransacciones(List<Venta> ventas) {
        StringBuilder transacciones = new StringBuilder();

        for (Venta venta : ventas) {
            List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(venta.getId());
            String ticket = detalles.stream()
                    .filter(detalle -> detalle.getProducto() != null)
                    .filter(detalle -> detalle.getProducto().getCodigoBarras() != null)
                    .map(detalle -> detalle.getProducto().getCodigoBarras()
                            + " (" + detalle.getProducto().getNombre()
                            + " - S/ " + detalle.getProducto().getPrecioVenta() + ")")
                    .collect(Collectors.joining(", "));

            if (!ticket.isBlank()) {
                transacciones.append("[").append(ticket).append("]\n");
            }
        }

        return transacciones.toString();
    }

    private String construirExclusionesManuales(Long idEmpresa) {
        return sugerenciaRepository.findByProductoBase_Empresa_Id(idEmpresa)
                .stream()
                .filter(promocion -> "MANUAL".equalsIgnoreCase(promocion.getOrigen()))
                .map(promocion -> "- No reemplaces manual: "
                        + promocion.getProductoBase().getCodigoBarras()
                        + " -> "
                        + promocion.getProductoSugerido().getCodigoBarras())
                .collect(Collectors.joining("\n"));
    }

    private String construirPrompt(int cantidad, String transacciones, String exclusiones) {
        return """
                Eres experto en retail y market basket analysis para un minimarket.
                Con estas ventas, genera exactamente %d promociones cross-selling logicas.

                Reglas:
                - Usa solo codigos que aparezcan en las ventas.
                - No inventes codigos.
                - No repitas la misma pareja base/sugerido.
                - No uses el mismo producto como base y sugerido.
                - El precioPromocional debe ser mayor que 0 y menor al precio normal del producto sugerido.
                - scriptVenta debe ser corto, maximo 120 caracteres, sin HTML.
                - Devuelve solo un arreglo JSON puro, sin markdown.

                Formato exacto:
                [
                  {"codigoBase":"CODIGO", "codigoSugerido":"CODIGO", "precioPromocional": 2.50, "scriptVenta":"Lleve tambien este producto a precio especial."}
                ]

                Promociones manuales protegidas:
                %s

                Ventas:
                %s
                """.formatted(cantidad, exclusiones == null || exclusiones.isBlank() ? "(ninguna)" : exclusiones, transacciones);
    }

    private String consultarGemini(String prompt) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ObjectNode requestNode = mapper.createObjectNode();
        requestNode.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", prompt);

        HttpEntity<String> requestEntity = new HttpEntity<>(mapper.writeValueAsString(requestNode), headers);
        return restTemplate.postForObject(geminiClient.construirUrlAnalitica(), requestEntity, String.class);
    }

    private JsonNode extraerSugerencias(String jsonResponse) {
        try {
            JsonNode rootNode = mapper.readTree(jsonResponse);
            JsonNode parts = rootNode.path("candidates").path(0).path("content").path("parts");
            String iaText = parts.path(0).path("text").asText("");

            iaText = iaText
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();

            int inicio = iaText.indexOf('[');
            int fin = iaText.lastIndexOf(']');
            if (inicio >= 0 && fin > inicio) {
                iaText = iaText.substring(inicio, fin + 1);
            }

            return mapper.readTree(iaText);
        } catch (Exception e) {
            throw new RuntimeException("Respuesta de IA sin JSON valido.", e);
        }
    }

    private boolean guardarSugerenciaSiEsValida(JsonNode node, Long idEmpresa) {
        try {
            String codigoBase = texto(node, "codigoBase");
            String codigoSugerido = texto(node, "codigoSugerido");
            if (codigoBase.isBlank() || codigoSugerido.isBlank() || codigoBase.equals(codigoSugerido)) {
                return false;
            }

            Optional<Producto> prodBaseOpt = productoRepository.findByCodigoBarrasAndEmpresaId(codigoBase, idEmpresa);
            Optional<Producto> prodSugOpt = productoRepository.findByCodigoBarrasAndEmpresaId(codigoSugerido, idEmpresa);
            if (prodBaseOpt.isEmpty() || prodSugOpt.isEmpty()) {
                log.warn("IA propuso codigos inexistentes para empresa {}: {} -> {}", idEmpresa, codigoBase, codigoSugerido);
                return false;
            }

            Producto prodBase = prodBaseOpt.get();
            Producto prodSugerido = prodSugOpt.get();
            if (sugerenciaRepository.existsByProductoBaseIdAndProductoSugeridoIdAndActivoTrue(prodBase.getId(), prodSugerido.getId())) {
                return false;
            }

            BigDecimal precioPromo = resolverPrecioPromocional(node.path("precioPromocional"), prodSugerido.getPrecioVenta());
            String scriptVenta = limitarTexto(texto(node, "scriptVenta"), MAX_SCRIPT_LENGTH);

            SugerenciaCrossSelling nueva = new SugerenciaCrossSelling();
            nueva.setProductoBase(prodBase);
            nueva.setProductoSugerido(prodSugerido);
            nueva.setScriptVenta(scriptVenta);
            nueva.setPrecioPromocional(precioPromo);
            nueva.setActivo(true);
            nueva.setOrigen("IA");

            sugerenciaRepository.save(nueva);
            return true;
        } catch (Exception e) {
            log.warn("Sugerencia IA descartada: {}", e.getMessage());
            return false;
        }
    }

    private BigDecimal resolverPrecioPromocional(JsonNode precioNode, BigDecimal precioOriginal) {
        BigDecimal precioFinal;
        try {
            precioFinal = precioNode.isNumber()
                    ? precioNode.decimalValue()
                    : new BigDecimal(precioNode.asText("0"));
        } catch (Exception e) {
            precioFinal = BigDecimal.ZERO;
        }

        if (precioOriginal == null || precioOriginal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Producto sugerido sin precio valido.");
        }

        if (precioFinal.compareTo(BigDecimal.ZERO) <= 0 || precioFinal.compareTo(precioOriginal) >= 0) {
            precioFinal = precioOriginal.multiply(new BigDecimal("0.90"));
        }

        return precioFinal.setScale(2, RoundingMode.HALF_UP);
    }

    private String texto(JsonNode node, String campo) {
        return node.path(campo).asText("").trim();
    }

    private String limitarTexto(String texto, int maximo) {
        String limpio = texto == null || texto.isBlank()
                ? "Lleve tambien este producto a precio especial."
                : texto.replaceAll("<[^>]*>", "").trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}

// Fin de la definición principal de CrossSellingIAService.
