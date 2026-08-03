/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de usuario dentro del sistema POS.
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.BarCodeSys.pos.dto.UsuarioResponseDTO; // 🛡️ Importamos el nuevo DTO
import com.BarCodeSys.pos.exceptions.RecursoNoEncontradoException;
import com.BarCodeSys.pos.models.Usuario;
import com.BarCodeSys.pos.service.UsuarioService;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de UsuarioController.

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    // 1. Inyección de dependencias por constructor.
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // 1. GET - Lista los de su propia empresa (Para ADMIN y CAJERO)
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<List<UsuarioResponseDTO>> listarTodos() {
        return ResponseEntity.ok(usuarioService.listarPorEmpresa());
    }

    // 2. GET SUPER ADMIN - Lista usuarios de una empresa específica
    // Endpoint restringido al rol SUPER_ADMIN.
    @PreAuthorize("hasRole('SUPER_ADMIN')") 
    @GetMapping("/empresa/{idEmpresa}")
    public ResponseEntity<List<UsuarioResponseDTO>> listarPorEmpresaEspecifica(@PathVariable Long idEmpresa) {
        return ResponseEntity.ok(usuarioService.listarPorEmpresaEspecifica(idEmpresa)); 
    }

    // 3. POST - Guardar nuevo usuario (Retorna 201 Created)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> guardar(@RequestBody Usuario usuario) {
        UsuarioResponseDTO nuevoUsuario = usuarioService.guardar(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoUsuario);
    }

    // 4. PUT - Actualizar usuario existente
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<UsuarioResponseDTO> actualizar(@PathVariable Long id, @RequestBody Usuario usuario) {
        return ResponseEntity.ok(usuarioService.actualizar(id, usuario));
    }
    
    // 5. PUT - Cambiar estado (Retorna 204 No Content por ser void)
    @PutMapping("/{id}/cambiar-estado")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id) {
        usuarioService.cambiarEstado(id);
        return ResponseEntity.noContent().build();
    }

    // 6. PUT - Resetear password (Retorna 204 No Content por ser void)
    @PutMapping("/{id}/reset-password")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Usuario payload) {
        usuarioService.resetPassword(id, payload.getPassword());
        return ResponseEntity.noContent().build();
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
}

// Fin de la definición principal de UsuarioController.
