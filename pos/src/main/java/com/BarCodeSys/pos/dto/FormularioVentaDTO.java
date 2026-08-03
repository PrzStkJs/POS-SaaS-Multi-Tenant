/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Objeto de transferencia de datos utilizado para transportar información de
 *              formulario venta entre capas.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.dto;

import java.util.List;
import com.BarCodeSys.pos.models.Venta;
import com.BarCodeSys.pos.models.DetalleVenta;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de FormularioVentaDTO.

public class FormularioVentaDTO {
    
    private Venta venta;
    private List<DetalleVenta> carrito;
    private String metodoPago;

    public Venta getVenta() {
        return venta;
    }

    public void setVenta(Venta venta) {
        this.venta = venta;
    }

    public List<DetalleVenta> getCarrito() {
        return carrito;
    }

    public void setCarrito(List<DetalleVenta> carrito) {
        this.carrito = carrito;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }
}

// Fin de la definición principal de FormularioVentaDTO.
