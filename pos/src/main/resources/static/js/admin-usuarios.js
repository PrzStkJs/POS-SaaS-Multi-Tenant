/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Seguridad y Usuarios
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin usuarios en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin-usuarios.js.

/* ------------------------------------------------------------------ */
/* Módulo de Gestión de Usuarios/Cajeros                              */
/* ------------------------------------------------------------------ */

window.initUsuariosModule = function() {
    console.log("Módulo de Usuarios Inicializado.");
    cargarUsuarios();
    configurarFormularioUsuario();
    configurarFormularioReset(); // Inicializamos el nuevo formulario
};

/**
 * Petición GET para listar los usuarios en la tabla.
 */
function cargarUsuarios() {
    const tbody = document.querySelector('#tablaUsuarios tbody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4"><div class="spinner-border text-success" role="status"></div></td></tr>';

    fetch('/api/usuarios')
        .then(response => {
            if (!response.ok) throw new Error('Error al obtener la lista de usuarios');
            return response.json();
        })
        .then(usuarios => {
            tbody.innerHTML = '';
            if (usuarios.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No hay usuarios registrados.</td></tr>';
                return;
            }

            usuarios.forEach(u => {
                const tr = document.createElement('tr');
                
                // 1. Diseño del badge de Estado basado en el boolean "activo"
                let badgeEstado = u.activo 
                    ? '<span class="badge bg-success bg-opacity-10 text-success border border-success">Activo</span>' 
                    : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger">Inactivo</span>';

                // 2. Lógica Anti-Suicidio + Botones dinámicos
                let botones = '';
                if (u.rol !== 'ADMIN') {
                    // Botón para resetear contraseña
                    let btnReset = `<button class="btn btn-sm btn-outline-warning me-1" onclick="abrirModalReset(${u.id}, '${u.username}')" title="Restablecer Contraseña"><i class="bi bi-key-fill"></i></button>`;
                    
                    // Botón para Activar/Desactivar (Borrado Lógico)
                    let btnEstado = u.activo 
                        ? `<button class="btn btn-sm btn-outline-danger" onclick="cambiarEstadoUsuario(${u.id}, 'desactivar')" title="Dar de baja"><i class="bi bi-person-x-fill"></i></button>`
                        : `<button class="btn btn-sm btn-outline-success" onclick="cambiarEstadoUsuario(${u.id}, 'activar')" title="Reactivar cajero"><i class="bi bi-person-check-fill"></i></button>`;
                    
                    botones = btnReset + btnEstado;
                }

                tr.innerHTML = `
                    <td class="text-muted fw-bold">#${u.id}</td>
                    <td class="fw-bold"><i class="bi bi-person-fill text-muted me-2"></i>${u.username}</td>
                    <td>${u.email ? u.email : '<span class="text-muted">Sin correo</span>'}</td>
                    <td><span class="badge ${u.rol === 'ADMIN' ? 'bg-danger' : 'bg-primary'}">${u.rol}</span></td>
                    <td>${badgeEstado}</td>
                    <td class="text-end">${botones}</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => {
            console.error("Error:", error);
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4"><i class="bi bi-exclamation-triangle me-2"></i>Error al cargar los datos.</td></tr>`;
        });
}

/**
 * Configura el envío del modal para crear un usuario nuevo.
 */
function configurarFormularioUsuario() {
    const form = document.getElementById('formNuevoUsuario');
    if (!form) return;

    form.addEventListener('submit', function(e) {
        e.preventDefault();

        const userVal = document.getElementById('username').value.trim();
        const passVal = document.getElementById('password').value.trim();

        if (userVal.length < 5) {
            Swal.fire({ icon: 'warning', title: 'Atención', text: 'El usuario debe tener al menos 5 caracteres.' });
            return; 
        }

        if (passVal.length < 5) {
            Swal.fire({ icon: 'warning', title: 'Atención', text: 'La contraseña debe tener al menos 5 caracteres.' });
            return; 
        }

        Swal.fire({
            title: '¿Registrar nuevo cajero?',
            text: `Se creará el acceso para el usuario "${userVal}".`,
            icon: 'question',
            showCancelButton: true,
            confirmButtonColor: '#198754', // Success
            cancelButtonColor: '#6c757d',  // Secondary
            confirmButtonText: 'Sí, registrar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                const data = {
                    username: userVal,
                    email: document.getElementById('emailUsuario').value.trim() || null,
                    password: passVal,
                    rol: document.getElementById('rolUsuario').value
                };

                // Loader opcional
                Swal.fire({ title: 'Procesando...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

                fetch('/api/usuarios', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                })
                .then(async response => {
                    if (!response.ok) {
                        const errorText = await response.text(); 
                        throw new Error(errorText);
                    }
                    Swal.fire('¡Registrado!', 'El cajero ha sido creado con éxito.', 'success');
                    form.reset();
                    bootstrap.Modal.getInstance(document.getElementById('modalNuevoUsuario')).hide();
                    cargarUsuarios();
                })
                .catch(error => {
                    console.error("Error:", error);
                    if (error.message.includes("LÍMITE")) {
                        Swal.fire('Límite Alcanzado', error.message, 'warning');
                    } else {
                        Swal.fire('Error', 'Hubo un problema al registrar el cajero. Verifica la consola.', 'error');
                    }
                });
            }
        });
    });
}

/* ========================================================== */
/* FUNCIONES CORPORATIVAS                                    */
/* ========================================================== */

/**
 * Función para abrir el modal de Reset Password con los datos del cajero
 */
window.abrirModalReset = function(id, username) {
    document.getElementById('resetUsuarioId').value = id;
    document.getElementById('resetUsername').value = username;
    document.getElementById('nuevaPassword').value = '';
    
    const modal = new bootstrap.Modal(document.getElementById('modalResetPassword'));
    modal.show();
};

/**
 * Configura el envío de la nueva contraseña
 */
function configurarFormularioReset() {
    const form = document.getElementById('formResetPassword');
    if (!form) return;

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        const id = document.getElementById('resetUsuarioId').value;
        const nuevaClave = document.getElementById('nuevaPassword').value.trim();

        if (nuevaClave.length < 5) {
            Swal.fire({ icon: 'warning', title: 'Atención', text: 'La nueva contraseña debe tener al menos 5 caracteres.' });
            return; 
        }

        Swal.fire({ title: 'Actualizando...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

        fetch(`/api/usuarios/${id}/reset-password`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ password: nuevaClave })
        })
        .then(response => {
            if (!response.ok) throw new Error('Error al actualizar');
            Swal.fire('¡Éxito!', 'La contraseña ha sido actualizada.', 'success');
            form.reset();
            bootstrap.Modal.getInstance(document.getElementById('modalResetPassword')).hide();
        })
        .catch(error => {
            Swal.fire('Error', 'No se pudo restablecer la contraseña.', 'error');
        });
    });
}

