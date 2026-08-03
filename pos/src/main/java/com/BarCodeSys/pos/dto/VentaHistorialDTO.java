/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Objeto de transferencia de datos utilizado para transportar información de
 *              venta historial entre capas.
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de VentaHistorialDTO.

public class VentaHistorialDTO {
    private Long idTicket;
    private LocalDateTime fechaHora;
    private BigDecimal total;
    private String cajero;
    private String estado;

    // Constructor que usaremos en la consulta de BD
    public VentaHistorialDTO(Long idTicket, LocalDateTime fechaHora, BigDecimal total, String cajero, String estado) {
        this.idTicket = idTicket;
        this.fechaHora = fechaHora;
        this.total = total;
        this.cajero = cajero;
        this.estado = estado;
    }

    // Genera aquí los Getters y Setters
    public Long getIdTicket() { return idTicket; }
    public void setIdTicket(Long idTicket) { this.idTicket = idTicket; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public String getCajero() { return cajero; }
    public void setCajero(String cajero) { this.cajero = cajero; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}

// Fin de la definición principal de VentaHistorialDTO.
