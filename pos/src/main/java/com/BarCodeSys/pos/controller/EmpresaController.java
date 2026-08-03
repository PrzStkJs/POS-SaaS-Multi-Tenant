/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Administración
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de empresa dentro del sistema POS.
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

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Importante para la seguridad
import org.springframework.web.bind.annotation.*;

import com.BarCodeSys.pos.dto.RegistroEmpresaDTO;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.service.EmpresaService;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de EmpresaController.

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    // 1. Inyección por constructor (Práctica Senior)
    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    // 2. BLOQUEO DE SEGURIDAD: Solo un súper administrador del sistema puede ver a todos tus clientes
    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')") // Asumiendo que tienes roles en tu JWT
    public List<Empresa> listarTodas() {
        return empresaService.listarTodas();
    }

    // 3. PÚBLICO: Registro de nueva empresa (Retorna 201 Created)
    @PostMapping
    public ResponseEntity<Empresa> guardar(@RequestBody RegistroEmpresaDTO dto) {
        Empresa nuevaEmpresa = empresaService.guardarConAdmin(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaEmpresa);
    }

    // 4. BLINDADO: El ID lo pone el backend, no el frontend
    @PutMapping("/configuracion")
    public Empresa actualizarConfiguracion(@RequestBody RegistroEmpresaDTO dto) {
        return empresaService.actualizar(dto);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> manejarErrores(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}

// Fin de la definición principal de EmpresaController.
