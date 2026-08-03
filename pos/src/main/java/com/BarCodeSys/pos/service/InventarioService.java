/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de inventario.
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

import com.BarCodeSys.pos.dto.MovimientoRequestDTO;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.models.MovimientoInventario;
import com.BarCodeSys.pos.models.Producto;
import com.BarCodeSys.pos.models.Usuario;
import com.BarCodeSys.pos.repository.MovimientoInventarioRepository;
import com.BarCodeSys.pos.repository.ProductoRepository;
import com.BarCodeSys.pos.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de InventarioService.

@Service
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public InventarioService(
            ProductoRepository productoRepository,
            MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Transactional
    public void registrarMovimientoManual(MovimientoRequestDTO request) {
        ejecutarMovimientoCentral(
                request,
                SecurityUtils.getCurrentEmpresaId(),
                SecurityUtils.getCurrentUsuarioId()
        );
    }

    @Transactional
    public void registrarMovimientoAutomatizado(MovimientoRequestDTO request, Long idEmpresa, Long idUsuario) {
        ejecutarMovimientoCentral(request, idEmpresa, idUsuario);
    }

    private void ejecutarMovimientoCentral(MovimientoRequestDTO request, Long idEmpresa, Long idUsuario) {
        validarRequest(request);

        Producto producto = productoRepository.findByIdAndEmpresaId(request.getProductoId(), idEmpresa)
                .orElseThrow(() -> new RuntimeException("Acceso denegado: el producto no existe o pertenece a otra empresa."));

        int stockActual = producto.getStockActual() != null ? producto.getStockActual() : 0;
        String tipo = request.getTipo().trim().toUpperCase();

        if ("ENTRADA".equals(tipo)) {
            producto.setStockActual(stockActual + request.getCantidad());
        } else if ("SALIDA".equals(tipo)) {
            producto.setStockActual(stockActual - request.getCantidad());
        } else {
            throw new IllegalArgumentException("Tipo de movimiento invalido. Debe ser ENTRADA o SALIDA.");
        }

        productoRepository.save(producto);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProducto(producto);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(request.getCantidad());
        movimiento.setMotivo(request.getMotivo());
        movimiento.setDescripcion(request.getDescripcion());

        if (idUsuario != null) {
            Usuario usuario = new Usuario();
            usuario.setId(idUsuario);
            movimiento.setUsuario(usuario);
        }

        Empresa empresa = new Empresa();
        empresa.setId(idEmpresa);
        movimiento.setEmpresa(empresa);

        movimientoRepository.save(movimiento);
    }

    private void validarRequest(MovimientoRequestDTO request) {
        if (request == null || request.getProductoId() == null) {
            throw new IllegalArgumentException("Debes indicar un producto.");
        }
        if (request.getTipo() == null || request.getTipo().trim().isEmpty()) {
            throw new IllegalArgumentException("Debes indicar el tipo de movimiento.");
        }
        if (request.getCantidad() == null || request.getCantidad() <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
    }
}

// Fin de la definición principal de InventarioService.
