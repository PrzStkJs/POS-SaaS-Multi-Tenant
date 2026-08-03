/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Implementación de servicio responsable de ejecutar reglas, validaciones e
 *              integraciones de gemini client.
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
import com.BarCodeSys.pos.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de GeminiClient.

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);
    private static final int LIMITE_DIARIO_EMPRESA = 5;

    @Value("${gemini.api.key.chatbot}")
    private String apiKeyChatbot;

    @Value("${gemini.model.chatbot:gemini-3.1-flash-lite}")
    private String modelChatbot;

    @Value("${gemini.api.key.ocr}")
    private String apiKeyOcr;

    @Value("${gemini.model.ocr:gemini-3.1-flash-lite}")
    private String modelOcr;

    @Value("${gemini.api.key.analitica}")
    private String apiKeyAnalitica;

    @Value("${gemini.model.analitica:gemini-3.1-flash-lite}")
    private String modelAnalitica;

    private final EmpresaRepository empresaRepository;
    private final AtomicBoolean cuotaGlobalAgotada = new AtomicBoolean(false);
    private final AtomicReference<LocalDateTime> tiempoBloqueo = new AtomicReference<>(null);

    public GeminiClient(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    public boolean isIaDisponibleGlobal() {
        LocalDateTime bloqueo = tiempoBloqueo.get();
        if (cuotaGlobalAgotada.get() && bloqueo != null && LocalDateTime.now().isAfter(bloqueo.plusHours(12))) {
            cuotaGlobalAgotada.set(false);
            tiempoBloqueo.set(null);
            log.info("Cuota global de Gemini restablecida despues de 12 horas.");
        }
        return !cuotaGlobalAgotada.get();
    }

    public void analizarErrorGlobal(Exception e) {
        String errorMsg = e.getMessage();
        if (e instanceof HttpStatusCodeException httpException) {
            errorMsg = httpException.getResponseBodyAsString();
        }

        if (errorMsg != null && (errorMsg.contains("PerMinute") || errorMsg.contains("retryDelay"))) {
            log.warn("Rate limit temporal de Gemini. No se bloquea la IA globalmente.");
            return;
        }

        if (errorMsg != null && (errorMsg.contains("Quota exceeded") || errorMsg.contains("RESOURCE_EXHAUSTED"))) {
            cuotaGlobalAgotada.set(true);
            tiempoBloqueo.set(LocalDateTime.now());
            log.error("Limite global diario de Gemini alcanzado. Bloqueo de 12h activado.");
        }
    }

    public String construirUrlGemini() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + modelChatbot + ":generateContent?key=" + apiKeyChatbot;
    }

    public String construirUrlOcr() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + modelOcr + ":generateContent?key=" + apiKeyOcr;
    }

    public String construirUrlAnalitica() {
        return "https://generativelanguage.googleapis.com/v1beta/models/" + modelAnalitica + ":generateContent?key=" + apiKeyAnalitica;
    }

    @Transactional
    public boolean tieneCuotaDisponible(Long idEmpresa) {
        if (!isIaDisponibleGlobal()) {
            return false;
        }

        Empresa empresa = empresaRepository.findById(idEmpresa).orElse(null);
        if (empresa == null) {
            return false;
        }

        normalizarCuotaDiaria(empresa);
        return consultasActuales(empresa) < LIMITE_DIARIO_EMPRESA;
    }

    @Transactional
    public void registrarUsoExitoso(Long idEmpresa) {
        empresaRepository.findById(idEmpresa).ifPresent(empresa -> {
            normalizarCuotaDiaria(empresa);
            empresa.setConsultasIaHoy(consultasActuales(empresa) + 1);
            empresaRepository.save(empresa);
            log.debug("Uso de IA registrado. Empresa {}, consultas hoy {}", idEmpresa, empresa.getConsultasIaHoy());
        });
    }

    @Transactional
    public void normalizarCuotaDiaria(Empresa empresa) {
        LocalDate hoy = LocalDate.now();
        if (empresa.getFechaCuotaIa() == null || !empresa.getFechaCuotaIa().isEqual(hoy)) {
            empresa.setFechaCuotaIa(hoy);
            empresa.setConsultasIaHoy(0);
            empresaRepository.save(empresa);
            return;
        }

        if (empresa.getConsultasIaHoy() == null) {
            empresa.setConsultasIaHoy(0);
            empresaRepository.save(empresa);
        }
    }

    private int consultasActuales(Empresa empresa) {
        return empresa.getConsultasIaHoy() != null ? empresa.getConsultasIaHoy() : 0;
    }
}

// Fin de la definición principal de GeminiClient.
