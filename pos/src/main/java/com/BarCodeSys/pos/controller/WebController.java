/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Punto de Venta
 * DESCRIPCIÓN: Controlador encargado de recibir solicitudes HTTP y coordinar las respuestas
 *              de web dentro del sistema POS.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de WebController.

@Controller
public class WebController {

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login"; 
    }
    
    @GetMapping("/admin")
    public String mostrarAdmin() {
        return "admin"; 
    }
    
    @GetMapping("/superadmin")
    public String mostrarSuperAdmin() {
        return "super-admin";
    }
    
    @GetMapping("/ventas")
    public String mostrarVentas() {
        return "ventas"; 
    }
    
    @GetMapping("/admin/vista/{nombreVista}")
    public String obtenerFragmentoAdmin(@PathVariable String nombreVista) {
        return "fragmentos/" + nombreVista; 
    }
}

// Fin de la definición principal de WebController.
