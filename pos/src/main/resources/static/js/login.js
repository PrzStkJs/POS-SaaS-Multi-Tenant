/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de login en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de login.js.

/* ===========================================
   SECCIÓN 1: Selección de elementos del DOM
   =========================================== */
document.getElementById('loginForm').addEventListener('submit', function(event) {
    // ===========================================
    // SECCIÓN 2: Obtener valores de los campos
    // ===========================================
    const usuario = document.getElementById('username').value.trim();
    const clave = document.getElementById('password').value.trim();
    const mensajeError = document.getElementById('mensajeError');

    // ===========================================
    // SECCIÓN 3: Validación antes de enviar
    // ===========================================
    if (usuario === "" || clave === "") {
        event.preventDefault();
        mensajeError.classList.remove('d-none');
        mensajeError.innerText = "Por favor completa todos los campos";
    } else {
        // ===========================================
        // SECCIÓN 4: Envío permitido
        // ===========================================
        mensajeError.classList.add('d-none');
    }
});

// Fin de la lógica principal de login.js.
