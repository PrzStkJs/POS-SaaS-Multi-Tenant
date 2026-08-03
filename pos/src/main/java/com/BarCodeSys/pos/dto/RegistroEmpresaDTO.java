/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Administración
 * DESCRIPCIÓN: Objeto de transferencia de datos utilizado para transportar información de
 *              registro empresa entre capas.
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

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de RegistroEmpresaDTO.

public class RegistroEmpresaDTO {

    // Datos de la Empresa
    private String nombreComercial;
    private String emailContacto;
    private Integer limiteUsuarios;
    private String pasarelaActiva;
    private String mpAccessToken;
    private String niubizMerchantId;
    private String niubizApiUser;
    private String niubizApiPassword;

    // Datos del Usuario Inicial
    private String adminUsername;
    private String adminPassword;

    // --- GETTERS Y SETTERS ---
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String nombreComercial) { this.nombreComercial = nombreComercial; }

    public String getEmailContacto() { return emailContacto; }
    public void setEmailContacto(String emailContacto) { this.emailContacto = emailContacto; }

    public Integer getLimiteUsuarios() { return limiteUsuarios; }
    public void setLimiteUsuarios(Integer limiteUsuarios) { this.limiteUsuarios = limiteUsuarios; }

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

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }
}

// Fin de la definición principal de RegistroEmpresaDTO.
