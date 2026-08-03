/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de producto.
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

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.BarCodeSys.pos.exceptions.RecursoNoEncontradoException;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.models.MovimientoInventario;
import com.BarCodeSys.pos.models.Producto;
import com.BarCodeSys.pos.models.Usuario;
import com.BarCodeSys.pos.repository.MovimientoInventarioRepository;
import com.BarCodeSys.pos.repository.ProductoRepository;
import com.BarCodeSys.pos.security.SecurityUtils;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de ProductoService.

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public ProductoService(ProductoRepository productoRepository,
                           MovimientoInventarioRepository movimientoInventarioRepository) {
        this.productoRepository = productoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    // ==========================================
    // MÉTODO AUXILIAR DE BLINDAJE
    // ==========================================
    private Long getEmpresaIdSeguro() {
        Long empresaId = SecurityUtils.getCurrentEmpresaId();

        if (empresaId == null) {
            throw new RuntimeException(
                "Acceso denegado: No se pudo identificar la empresa del usuario actual."
            );
        }

        return empresaId;
    }

    // ==========================================
    // MÉTODOS DE NEGOCIO
    // ==========================================

    public Optional<Producto> buscarPorCodigo(String codigoBarras) {
        return productoRepository.findByCodigoBarrasAndEmpresaId(
                codigoBarras,
                getEmpresaIdSeguro()
        );
    }

    public Optional<Producto> buscarPorId(Long id) {
        return productoRepository.findByIdAndEmpresaId(
                id,
                getEmpresaIdSeguro()
        );
    }

    // SOLO LISTA PRODUCTOS ACTIVOS
    public List<Producto> listarTodos() {
        return productoRepository.findByEmpresaIdAndActivoTrue(
                getEmpresaIdSeguro()
        );
    }

    @Transactional
    public Producto guardar(Producto producto) {

        Long empresaId = getEmpresaIdSeguro();

        if (producto.getId() != null) {
            productoRepository.findByIdAndEmpresaId(producto.getId(), empresaId)
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Inconsistencia: El producto no existe o pertenece a otra empresa."));
        }

        Empresa empresa = new Empresa();
        empresa.setId(empresaId);
        producto.setEmpresa(empresa);

        return productoRepository.save(producto);
    }

    @Transactional
    public void eliminar(Long idProducto) {

        Long idEmpresa = getEmpresaIdSeguro();
        Long idUsuario = SecurityUtils.getCurrentUsuarioId();

        if (idUsuario == null) {
            throw new RuntimeException(
                    "Acceso denegado: No se pudo identificar al usuario.");
        }

        Producto producto = productoRepository
                .findByIdAndEmpresaId(idProducto, idEmpresa)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "El producto que intentas eliminar no existe o no te pertenece."));

        int stockAnterior = producto.getStockActual() != null
                ? producto.getStockActual()
                : 0;

        // Eliminación lógica
        producto.setActivo(false);
        producto.setStockActual(0);

        productoRepository.save(producto);

        if (stockAnterior > 0) {

            MovimientoInventario mov = new MovimientoInventario();

            mov.setProducto(producto);

            Usuario usuario = new Usuario();
            usuario.setId(idUsuario);
            mov.setUsuario(usuario);

            Empresa empresa = new Empresa();
            empresa.setId(idEmpresa);
            mov.setEmpresa(empresa);

            mov.setTipo("SALIDA");
            mov.setCantidad(stockAnterior);
            mov.setMotivo("AJUSTE_INVENTARIO");
            mov.setDescripcion(
                    "Ajuste por eliminación (De " + stockAnterior + " a 0)");

            movimientoInventarioRepository.save(mov);
        }
    }
}

// Fin de la definición principal de ProductoService.
