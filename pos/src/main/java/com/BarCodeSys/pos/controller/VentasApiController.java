/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de ventas api dentro del sistema POS.
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

import com.BarCodeSys.pos.models.Producto;
import com.BarCodeSys.pos.models.SugerenciaCrossSelling;
import com.BarCodeSys.pos.repository.SugerenciaCrossSellingRepository;
import com.BarCodeSys.pos.security.SecurityUtils;
import com.BarCodeSys.pos.service.ProductoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de VentasApiController.

@RestController
@RequestMapping("/api/ventas")
public class VentasApiController {

    // 1. Refactor: Inyección por Constructor e inmutabilidad (final)
    private final ProductoService productoService;
    private final SugerenciaCrossSellingRepository sugerenciaRepository;

    public VentasApiController(ProductoService productoService, SugerenciaCrossSellingRepository sugerenciaRepository) {
        this.productoService = productoService;
        this.sugerenciaRepository = sugerenciaRepository;
    }

    @GetMapping("/buscar-producto")
    @PreAuthorize("hasAnyRole('CAJERO', 'ADMIN')")
    public ResponseEntity<?> buscarPorCodigo(@RequestParam String codigo) {

        // ASUMIMOS: productoService.buscarPorCodigo(codigo) ya filtra por EmpresaId internamente
        Producto producto = productoService.buscarPorCodigo(codigo).orElse(null);

        if (producto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Producto no encontrado"));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("id", producto.getId());
        response.put("codigo", producto.getCodigoBarras());
        response.put("nombre", producto.getNombre());
        response.put("precioVenta", producto.getPrecioVenta());

        // 2. Refactor: Obtenemos la empresa actual para aislar el Cross-Selling
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();

        // 3. BLINDAJE CORREGIDO: Usamos el método exacto definido en tu repositorio
        List<SugerenciaCrossSelling> sugerencias = sugerenciaRepository
                .findByProductoBaseIdAndActivoTrueAndProductoBase_Empresa_IdOrderByOrigenDesc(producto.getId(), idEmpresa);

        for (SugerenciaCrossSelling sugerencia : sugerencias) {
            Producto prodSugerido = sugerencia.getProductoSugerido();
            Integer stockActual = prodSugerido.getStockActual();

            if (stockActual != null && stockActual > 0) {
                
                Map<String, Object> datosSugerencia = new HashMap<>();
                datosSugerencia.put("id", prodSugerido.getId());
                datosSugerencia.put("codigo", prodSugerido.getCodigoBarras());
                datosSugerencia.put("nombre", prodSugerido.getNombre());
                datosSugerencia.put("precioPromocional", sugerencia.getPrecioPromocional());

                String scriptFinal = sugerencia.getScriptVenta();
                
                if (scriptFinal.toLowerCase().contains("este producto")) {
                    scriptFinal = scriptFinal.replaceAll("(?i)este producto", prodSugerido.getNombre());
                }
                
                String precioFormateado = String.format("S/ %.2f", sugerencia.getPrecioPromocional());
                scriptFinal = scriptFinal + " (Por solo: " + precioFormateado + ")";
                
                datosSugerencia.put("script", scriptFinal);

                response.put("sugerenciaCrossSelling", datosSugerencia);
                
                break; 
            }
        }

        return ResponseEntity.ok(response);
    }
}

// Fin de la definición principal de VentasApiController.
