/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Administración
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de super admin en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de super-admin.js.

/* ------------------------------------------------------------------ */
/* Panel Super Admin - Gestión de Empresas y Usuarios                 */
/* ------------------------------------------------------------------ */

// ==========================================
// SEGURIDAD: Leer Token CSRF
// ==========================================
let empresasCache = [];
let usuariosCache = [];
let empresaActualId = null; // Para saber de qué empresa estamos viendo los usuarios

document.addEventListener('DOMContentLoaded', () => {
    cargarEmpresas();
    configurarFormularioEmpresa();
    configurarEventosVistas();
    configurarFormularioUsuario();
});

// ==========================================
// SECCIÓN 1: EMPRESAS
// ==========================================
function cargarEmpresas() {
    const tbody = document.querySelector('#tablaEmpresas tbody');
    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4"><div class="spinner-border text-success" role="status"></div></td></tr>';

    fetch('/api/empresas')
        .then(response => {
            if (!response.ok) throw new Error('No se pudo obtener la lista de empresas');
            return response.json();
        })
        .then(empresas => {
            empresasCache = empresas || [];
            renderizarEmpresas(tbody, empresasCache);
        })
        .catch(error => {
            console.error('Error:', error);
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger py-4"><i class="bi bi-exclamation-triangle me-2"></i>Error al cargar empresas.</td></tr>';
        });
}

function renderizarEmpresas(tbody, empresas) {
    tbody.innerHTML = '';

    if (!empresas.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No hay empresas registradas.</td></tr>';
        return;
    }

    empresas.forEach(empresa => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="text-muted fw-bold">#${empresa.id}</td>
            <td class="fw-bold">${escapeHtml(empresa.nombreComercial || '')}</td>
            <td>${empresa.emailContacto ? escapeHtml(empresa.emailContacto) : '<span class="text-muted">Sin correo</span>'}</td>
            <td><span class="badge bg-light text-dark border">${empresa.limiteUsuarios || 0}</span></td>
            <td>${badgePasarela(empresa.pasarelaActiva)}</td>
            <td>${badgeCredenciales(empresa)}</td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-primary btn-usuarios-empresa me-1" data-id="${empresa.id}" data-nombre="${escapeHtml(empresa.nombreComercial)}" title="Gestionar Usuarios">
                    <i class="bi bi-people"></i> Usuarios
                </button>
                <button class="btn btn-sm btn-outline-green btn-editar-empresa" data-id="${empresa.id}" title="Editar empresa">
                    <i class="bi bi-pencil-square"></i>
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll('.btn-editar-empresa').forEach(btn => {
        btn.addEventListener('click', () => abrirModalEmpresa(btn.dataset.id));
    });

    tbody.querySelectorAll('.btn-usuarios-empresa').forEach(btn => {
        btn.addEventListener('click', () => {
            cambiarVistaUsuarios(btn.dataset.id, btn.dataset.nombre);
        });
    });
}

function configurarFormularioEmpresa() {
    const btnNueva = document.getElementById('btnNuevaEmpresa');
    const form = document.getElementById('formEmpresa');
    const selectPasarela = document.getElementById('empresaPasarela');

    if (btnNueva) btnNueva.addEventListener('click', () => abrirModalEmpresa());
    if (selectPasarela) selectPasarela.addEventListener('change', actualizarPanelesCredenciales);

    if (!form) return;

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        const id = document.getElementById('empresaId').value;
        const data = obtenerPayloadEmpresa();

        if (!data.nombreComercial) {
            alert('Ingresa el nombre comercial de la empresa.');
            return;
        }

        const url = id ? `/api/empresas/${id}` : '/api/empresas';
        const method = id ? 'PUT' : 'POST';

        fetch(url, {
            method,
            headers: { 
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': window.obtenerCsrfToken()
            },
            body: JSON.stringify(data)
        })
        .then(async response => {
            if (!response.ok) throw new Error('Error al guardar la empresa');
            bootstrap.Modal.getInstance(document.getElementById('modalEmpresa')).hide();
            cargarEmpresas();
        })
        .catch(error => alert('No se pudo guardar la empresa.'));
    });
}

