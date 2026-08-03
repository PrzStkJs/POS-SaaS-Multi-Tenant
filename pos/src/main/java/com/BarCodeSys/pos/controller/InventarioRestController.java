/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de inventario dentro del sistema POS.
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

import com.BarCodeSys.pos.dto.MovimientoRequestDTO;
import com.BarCodeSys.pos.service.InventarioService;
import jakarta.validation.Valid; 
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de InventarioRestController.

@RestController
@RequestMapping("/api/inventario")
@PreAuthorize("hasRole('ADMIN')")
public class InventarioRestController {

    private final InventarioService inventarioService;

    public InventarioRestController(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @PostMapping("/movimiento")
    // BLINDADO: Ya no necesitamos inyectar el @AuthenticationPrincipal aquí
    public ResponseEntity<Map<String, String>> registrarMovimiento(@Valid @RequestBody MovimientoRequestDTO request) {

        // Solo le pasamos el request. El servicio obtendrá la empresa y el usuario en secreto
        inventarioService.registrarMovimientoManual(request);
        
        return ResponseEntity.ok(Map.of("mensaje", "Movimiento registrado correctamente"));
    }
}

// Fin de la definición principal de InventarioRestController.
