/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Componente de seguridad encargado de autenticación, autorización o contexto
 *              del usuario activo.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import java.util.Collection;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de UsuarioPrincipal.

public class UsuarioPrincipal extends User {
    
    private final Long idUsuario;
    private final Long idEmpresa;

    public UsuarioPrincipal(Long idUsuario, String username, String password, boolean activo, Long idEmpresa, Collection<? extends GrantedAuthority> authorities) {
        
        super(username, password, activo, true, true, true, authorities);
        
        this.idUsuario = idUsuario;
        this.idEmpresa = idEmpresa;
    }

    public Long getIdUsuario() { return idUsuario; }
    public Long getIdEmpresa() { return idEmpresa; }
}

// Fin de la definición principal de UsuarioPrincipal.
