/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Modelo de dominio que representa la estructura, relaciones y reglas base de
 *              sugerencia cross selling.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de SugerenciaCrossSelling.

@Entity
@Table(name = "sugerencias_cross_selling")
public class SugerenciaCrossSelling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El producto que dispara la regla (Ej: Snack Los Cuates)
    @ManyToOne
    @JoinColumn(name = "producto_base_id", nullable = false)
    private Producto productoBase;

    // El producto que el sistema le sugerirá al cajero (Ej: Gaseosa Guaraná)
    @ManyToOne
    @JoinColumn(name = "producto_sugerido_id", nullable = false)
    private Producto productoSugerido;

    // El texto exacto que el cajero debe leer al cliente
    @Column(name = "script_venta", nullable = false, length = 255)
    private String scriptVenta;

    // (Opcional) Si la sugerencia tiene un precio de oferta
    @Column(name = "precio_promocional")
    private BigDecimal precioPromocional;

    // Para activar o desactivar la promoción sin borrarla de la base de datos
    @Column(name = "estado_activo")
    private boolean activo = true;
    
    @Column(name = "origen", length = 20)
    private String origen;

    public SugerenciaCrossSelling() {
    }

    // ==========================================
    // Genera aquí tus Getters y Setters
    // ==========================================
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Producto getProductoBase() { return productoBase; }
    public void setProductoBase(Producto productoBase) { this.productoBase = productoBase; }
    
    public Producto getProductoSugerido() { return productoSugerido; }
    public void setProductoSugerido(Producto productoSugerido) { this.productoSugerido = productoSugerido; }
    
    public String getScriptVenta() { return scriptVenta; }
    public void setScriptVenta(String scriptVenta) { this.scriptVenta = scriptVenta; }
    
    public BigDecimal getPrecioPromocional() { return precioPromocional; }
    public void setPrecioPromocional(BigDecimal precioPromocional) { this.precioPromocional = precioPromocional; }
    
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    
    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }
}

// Fin de la definición principal de SugerenciaCrossSelling.
