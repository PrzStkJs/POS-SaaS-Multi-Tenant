/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de ia corporativa.
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

import com.BarCodeSys.pos.dto.DashboardDTO;
import com.BarCodeSys.pos.dto.StockCriticoDTO;
import com.BarCodeSys.pos.dto.TopProductoDTO;
import com.BarCodeSys.pos.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de IACorporativaService.

@Service
public class IACorporativaService {

    private static final Locale LOCALE_ES = new Locale("es", "ES");
    private static final String ERROR_CUOTA_AGOTADA = "ERROR_CUOTA_AGOTADA";
    private static final String ERROR_LIMITE_API = "ERROR_LIMITE_API";

    private final DashboardService dashboardService;
    private final GeminiClient geminiClient;
    private final ChatbotCorporativoService chatbotService;
    private final ReporteEjecutivoService reporteService;

    public IACorporativaService(DashboardService dashboardService,
                                GeminiClient geminiClient,
                                ChatbotCorporativoService chatbotService,
                                ReporteEjecutivoService reporteService) {
        this.dashboardService = dashboardService;
        this.geminiClient = geminiClient;
        this.chatbotService = chatbotService;
        this.reporteService = reporteService;
    }

    public Map<String, String> consultarAsistente(String preguntaGerente) {
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        Map<String, String> respuesta = new HashMap<>();

        if (!geminiClient.tieneCuotaDisponible(idEmpresa)) {
            respuesta.put("respuesta", ERROR_CUOTA_AGOTADA);
            return respuesta;
        }

        String systemPrompt = construirPromptAsistente();
        
        String respuestaFinal = chatbotService.ejecutarAsistenteFlujoCompleto(systemPrompt, preguntaGerente);

        if (esRespuestaExitosa(respuestaFinal)) {
            geminiClient.registrarUsoExitoso(idEmpresa);
        }

        respuesta.put("respuesta", respuestaFinal);
        return respuesta;
    }

