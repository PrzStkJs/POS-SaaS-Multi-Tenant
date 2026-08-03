/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de categoria dentro del sistema POS.
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

import com.BarCodeSys.pos.models.Categoria;
import com.BarCodeSys.pos.service.CategoriaService;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de CategoriaController.

@RestController
@RequestMapping("/api/categorias")
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // ==========================================
    // ENDPOINT: Listar todas las categorías de la empresa
    // ==========================================
    @GetMapping
    public ResponseEntity<List<Categoria>> listarTodas() {
        return ResponseEntity.ok(categoriaService.listarTodas());
    }

    // ==========================================
    // ENDPOINT: Buscar una categoría por ID
    // ==========================================
    @GetMapping("/{id}")
    public ResponseEntity<Categoria> buscarPorId(@PathVariable Long id) {
        Categoria categoria = categoriaService.buscarPorId(id);
        return ResponseEntity.ok(categoria);
    }

    // ==========================================
    // ENDPOINT: Crear una nueva categoría (POST)
    // ==========================================
    @PostMapping
    public ResponseEntity<Categoria> guardar(@RequestBody Categoria categoria) {
        categoria.setId(null); 
        Categoria nuevaCategoria = categoriaService.guardar(categoria);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaCategoria);
    }

    // ==========================================
    // ENDPOINT: Editar una categoría existente (PUT)
    // ==========================================
    @PutMapping("/{id}")
    public ResponseEntity<Categoria> actualizar(@PathVariable Long id, @RequestBody Categoria categoriaActualizada) {
        // 1. Buscamos la categoría. El servicio valida que exista y pertenezca a la empresa.
        Categoria categoriaExistente = categoriaService.buscarPorId(id);
        
        // 2. Solo actualizamos el nombre (el único campo editable según tu modelo)
        categoriaExistente.setNombre(categoriaActualizada.getNombre());
        
        // 3. Guardamos los cambios
        Categoria categoriaGuardada = categoriaService.guardar(categoriaExistente);
        return ResponseEntity.ok(categoriaGuardada);
    }

    // ==========================================
    // ENDPOINT: Eliminar una categoría (DELETE)
    // ==========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
        return ResponseEntity.noContent().build(); 
    }
}

// Fin de la definición principal de CategoriaController.
