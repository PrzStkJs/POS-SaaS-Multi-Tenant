/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Objeto de transferencia de datos utilizado para transportar información de
 *              usuario response entre capas.
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

import com.BarCodeSys.pos.models.Rol;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de UsuarioResponseDTO.

public class UsuarioResponseDTO {
    
    private Long id;
    private String username;
    private String email;
    private Rol rol;
    private Boolean activo;
    private Long empresaId;
    private String empresaNombre; // Opcional, pero muy útil para el frontend

    // Constructor vacío
    public UsuarioResponseDTO() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getEmpresaNombre() {
        return empresaNombre;
    }

    public void setEmpresaNombre(String empresaNombre) {
        this.empresaNombre = empresaNombre;
    }
}

// Fin de la definición principal de UsuarioResponseDTO.
