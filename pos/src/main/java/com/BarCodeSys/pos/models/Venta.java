/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Modelo de dominio que representa la estructura, relaciones y reglas base de
 *              venta.
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
import java.time.LocalDateTime;
import jakarta.persistence.*;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de Venta.

@Entity
@Table(name = "TB_VENTA")
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_VENTA")
    private Long id;

    
    @Column(name = "FECHA_HORA", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "TOTAL", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "METODO_PAGO", nullable = false, length = 30, columnDefinition = "varchar(30) default 'MANUAL'")
    private String metodoPago = "MANUAL";

    @Column(name = "PASARELA_USADA", nullable = false, length = 20, columnDefinition = "varchar(20) default 'MANUAL'")
    private String pasarelaUsada = "MANUAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_USUARIO_FK", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_EMPRESA_FK", nullable = false)
    private Empresa empresa;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoVenta estado;
    
    
    @PrePersist
    protected void onCreate() {
        if (this.estado == null) {
            this.estado = EstadoVenta.COMPLETADA; // Por defecto toda venta nace completada
        }
        if (this.metodoPago == null || this.metodoPago.isBlank()) {
            this.metodoPago = "MANUAL";
        }
        if (this.pasarelaUsada == null || this.pasarelaUsada.isBlank()) {
            this.pasarelaUsada = "MANUAL";
        }
    }
    
    
    // Constructores
    public Venta() {}
    
    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getPasarelaUsada() { return pasarelaUsada; }
    public void setPasarelaUsada(String pasarelaUsada) { this.pasarelaUsada = pasarelaUsada; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
}

// Fin de la definición principal de Venta.
