/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de sugerencia cross selling dentro del sistema POS.
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
import com.BarCodeSys.pos.repository.ProductoRepository;
import com.BarCodeSys.pos.repository.SugerenciaCrossSellingRepository;
import com.BarCodeSys.pos.security.UsuarioPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de SugerenciaCrossSellingRestController.

@RestController
@RequestMapping("/api/promociones")
@PreAuthorize("hasRole('ADMIN')")
public class SugerenciaCrossSellingRestController {

    private final SugerenciaCrossSellingRepository repository;
    private final ProductoRepository productoRepository;

    public SugerenciaCrossSellingRestController(
            SugerenciaCrossSellingRepository repository,
            ProductoRepository productoRepository) {
        this.repository = repository;
        this.productoRepository = productoRepository;
    }

    @GetMapping
    public List<SugerenciaCrossSelling> listarTodas(@AuthenticationPrincipal UsuarioPrincipal principal) {
        return repository.findByActivoTrueAndProductoBase_Empresa_Id(principal.getIdEmpresa());
    }

    @PostMapping
    public ResponseEntity<?> guardarPromocion(
            @RequestBody SugerenciaCrossSelling promocion,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        try {
            Long idEmpresa = principal.getIdEmpresa();
            Long idProductoBase = obtenerProductoId(promocion.getProductoBase());
            Long idProductoSugerido = obtenerProductoId(promocion.getProductoSugerido());

            if (idProductoBase.equals(idProductoSugerido)) {
                return ResponseEntity.badRequest().body("La promocion necesita dos productos distintos.");
            }

            Producto productoBase = productoRepository.findByIdAndEmpresaId(idProductoBase, idEmpresa)
                    .orElseThrow(() -> new IllegalArgumentException("El producto base no existe o no pertenece a tu empresa."));
            Producto productoSugerido = productoRepository.findByIdAndEmpresaId(idProductoSugerido, idEmpresa)
                    .orElseThrow(() -> new IllegalArgumentException("El producto sugerido no existe o no pertenece a tu empresa."));

            SugerenciaCrossSelling promocionSegura = new SugerenciaCrossSelling();
            promocionSegura.setProductoBase(productoBase);
            promocionSegura.setProductoSugerido(productoSugerido);
            promocionSegura.setPrecioPromocional(validarPrecio(promocion.getPrecioPromocional()));
            promocionSegura.setScriptVenta(limitarTexto(promocion.getScriptVenta(), 255));
            promocionSegura.setOrigen("MANUAL");
            promocionSegura.setActivo(true);

            SugerenciaCrossSelling guardada = repository.save(promocionSegura);
            return ResponseEntity.status(HttpStatus.CREATED).body(guardada);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al guardar: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPromocion(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        try {
            return repository.findByIdAndProductoBase_Empresa_Id(id, principal.getIdEmpresa())
                    .map(promocion -> {
                        promocion.setActivo(false);
                        repository.save(promocion);
                        return ResponseEntity.ok().body("{\"status\": \"success\", \"message\": \"Promocion desactivada\"}");
                    })
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al desactivar la promocion: " + e.getMessage());
        }
    }

    private Long obtenerProductoId(Producto producto) {
        if (producto == null || producto.getId() == null) {
            throw new IllegalArgumentException("Debes seleccionar ambos productos.");
        }
        return producto.getId();
    }

    private BigDecimal validarPrecio(BigDecimal precio) {
        if (precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio promocional debe ser mayor a cero.");
        }
        return precio;
    }

    private String limitarTexto(String texto, int maximo) {
        String limpio = texto == null || texto.trim().isEmpty()
                ? "Lleve tambien este producto a un precio especial."
                : texto.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}

// Fin de la definición principal de SugerenciaCrossSellingRestController.
