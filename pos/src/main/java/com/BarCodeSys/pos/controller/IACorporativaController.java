/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de ia corporativa dentro del sistema POS.
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

import com.BarCodeSys.pos.dto.ProyeccionDemandaDTO;
import com.BarCodeSys.pos.service.IACorporativaService;
import com.BarCodeSys.pos.service.PrediccionDemandaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.BarCodeSys.pos.service.CrossSellingIAService;

import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de IACorporativaController.

@RestController
@RequestMapping("/api/ia")
@PreAuthorize("hasRole('ADMIN')")
public class IACorporativaController {

    private final IACorporativaService iaCorporativaService;
    private final PrediccionDemandaService prediccionDemandaService;
    private final CrossSellingIAService crossSellingIAService;

    // Inyectamos TODOS los servicios en el constructor
    public IACorporativaController(IACorporativaService iaCorporativaService, 
            PrediccionDemandaService prediccionDemandaService,
            CrossSellingIAService crossSellingIAService) { 
        this.iaCorporativaService = iaCorporativaService;
        this.prediccionDemandaService = prediccionDemandaService;
        this.crossSellingIAService = crossSellingIAService; 
    }

    // =======================================================
    // MÉTODOS REFACTORIZADOS (Limpios y seguros)
    // =======================================================

    @PostMapping("/consultar")
    // Eliminamos @AuthenticationPrincipal, el servicio hace la magia
    public Map<String, String> consultarAsistente(@RequestBody Map<String, String> payload) {
        return iaCorporativaService.consultarAsistente(payload.get("pregunta"));
    }

    @GetMapping("/grafico-ventas")
    // Eliminamos @AuthenticationPrincipal
    public Map<String, Object> obtenerDatosGrafico() {
        return iaCorporativaService.obtenerDatosGrafico();
    }

    @GetMapping("/reporte-ejecutivo")
    // Eliminamos @AuthenticationPrincipal
    public Map<String, String> generarReporteEjecutivo() {
        return iaCorporativaService.generarReporteEjecutivo();
    }
    
    // =======================================================
    // MÉTODO AHORA SÍ REFACTORIZADO Y BLINDADO
    // =======================================================

    @GetMapping("/proyeccion-inventario")
    public List<ProyeccionDemandaDTO> obtenerProyeccionInventario() {
        // Ya no enviamos parámetros, el servicio obtiene el idEmpresa de SecurityUtils
        return prediccionDemandaService.obtenerProyeccionInventario();
    }
    
    // =======================================================
    // MÉTODOS REFACTORIZADOS (Cross-Selling IA)
    // =======================================================

    @PostMapping("/generar-ofertas")
    // Eliminamos @AuthenticationPrincipal, el servicio extrae la empresa internamente
    public Map<String, Object> generarOfertasCrossSelling(@RequestParam(defaultValue = "3") int cantidad) {
        // Ya no enviamos el idEmpresa, solo la cantidad
        return crossSellingIAService.generarPromocionesAutomaticas(cantidad);
    }

    @PostMapping("/cancelar-generacion")
    // Eliminamos @AuthenticationPrincipal
    public ResponseEntity<?> cancelarGeneracion() {
        // Llamamos al método sin enviarle parámetros
        crossSellingIAService.cancelarProceso();
        return ResponseEntity.ok().body("{\"status\": \"cancelled\", \"message\": \"Señal de cancelación recibida.\"}");
    }
}

// Fin de la definición principal de IACorporativaController.