function abrirModalEmpresa(id = null) {
    const form = document.getElementById('formEmpresa');
    const titulo = document.getElementById('tituloModalEmpresa');
    const seccionAdmin = document.getElementById('seccionAdminInicial');
    if (!form) return;

    form.reset();
    document.getElementById('empresaId').value = '';
    document.getElementById('empresaLimite').value = 3;
    document.getElementById('empresaPasarela').value = 'MANUAL';

    if (id) {
        // MODO EDICIÓN
        const empresa = empresasCache.find(item => String(item.id) === String(id));
        if (empresa) {
            titulo.innerHTML = '<i class="bi bi-building-gear me-2"></i>Editar Empresa';
            document.getElementById('empresaId').value = empresa.id;
            document.getElementById('empresaNombre').value = empresa.nombreComercial || '';
            document.getElementById('empresaEmail').value = empresa.emailContacto || '';
            document.getElementById('empresaLimite').value = empresa.limiteUsuarios || 3;
            document.getElementById('empresaPasarela').value = empresa.pasarelaActiva || 'MANUAL';
            document.getElementById('empresaMpToken').value = empresa.mpAccessToken || '';
            document.getElementById('empresaNiubizMerchant').value = empresa.niubizMerchantId || '';
            document.getElementById('empresaNiubizUser').value = empresa.niubizApiUser || '';
            document.getElementById('empresaNiubizPassword').value = empresa.niubizApiPassword || '';
            
            // Ocultamos la sección de admin (empresa ya existe)
            seccionAdmin.classList.add('d-none');
            document.getElementById('empresaAdminUser').required = false;
            document.getElementById('empresaAdminPass').required = false;
        }
    } else {
        // MODO CREACIÓN
        titulo.innerHTML = '<i class="bi bi-building-add me-2"></i>Nueva Empresa';
        
        // Mostramos la sección de admin y la hacemos obligatoria
        seccionAdmin.classList.remove('d-none');
        document.getElementById('empresaAdminUser').required = true;
        document.getElementById('empresaAdminPass').required = true;
    }

    actualizarPanelesCredenciales();
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEmpresa')).show();
}

function obtenerPayloadEmpresa() {
    return {
        nombreComercial: document.getElementById('empresaNombre').value.trim(),
        emailContacto: nullSiVacio(document.getElementById('empresaEmail').value),
        limiteUsuarios: parseInt(document.getElementById('empresaLimite').value, 10) || 3,
        pasarelaActiva: document.getElementById('empresaPasarela').value,
        mpAccessToken: nullSiVacio(document.getElementById('empresaMpToken').value),
        niubizMerchantId: nullSiVacio(document.getElementById('empresaNiubizMerchant').value),
        niubizApiUser: nullSiVacio(document.getElementById('empresaNiubizUser').value),
        niubizApiPassword: nullSiVacio(document.getElementById('empresaNiubizPassword').value),
        // NUEVOS CAMPOS: solo se envían llenos al crear empresa
        adminUsername: document.getElementById('empresaAdminUser').value.trim(),
        adminPassword: document.getElementById('empresaAdminPass').value
    };
}

// ==========================================
// SECCIÓN 2: CONTROL DE VISTAS (SPA)
// ==========================================
function configurarEventosVistas() {
    const btnVolver = document.getElementById('btnVolverEmpresas');
    if (btnVolver) {
        btnVolver.addEventListener('click', () => {
            document.getElementById('vistaUsuarios').classList.add('d-none');
            document.getElementById('vistaEmpresas').classList.remove('d-none');
            empresaActualId = null;
        });
    }
}

function cambiarVistaUsuarios(idEmpresa, nombreEmpresa) {
    empresaActualId = idEmpresa;
    
    document.getElementById('tituloVistaUsuarios').innerHTML = `<i class="bi bi-people me-2"></i>Usuarios: ${nombreEmpresa}`;
    
    document.getElementById('vistaEmpresas').classList.add('d-none');
    document.getElementById('vistaUsuarios').classList.remove('d-none');

    cargarUsuarios(idEmpresa);
}

// ==========================================
// SECCIÓN 3: USUARIOS
// ==========================================
function cargarUsuarios(idEmpresa) {
    const tbody = document.querySelector('#tablaUsuarios tbody');
    tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4"><div class="spinner-border text-primary" role="status"></div></td></tr>';

    fetch(`/api/usuarios/empresa/${idEmpresa}`)
        .then(response => response.json())
        .then(usuarios => {
            usuariosCache = usuarios || [];
            renderizarUsuarios(tbody, usuariosCache);
        })
        .catch(error => {
            console.error('Error:', error);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger py-4">Error al cargar usuarios.</td></tr>';
        });
}

function renderizarUsuarios(tbody, usuarios) {
    tbody.innerHTML = '';

    if (!usuarios.length) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">Esta empresa no tiene usuarios registrados.</td></tr>';
        return;
    }

    usuarios.forEach(usuario => {
        const estadoBadge = usuario.activo 
            ? '<span class="badge bg-success bg-opacity-10 text-success border border-success">Activo</span>'
            : '<span class="badge bg-danger bg-opacity-10 text-danger border border-danger">Inactivo</span>';

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="text-muted fw-bold">#${usuario.id}</td>
            <td class="fw-bold">${escapeHtml(usuario.username)}</td>
            <td>${escapeHtml(usuario.email || 'Sin correo')}</td>
            <td><span class="badge bg-light text-dark border">${usuario.rol}</span></td>
            <td>${estadoBadge}</td>
            <td class="text-end">
                <button class="btn btn-sm btn-outline-primary btn-editar-usuario" data-id="${usuario.id}" title="Editar usuario">
                    <i class="bi bi-pencil"></i> Editar
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });

    tbody.querySelectorAll('.btn-editar-usuario').forEach(btn => {
        btn.addEventListener('click', () => abrirModalUsuario(btn.dataset.id));
    });
}

