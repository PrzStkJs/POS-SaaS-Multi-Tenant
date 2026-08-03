/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de producto dentro del sistema POS.
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

import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.BarCodeSys.pos.exceptions.RecursoNoEncontradoException;
import com.BarCodeSys.pos.models.Categoria;
import com.BarCodeSys.pos.models.Producto;
import com.BarCodeSys.pos.service.CategoriaService;
import com.BarCodeSys.pos.service.ProductoService;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de ProductoController.

@RestController
@RequestMapping("/api/productos")
@PreAuthorize("hasRole('ADMIN')")
public class ProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    // ==========================================
    // ENDPOINT: Listar todos los productos
    // ==========================================
    @GetMapping
    public ResponseEntity<Iterable<Producto>> listarTodos() {
        // El servicio ya filtra automáticamente por la empresa del usuario logueado
        return ResponseEntity.ok(productoService.listarTodos());
    }

    // ==========================================
    // ENDPOINT: Crear un nuevo producto (POST)
    // ==========================================
    @PostMapping
    public ResponseEntity<?> guardar(@RequestBody Producto producto) {
        // El servicio busca por código solo dentro de la empresa actual
        Optional<Producto> productoOpt = productoService.buscarPorCodigo(producto.getCodigoBarras());

        if (productoOpt.isPresent()) {
            Producto prodDb = productoOpt.get();
            
            if (prodDb.getActivo()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Este producto ya está registrado en tu inventario.");
            } else {
                prodDb.setNombre(producto.getNombre());
                prodDb.setPrecioVenta(producto.getPrecioVenta());
                prodDb.setActivo(true);
                
                if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
                    // Nota: Asumimos que categoriaService.buscarPorId también está blindado
                    Categoria categoriaReal = categoriaService.buscarPorId(producto.getCategoria().getId());
                    prodDb.setCategoria(categoriaReal);
                }
                
                return ResponseEntity.ok(productoService.guardar(prodDb));
            }
        }

        if (producto.getCategoria() != null && producto.getCategoria().getId() != null) {
            Categoria categoriaReal = categoriaService.buscarPorId(producto.getCategoria().getId());
            producto.setCategoria(categoriaReal);
        }

        producto.setActivo(true);
        producto.setId(null); // Asegura que sea un INSERT
        
        // El servicio guardar() se encarga de inyectarle la Empresa correcta
        Producto guardado = productoService.guardar(producto);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    // ==========================================
    // ENDPOINT: Buscar producto por código de barras
    // ==========================================
    @GetMapping("/buscar")
    public ResponseEntity<?> buscarPorCodigo(@RequestParam String codigoBarras) {
        return productoService.buscarPorCodigo(codigoBarras)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // ==========================================
    // ENDPOINT: Editar producto completo (PUT)
    // ==========================================
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(
            @PathVariable Long id,
            @RequestBody Producto productoActualizado) {

        // El buscarPorId del servicio ya se asegura de que este ID sea de esta empresa
        Optional<Producto> productoOpt = productoService.buscarPorId(id);

        if (!productoOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Producto no encontrado.");
        }

        Producto productoExistente = productoOpt.get();

        if ("GENERICO".equalsIgnoreCase(productoExistente.getCodigoBarras())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acción denegada: No se puede editar el producto de Venta Rápida.");
        }

        productoExistente.setNombre(productoActualizado.getNombre());
        productoExistente.setPrecioVenta(productoActualizado.getPrecioVenta());

        if (productoActualizado.getCategoria() != null && productoActualizado.getCategoria().getId() != null) {
            Categoria categoriaReal = categoriaService.buscarPorId(productoActualizado.getCategoria().getId());
            productoExistente.setCategoria(categoriaReal);
        }

        return ResponseEntity.ok(productoService.guardar(productoExistente));
    }

    // ==========================================
    // ENDPOINT: Eliminar producto (DELETE)
    // ==========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        
        Optional<Producto> productoOpt = productoService.buscarPorId(id);

        if (!productoOpt.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Producto no encontrado.");
        }

        if ("GENERICO".equalsIgnoreCase(productoOpt.get().getCodigoBarras())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Acción denegada: No se puede eliminar el producto base de Venta Rápida del sistema.");
        }

        try {
            // El servicio ya hace el orElseThrow, el ajuste de stock y guarda el movimiento
            productoService.eliminar(id);
            return ResponseEntity.ok("Producto eliminado exitosamente y stock reducido a 0.");
            
        } catch (RecursoNoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Ocurrió un error inesperado al intentar eliminar el producto.");
        }
    }

   
}

// Fin de la definición principal de ProductoController.