/**
 * Función global para Inactivar / Reactivar usuario (Borrado Lógico)
 */
window.cambiarEstadoUsuario = function(id, accion) {
    const esDesactivar = accion === 'desactivar';
    
    Swal.fire({
        title: esDesactivar ? '¿Dar de baja al cajero?' : '¿Reactivar cajero?',
        text: esDesactivar 
            ? "Ya no podrá iniciar sesión en el sistema." 
            : "Volverá a tener acceso al sistema.",
        icon: esDesactivar ? 'warning' : 'info',
        showCancelButton: true,
        confirmButtonColor: esDesactivar ? '#dc3545' : '#198754',
        cancelButtonColor: '#6c757d',
        confirmButtonText: esDesactivar ? 'Sí, dar de baja' : 'Sí, reactivar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
    }).then((result) => {
        if (result.isConfirmed) {
            Swal.fire({ title: 'Procesando...', allowOutsideClick: false, didOpen: () => Swal.showLoading() });

            fetch(`/api/usuarios/${id}/cambiar-estado`, { method: 'PUT' })
                .then(response => {
                    if(response.ok) {
                        Swal.fire('¡Actualizado!', 'El estado del usuario ha cambiado.', 'success');
                        cargarUsuarios(); 
                    } else {
                        throw new Error('Error en el servidor');
                    }
                })
                .catch(error => {
                    console.error("Error:", error);
                    Swal.fire('Error', 'No se pudo cambiar el estado del usuario.', 'error');
                });
        }
    });
};

// Fin de la lógica principal de admin-usuarios.js.
