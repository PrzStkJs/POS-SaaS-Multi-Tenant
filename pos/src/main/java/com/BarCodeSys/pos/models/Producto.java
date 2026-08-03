/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Modelo de dominio que representa la estructura, relaciones y reglas base de
 *              producto.
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

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import jakarta.persistence.Transient;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de Producto.

@Entity
@Table(name = "TB_PRODUCTO")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_PRODUCTO")
    private Long id;

    @Column(name = "CODIGO_BARRAS", nullable = false)
    private String codigoBarras;

    @Column(name = "NOMBRE", nullable = false)
    private String nombre;

    @Column(name = "PRECIO_VENTA", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    @Column(name = "STOCK_ACTUAL", nullable = false)
    private Integer stockActual;

    @Column(name = "activo", nullable = false, columnDefinition = "boolean default true")
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_CATEGORIA_FK", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_EMPRESA_FK", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Empresa empresa;
    
    @Transient
    private Map<String, Object> sugerenciaCrossSelling;

    // Constructores
    public Producto() {}
    
    
    // Getters y Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Integer getStockActual() {
        return stockActual;
    }

    public void setStockActual(Integer stockActual) {
        this.stockActual = stockActual;
    }

    // Getter y Setter para el nuevo campo 'activo'
    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }
    
    public Map<String, Object> getSugerenciaCrossSelling() {
        return sugerenciaCrossSelling;
    }

    public void setSugerenciaCrossSelling(Map<String, Object> sugerenciaCrossSelling) {
        this.sugerenciaCrossSelling = sugerenciaCrossSelling;
    }
}

// Fin de la definición principal de Producto.
