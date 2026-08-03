/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Administración
 * DESCRIPCIÓN: Modelo de dominio que representa la estructura, relaciones y reglas base de
 *              empresa.
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
import java.time.LocalDate;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de Empresa.

@Entity
@Table(name = "TB_EMPRESA")
public class Empresa {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EMPRESA")
    private Long id;

    @Column(name = "NOMBRE_COMERCIAL", nullable = false)
    private String nombreComercial;

    @Column(name = "EMAIL_CONTACTO", length = 255)
    private String emailContacto;

    @Column(name = "LIMITE_USUARIOS", nullable = false, columnDefinition = "integer default 3")
    private Integer limiteUsuarios;
    
    @Column(name = "FECHA_CUOTA_IA")
    private LocalDate fechaCuotaIa;
    
    @Column(name = "CONSULTAS_IA_HOY", columnDefinition = "integer default 0")
    private Integer consultasIaHoy = 0;

    // Nace como MANUAL por defecto
    @Column(name = "PASARELA_ACTIVA", length = 20, nullable = false, columnDefinition = "varchar(20) default 'MANUAL'")
    private String pasarelaActiva = "MANUAL";

    // --- CREDENCIALES MERCADO PAGO ---
    @Column(name = "MP_ACCESS_TOKEN", length = 255)
    private String mpAccessToken; // Nace en null

    // --- CREDENCIALES NIUBIZ ---
    @Column(name = "NIUBIZ_MERCHANT_ID", length = 100)
    private String niubizMerchantId; // Nace en null

    @Column(name = "NIUBIZ_API_USER", length = 255)
    private String niubizApiUser; // Nace en null

    @Column(name = "NIUBIZ_API_PASSWORD", length = 255)
    private String niubizApiPassword; // Nace en null

    public Empresa() {}

    // --- GETTERS Y SETTERS ORIGINALES ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }

    public Integer getLimiteUsuarios() { return limiteUsuarios; }
    public void setLimiteUsuarios(Integer limiteUsuarios) { this.limiteUsuarios = limiteUsuarios; }
    
    public LocalDate getFechaCuotaIa() { return fechaCuotaIa; }
    public void setFechaCuotaIa(LocalDate fechaCuotaIa) { this.fechaCuotaIa = fechaCuotaIa; }

    public Integer getConsultasIaHoy() { return consultasIaHoy; }
    public void setConsultasIaHoy(Integer consultasIaHoy) { this.consultasIaHoy = consultasIaHoy; }

    // --- NUEVOS GETTERS Y SETTERS PARA PAGOS ---

    public String getPasarelaActiva() { return pasarelaActiva; }
    public void setPasarelaActiva(String pasarelaActiva) { this.pasarelaActiva = pasarelaActiva; }

    public String getMpAccessToken() { return mpAccessToken; }
    public void setMpAccessToken(String mpAccessToken) { this.mpAccessToken = mpAccessToken; }

    public String getNiubizMerchantId() { return niubizMerchantId; }
    public void setNiubizMerchantId(String niubizMerchantId) { this.niubizMerchantId = niubizMerchantId; }

    public String getNiubizApiUser() { return niubizApiUser; }
    public void setNiubizApiUser(String niubizApiUser) { this.niubizApiUser = niubizApiUser; }

    public String getNiubizApiPassword() { return niubizApiPassword; }
    public void setNiubizApiPassword(String niubizApiPassword) { this.niubizApiPassword = niubizApiPassword; }
}

// Fin de la definición principal de Empresa.
