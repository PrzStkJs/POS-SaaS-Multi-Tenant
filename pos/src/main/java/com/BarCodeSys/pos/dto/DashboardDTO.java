/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Analítica y Dashboard
 * DESCRIPCIÓN: Objeto de transferencia de datos utilizado para transportar información de
 *              dashboard entre capas.
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

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de DashboardDTO.

public class DashboardDTO {
    private BigDecimal totalVentas;
    private int totalTransacciones;
    private BigDecimal ticketPromedio;
    private int stockCritico;

    public DashboardDTO(BigDecimal totalVentas, int totalTransacciones, BigDecimal ticketPromedio, int stockCritico) {
        this.totalVentas = totalVentas;
        this.totalTransacciones = totalTransacciones;
        this.ticketPromedio = ticketPromedio;
        this.stockCritico = stockCritico;
    }

    // Getters y Setters
    public BigDecimal getTotalVentas() { return totalVentas; }
    public void setTotalVentas(BigDecimal totalVentas) { this.totalVentas = totalVentas; }
    public int getTotalTransacciones() { return totalTransacciones; }
    public void setTotalTransacciones(int totalTransacciones) { this.totalTransacciones = totalTransacciones; }
    public BigDecimal getTicketPromedio() { return ticketPromedio; }
    public void setTicketPromedio(BigDecimal ticketPromedio) { this.ticketPromedio = ticketPromedio; }
    public int getStockCritico() { return stockCritico; }
    public void setStockCritico(int stockCritico) { this.stockCritico = stockCritico; }
}

// Fin de la definición principal de DashboardDTO.
