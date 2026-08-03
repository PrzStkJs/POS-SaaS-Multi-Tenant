/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de venta dentro del sistema POS.
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

import com.BarCodeSys.pos.dto.FormularioVentaDTO;
import com.BarCodeSys.pos.dto.VentaHistorialDTO;
import com.BarCodeSys.pos.exceptions.RecursoNoEncontradoException;
import com.BarCodeSys.pos.models.DetalleVenta;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.models.Usuario;
import com.BarCodeSys.pos.models.Venta;
import com.BarCodeSys.pos.repository.DetalleVentaRepository;
import com.BarCodeSys.pos.repository.UsuarioRepository;
import com.BarCodeSys.pos.security.SecurityUtils;
import com.BarCodeSys.pos.service.VentaService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de VentaController.

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;
    private final UsuarioRepository usuarioRepository;
    private final DetalleVentaRepository detalleVentaRepository;

    // 1. Inyección de dependencias por constructor.
    public VentaController(VentaService ventaService, 
                           UsuarioRepository usuarioRepository, 
                           DetalleVentaRepository detalleVentaRepository) {
        this.ventaService = ventaService;
        this.usuarioRepository = usuarioRepository;
        this.detalleVentaRepository = detalleVentaRepository;
    }

    @PostMapping("/procesar")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> crearVentaCompleta(@RequestBody FormularioVentaDTO request) {
        // El servicio protege la operación con el contexto de seguridad actual.
        Map<String, Object> respuesta = ventaService.procesarVenta(
                request.getVenta(),
                request.getCarrito(),
                request.getMetodoPago() 
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta); // 201 Created
    }

    @PostMapping({"/sincronizar-offline", "/sincronizar"})
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> sincronizarVentasOffline(@RequestBody List<FormularioVentaDTO> ventasOffline) {
        Map<String, Object> respuesta = ventaService.procesarSincronizacionMasiva(ventasOffline);
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping({"/opciones-pago", "/opciones"})
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> obtenerOpcionesPago() {
        // Usamos SecurityUtils en lugar de @AuthenticationPrincipal para mantener el controlador limpio
        Long idUsuario = SecurityUtils.getCurrentUsuarioId();
        Usuario usuarioLogueado = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado en la base de datos."));
        
        Empresa empresa = usuarioLogueado.getEmpresa();
        String pasarelaActiva = normalizarPasarela(empresa.getPasarelaActiva());

        List<Map<String, Object>> metodos = new ArrayList<>();
        metodos.add(metodoPago("MANUAL", "Efectivo / manual", true, "Cobro validado por el cajero."));
        metodos.add(metodoPago(
                "NIUBIZ",
                "Yape / QR Niubiz",
                "NIUBIZ".equals(pasarelaActiva)
                        && tieneTexto(empresa.getNiubizMerchantId())
                        && tieneTexto(empresa.getNiubizApiUser())
                        && tieneTexto(empresa.getNiubizApiPassword()),
                "Requiere Niubiz activo y credenciales completas."
        ));
        metodos.add(metodoPago(
                "MERCADOPAGO",
                "QR Mercado Pago",
                "MERCADOPAGO".equals(pasarelaActiva) && tieneTexto(empresa.getMpAccessToken()),
                "Requiere Mercado Pago activo y Access Token."
        ));

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("pasarelaActiva", pasarelaActiva != null ? pasarelaActiva : "MANUAL");
        respuesta.put("metodos", metodos);
        return ResponseEntity.ok(respuesta);
    }

    @PostMapping("/{id}/confirmar-pago")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    public ResponseEntity<Venta> confirmarPagoPendiente(@PathVariable Long id) {
        // Eliminado el try-catch, delegamos la validación a @ExceptionHandler
        return ResponseEntity.ok(ventaService.confirmarVentaManual(id));
    }

    @GetMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    public ResponseEntity<Map<String, String>> obtenerEstadoVenta(@PathVariable Long id) {
        Venta venta = ventaService.buscarPorId(id);
        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("estado", venta.getEstado().name());
        return ResponseEntity.ok(respuesta);
    }

    @PutMapping("/{id}/anular")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Venta> anularVenta(@PathVariable Long id) {
        // Eliminado el try-catch, delegamos la validación a @ExceptionHandler
        return ResponseEntity.ok(ventaService.anularVenta(id));
    }

    @GetMapping("/{id}/detalles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DetalleVenta>> obtenerDetallesVenta(@PathVariable Long id) {
        // Se delega al servicio la validación de pertenencia a la empresa.
        return ResponseEntity.ok(ventaService.obtenerDetallesSeguros(id));
    }

    @GetMapping("/historial")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VentaHistorialDTO>> getHistorialVentas(
            @RequestParam(required = false) Long ticket,
            @RequestParam(required = false) Long cajeroId,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        LocalDate fechaInicioFiltro = fechaInicio != null ? fechaInicio : LocalDate.now();
        LocalDate fechaFinFiltro = fechaFin != null ? fechaFin : LocalDate.now();
        LocalDateTime inicioDia = fechaInicioFiltro.atStartOfDay();
        LocalDateTime finDia = fechaFinFiltro.atTime(LocalTime.MAX);

        List<VentaHistorialDTO> historial = ventaService.buscarHistorial(
                ticket, cajeroId, estado, inicioDia, finDia
        );

        return ResponseEntity.ok(historial);
    }

    // --- MANEJO DE ERRORES ---
    
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<String> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> manejarErroresDeNegocio(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    // --- MÉTODOS PRIVADOS AUXILIARES ---

    private Map<String, Object> metodoPago(String codigo, String nombre, boolean disponible, String descripcion) {
        Map<String, Object> metodo = new HashMap<>();
        metodo.put("codigo", codigo);
        metodo.put("nombre", nombre);
        metodo.put("disponible", disponible);
        metodo.put("descripcion", descripcion);
        return metodo;
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }

    private String normalizarPasarela(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }

        String normalizado = valor.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        if ("CASH".equals(normalizado) || "EFECTIVO".equals(normalizado)) {
            return "MANUAL";
        }
        if ("YAPE".equals(normalizado)
                || "PLIN".equals(normalizado)
                || "QR_NIUBIZ".equals(normalizado)
                || "YAPE_QR_NIUBIZ".equals(normalizado)) {
            return "NIUBIZ";
        }
        if ("MP".equals(normalizado)
                || "MERCADO_PAGO".equals(normalizado)
                || "QR_MERCADOPAGO".equals(normalizado)) {
            return "MERCADOPAGO";
        }

        return normalizado;
    }
}

// Fin de la definición principal de VentaController.
