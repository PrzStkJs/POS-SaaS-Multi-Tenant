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

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de SecurityUtils.

public class SecurityUtils {

    public static Long getCurrentEmpresaId() {
        UsuarioPrincipal usuario = getUsuarioPrincipal();
        return usuario.getIdEmpresa();
    }

    public static Long getCurrentUsuarioId() {
        UsuarioPrincipal usuario = getUsuarioPrincipal();
        return usuario.getIdUsuario();
    }
    public static boolean isSuperAdmin() {
        UsuarioPrincipal usuario = getUsuarioPrincipal();
        return usuario.getAuthorities().stream()
                .anyMatch(rol -> rol.getAuthority().equals("ROLE_SUPER_ADMIN"));
    }

    private static UsuarioPrincipal getUsuarioPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof UsuarioPrincipal) {
            return (UsuarioPrincipal) authentication.getPrincipal();
        }

        throw new IllegalStateException("No hay un usuario autenticado en el contexto de seguridad.");
    }
}

// Fin de la definición principal de SecurityUtils.