function configurarFormularioUsuario() {
    const btnNuevo = document.getElementById('btnNuevoUsuario');
    const form = document.getElementById('formUsuario');

    if (btnNuevo) btnNuevo.addEventListener('click', () => abrirModalUsuario());

    if (!form) return;

    form.addEventListener('submit', function(e) {
        e.preventDefault();
        const id = document.getElementById('usuarioId').value;
        
        const payload = {
            username: document.getElementById('usuarioUsername').value.trim(),
            email: document.getElementById('usuarioEmail').value.trim(),
            rol: document.getElementById('usuarioRol').value,
            activo: document.getElementById('usuarioActivo').checked,
            empresa: { id: empresaActualId }
        };

        const password = document.getElementById('usuarioPassword').value;
        if (password) payload.password = password;

        const url = id ? `/api/usuarios/${id}` : '/api/usuarios';
        const method = id ? 'PUT' : 'POST';

        fetch(url, {
            method,
            headers: { 
                'Content-Type': 'application/json',
                'X-XSRF-TOKEN': window.obtenerCsrfToken()
            },
            body: JSON.stringify(payload)
        })
        .then(async response => {
            if (!response.ok) throw new Error('Error guardando usuario');
            bootstrap.Modal.getInstance(document.getElementById('modalUsuario')).hide();
            cargarUsuarios(empresaActualId);
        })
        .catch(error => alert('No se pudo guardar el usuario.'));
    });
}

function abrirModalUsuario(id = null) {
    const form = document.getElementById('formUsuario');
    const titulo = document.getElementById('tituloModalUsuario');
    
    form.reset();
    document.getElementById('usuarioId').value = '';
    document.getElementById('usuarioActivo').checked = true;
    document.getElementById('switchActivoContainer').classList.add('d-none');
    document.getElementById('usuarioPassword').required = true;

    if (id) {
        const usuario = usuariosCache.find(item => String(item.id) === String(id));
        if (usuario) {
            titulo.innerHTML = '<i class="bi bi-person-gear me-2"></i>Editar Usuario';
            document.getElementById('usuarioId').value = usuario.id;
            document.getElementById('usuarioUsername').value = usuario.username || '';
            document.getElementById('usuarioEmail').value = usuario.email || '';
            document.getElementById('usuarioRol').value = usuario.rol || 'CAJERO';
            document.getElementById('usuarioActivo').checked = usuario.activo;
            
            document.getElementById('usuarioPassword').required = false;
            document.getElementById('switchActivoContainer').classList.remove('d-none');
        }
    } else {
        titulo.innerHTML = '<i class="bi bi-person-plus me-2"></i>Nuevo Usuario';
    }

    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalUsuario')).show();
}

// ==========================================
// UTILIDADES
// ==========================================
function actualizarPanelesCredenciales() {
    const pasarela = document.getElementById('empresaPasarela').value;
    const panelMercadoPago = document.getElementById('panelMercadoPago');
    const panelNiubiz = document.getElementById('panelNiubiz');

    if (panelMercadoPago) panelMercadoPago.classList.toggle('d-none', pasarela !== 'MERCADOPAGO');
    if (panelNiubiz) panelNiubiz.classList.toggle('d-none', pasarela !== 'NIUBIZ');
}

function badgePasarela(pasarela) {
    const valor = pasarela || 'MANUAL';
    const clases = { MANUAL: 'bg-secondary', NIUBIZ: 'bg-primary', MERCADOPAGO: 'bg-info text-dark' };
    return `<span class="badge ${clases[valor] || 'bg-secondary'}">${valor}</span>`;
}

function badgeCredenciales(empresa) {
    const pasarela = empresa.pasarelaActiva || 'MANUAL';
    if (pasarela === 'NIUBIZ') {
        const ok = tieneTexto(empresa.niubizMerchantId) && tieneTexto(empresa.niubizApiUser) && tieneTexto(empresa.niubizApiPassword);
        return ok ? '<span class="badge bg-success bg-opacity-10 text-success border border-success">Niubiz completo</span>' : '<span class="badge bg-warning bg-opacity-10 text-warning border border-warning">Niubiz incompleto</span>';
    }
    if (pasarela === 'MERCADOPAGO') {
        return tieneTexto(empresa.mpAccessToken) ? '<span class="badge bg-success bg-opacity-10 text-success border border-success">MP completo</span>' : '<span class="badge bg-warning bg-opacity-10 text-warning border border-warning">MP incompleto</span>';
    }
    return '<span class="badge bg-light text-dark border">Manual</span>';
}

function tieneTexto(valor) { return valor !== null && valor !== undefined && String(valor).trim() !== ''; }
function nullSiVacio(valor) { const limpio = (valor || '').trim(); return limpio === '' ? null : limpio; }
function escapeHtml(valor) {
    return String(valor).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#039;');
}

// Fin de la lógica principal de super-admin.js.