    public Map<String, Object> obtenerDatosGrafico() {
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId(); // Lo conservamos para el insight o logs si es necesario
        List<String> dias = Arrays.asList("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom");
        Double[] ventasReales = new Double[7];
        Double[] prediccionIA = new Double[7];

        LocalDate hoy = LocalDate.now();
        LocalDate lunesDeEstaSemana = hoy.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));

        double sumaVentasPasadas = 0.0;
        int diasConVentasEfectivas = 0;
        int indiceHoy = hoy.getDayOfWeek().getValue() - 1;

        for (int i = 0; i < 7; i++) {
            LocalDate diaEvaluado = lunesDeEstaSemana.plusDays(i);
            if (i <= indiceHoy) {
                LocalDateTime inicioDia = diaEvaluado.atStartOfDay();
                LocalDateTime finDia = (i == indiceHoy) ? LocalDateTime.now() : diaEvaluado.atTime(23, 59, 59);
                try {
                    // El servicio de dashboard resuelve internamente la empresa activa. a dashboardService
                    DashboardDTO kpisDia = dashboardService.obtenerKpis(inicioDia, finDia);
                    BigDecimal totalDiaBd = kpisDia.getTotalVentas();
                    ventasReales[i] = (totalDiaBd != null) ? totalDiaBd.doubleValue() : 0.0;
                } catch (Exception e) {
                    ventasReales[i] = 0.0;
                }
                sumaVentasPasadas += ventasReales[i];
                diasConVentasEfectivas++;
                prediccionIA[i] = (i == indiceHoy) ? ventasReales[i] : null;
            } else {
                ventasReales[i] = null;
                double promedioSemanal = diasConVentasEfectivas > 0 ? (sumaVentasPasadas / diasConVentasEfectivas) : 50.0;
                double factorFinDeSemana = (i >= 4) ? 1.35 : 0.95;
                prediccionIA[i] = Math.round((promedioSemanal * factorFinDeSemana) * 100.0) / 100.0;
            }
        }

        String mensajeInicial = "¡Hola! He analizado tu base de datos y tus gráficas están listas. Escribe en el chat si tienes alguna consulta.";
        // El servicio de dashboard resuelve internamente la empresa activa.
        List<StockCriticoDTO> listaStock = dashboardService.obtenerStockCritico();
        String insight = listaStock.isEmpty()
                ? "Todo en orden: Tus niveles de stock son óptimos."
                : "Atención: Tienes " + listaStock.size() + " productos en zona de riesgo de stock.";

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("labels", dias);
        respuesta.put("ventas", ventasReales);
        respuesta.put("prediccion", prediccionIA);
        respuesta.put("insight", insight);
        respuesta.put("mensajeInicial", mensajeInicial);

        return respuesta;
    }

    public Map<String, String> generarReporteEjecutivo() {
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        Map<String, String> respuesta = new HashMap<>();

        if (!geminiClient.tieneCuotaDisponible(idEmpresa)) {
            respuesta.put("error", "Límite de consultas de IA alcanzado por hoy.");
            return respuesta;
        }

        try {
            LocalDateTime inicioMesActual = YearMonth.now().atDay(1).atStartOfDay();
            LocalDateTime ahora = LocalDateTime.now();
            LocalDateTime inicioMesAnterior = inicioMesActual.minusMonths(1);
            LocalDateTime finMesAnterior = inicioMesActual.minusSeconds(1);

            // La empresa activa se resuelve dentro del servicio de dashboard. de todas estas llamadas
            DashboardDTO kpisActuales = dashboardService.obtenerKpis(inicioMesActual, ahora);
            DashboardDTO kpisAnteriores = dashboardService.obtenerKpis(inicioMesAnterior, finMesAnterior);
            List<StockCriticoDTO> listaStock = dashboardService.obtenerStockCritico();
            List<TopProductoDTO> topVentas = dashboardService.obtenerTopVentas(inicioMesActual, ahora, 5);
            List<String> productosEstancados = dashboardService.obtenerProductosSinRotacion(30);
            
            String historial = construirHistorialReporte();

            String reporteGenerado = reporteService.generarReporteEjecutivo(
                    kpisActuales, kpisAnteriores, listaStock, topVentas, productosEstancados, historial
            );

            if (reporteGenerado != null && !reporteGenerado.contains("ERROR_")) {
                geminiClient.registrarUsoExitoso(idEmpresa);
                respuesta.put("reporte", reporteGenerado);
            } else {
                respuesta.put("error", "Error por límite de API o cuota agotada. Por favor, reintente en unos segundos.");
            }
        } catch (Exception e) {
            System.err.println("Error generando reporte ejecutivo: " + e.getMessage());
            respuesta.put("error", "Ocurrió un error interno al generar el reporte.");
        }

        return respuesta;
    }

    // =========================================================================
    // MÉTODOS PRIVADOS REFACTORIZADOS
    // =========================================================================
    
    private String construirPromptAsistente() {
        LocalDate fechaActual = LocalDate.now();
        LocalDateTime inicioMesActual = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime inicioMesAnterior = inicioMesActual.minusMonths(1);
        LocalDateTime finMesAnterior = inicioMesActual.minusSeconds(1);

        // Las consultas se ejecutan con la empresa activa del contexto de seguridad.
        DashboardDTO kpisActuales = dashboardService.obtenerKpis(inicioMesActual, ahora);
        DashboardDTO kpisAnteriores = dashboardService.obtenerKpis(inicioMesAnterior, finMesAnterior);
        List<StockCriticoDTO> listaStockCritico = dashboardService.obtenerStockCritico();
        List<TopProductoDTO> topVentas = dashboardService.obtenerTopVentas(inicioMesActual, ahora, 3);
        List<String> productosEstancados = dashboardService.obtenerProductosSinRotacion(30);

        String contextoHistorial = construirHistorialAsistente(fechaActual);
        String contextoStock = construirContextoStock(listaStockCritico);
        String contextoTopVentas = construirContextoTopVentas(topVentas);
        String mesActualNombre = fechaActual.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        String strProductosEstancados = productosEstancados.isEmpty()
                ? "Ninguno, inventario 100% sano."
                : String.join(", ", productosEstancados);

        return "Actúa directamente como el Consultor Financiero y Estratégico de la empresa. Eres parte del equipo directivo y mi mano derecha. " +
                "ESTÁ ESTRICTAMENTE PROHIBIDO usar frases como 'Como modelo de lenguaje', 'No puedo implementar', 'Mi función es solo proporcionar datos' o similares. " +
                "Sé proactivo, asume el rol por completo y propón soluciones de negocio en primera persona del plural.\n\n" +

                "REGLA DE INTERACCIÓN PROACTIVA (CRÍTICA): Si el usuario te envía solo un saludo o mensaje corto, " +
                "saluda amigablemente, preséntate como su consultor y dale un 'Flash Update' natural y conversacional comparando el mes actual con el anterior, " +
                "o alertando sobre productos estancados/stock crítico. Luego dale 3 opciones de lo que pueden revisar hoy.\n\n" +

                "REGLAS DE HERRAMIENTAS (CRÍTICO): NUNCA uses herramientas para consultar datos de " + mesActualNombre + ". Usa los datos de abajo.\n\n" +

                "IMPORTANTE: Hoy es " + fechaActual + ".\n\n" +
                "--- MÉTRICAS DE " + mesActualNombre.toUpperCase(LOCALE_ES) + " ---\n" +
                "- Ventas Totales: S/" + kpisActuales.getTotalVentas() + "\n" +
                "- Transacciones: " + kpisActuales.getTotalTransacciones() + "\n" +
                "- Ticket Promedio: S/" + kpisActuales.getTicketPromedio() + "\n\n" +

                "--- MÉTRICAS DEL MES PASADO (Para comparar crecimiento/caída) ---\n" +
                "- Ventas Totales: S/" + kpisAnteriores.getTotalVentas() + "\n" +
                "- Transacciones: " + kpisAnteriores.getTotalTransacciones() + "\n\n" +

                "- Productos SIN ROTACIÓN (Cero ventas en 30 días - Sugiere promos):\n  * " + strProductosEstancados + "\n\n" +
                "- Historial de Ventas (Últimos 7 días):\n" + contextoHistorial + "\n" +
                "- Top 3 Ventas:\n" + contextoTopVentas + "\n" +
                "- Stock Crítico:\n" + contextoStock;
    }

    // Construye historial usando la empresa activa del contexto de seguridad.
    private String construirHistorialAsistente(LocalDate fechaActual) {
        StringBuilder contextoHistorial = new StringBuilder();
        for (int i = 6; i >= 0; i--) {
            LocalDate diaEval = fechaActual.minusDays(i);
            LocalDateTime inicioDia = diaEval.atStartOfDay();
            LocalDateTime finDia = diaEval.atTime(23, 59, 59);
            try {
                // La empresa activa se resuelve dentro del servicio de dashboard.
                DashboardDTO kpisDia = dashboardService.obtenerKpis(inicioDia, finDia);
                BigDecimal totalDia = (kpisDia != null && kpisDia.getTotalVentas() != null)
                        ? kpisDia.getTotalVentas()
                        : BigDecimal.ZERO;
                String nombreDia = diaEval.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES);
                contextoHistorial.append("  * ").append(nombreDia).append(" ").append(diaEval.getDayOfMonth())
                        .append(": S/ ").append(totalDia).append("\n");
            } catch (Exception e) {
                contextoHistorial.append("  * ").append(diaEval).append(": Sin datos\n");
            }
        }
        return contextoHistorial.toString();
    }

    // Construye historial usando la empresa activa del contexto de seguridad.
    private String construirHistorialReporte() {
        StringBuilder historial = new StringBuilder();
        LocalDate fechaActual = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate diaEval = fechaActual.minusDays(i);
            // La empresa activa se resuelve dentro del servicio de dashboard.
            DashboardDTO kpisDia = dashboardService.obtenerKpis(diaEval.atStartOfDay(), diaEval.atTime(23, 59, 59));
            BigDecimal totalDia = (kpisDia != null && kpisDia.getTotalVentas() != null)
                    ? kpisDia.getTotalVentas()
                    : BigDecimal.ZERO;
            historial.append("Día ").append(diaEval.getDayOfMonth()).append(": S/ ").append(totalDia).append("\n");
        }
        return historial.toString();
    }

    private String construirContextoStock(List<StockCriticoDTO> listaStockCritico) {
        StringBuilder contextoStock = new StringBuilder();
        if (listaStockCritico.isEmpty()) {
            contextoStock.append("  * Todo el stock está en niveles óptimos.\n");
        } else {
            for (StockCriticoDTO sc : listaStockCritico) {
                contextoStock.append("  * ").append(sc.nombre())
                        .append(" (Stock actual: ").append(sc.stockActual()).append(" unidades)\n");
            }
        }
        return contextoStock.toString();
    }

    private String construirContextoTopVentas(List<TopProductoDTO> topVentas) {
        StringBuilder contextoTopVentas = new StringBuilder();
        if (topVentas.isEmpty()) {
            contextoTopVentas.append("  * Aún no hay ventas registradas este mes.\n");
        } else {
            for (TopProductoDTO tv : topVentas) {
                contextoTopVentas.append("  * ").append(tv.nombre())
                        .append(" (").append(tv.cantidadVendida()).append(" unidades vendidas)\n");
            }
        }
        return contextoTopVentas.toString();
    }

    private boolean esRespuestaExitosa(String respuestaFinal) {
        return respuestaFinal != null
                && !ERROR_CUOTA_AGOTADA.equals(respuestaFinal)
                && !ERROR_LIMITE_API.equals(respuestaFinal);
    }
}

// Fin de la definición principal de IACorporativaService.
