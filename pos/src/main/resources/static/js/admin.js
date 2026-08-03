/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Administración
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin.js.

// ==========================================
// SEGURIDAD GLOBAL: Función para obtener el Token CSRF de Spring Boot
// ==========================================

// Obtiene el token CSRF desde la cookie XSRF-TOKEN (configuración por defecto de Spring Security)
function getCsrfToken() {
    const cookies = document.cookie.split(';');
    for (let cookie of cookies) {
        const [name, value] = cookie.trim().split('=');
        if (name === 'XSRF-TOKEN') {
            return decodeURIComponent(value);
        }
    }
    return '';
}

// ==========================================
// CONTROLADOR DE VISTAS (SPA)
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const contenedorVistas = document.getElementById('contenedor-vistas');
    
    // Se seleccionan TODOS los enlaces del sidebar, incluido el de Salir
    const navLinks = document.querySelectorAll('.sidebar-link');

    function cargarVista(vistaId) {
        // Mostrar spinner de carga estético
        contenedorVistas.innerHTML = `
            <div class="d-flex justify-content-center align-items-center" style="height: 60vh;">
                <div class="spinner-border text-success" role="status"></div>
                <span class="ms-3 text-muted fw-bold">Cargando módulo...</span>
            </div>`;

        // Petición a tu WebController de Spring Boot
        fetch(`/admin/vista/${vistaId}`)
            .then(response => {
                if (!response.ok) throw new Error('No se pudo cargar la vista');
                return response.text();
            })
            .then(html => {
                // Inyectar el HTML en el DOM maestro (Seguro: Viene del servidor, no del usuario)
                contenedorVistas.innerHTML = html;

                // Invocar dinámicamente el inicializador del módulo correspondiente si existe
                switch (vistaId) {
                    case 'dashboard':
                        if (typeof window.initDashboardModule === 'function') window.initDashboardModule();
                        break;
                    case 'productos':
                        if (typeof window.initProductosModule === 'function') window.initProductosModule();
                        break;
                    case 'stock':
                        if (typeof window.initStockModule === 'function') window.initStockModule();
                        break;
                    case 'ventas':
                        if (typeof window.initVentasModule === 'function') window.initVentasModule();
                        break;
                    case 'analitica':
                        if (typeof window.initAnaliticaModule === 'function') window.initAnaliticaModule();
                        break;
                    case 'promociones':
                        if (typeof window.initPromocionesModule === 'function') window.initPromocionesModule();
                        break;
                    case 'usuarios':
                        if (typeof window.initUsuariosModule === 'function') window.initUsuariosModule();
                        break;
                }
            })
            .catch(error => {
                console.error(error);
                contenedorVistas.innerHTML = `
                    <div class="alert alert-danger m-4">
                        <i class="bi bi-exclamation-triangle-fill me-2"></i> Error al cargar el módulo: ${error.message}
                    </div>`;
            });
    }

    // Escuchador del menú lateral (ahora incluye el enlace de Salir)
    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            // ---- Manejo especial para el botón Salir ----
            if (link.getAttribute('href') === '/logout') {
                e.preventDefault(); // evita el GET por defecto
                fetch('/logout', {
                    method: 'POST',
                    headers: {
                        'X-CSRF-TOKEN': getCsrfToken()
                    }
                })
                .then(() => {
                    window.location.href = '/login?logout';
                })
                .catch(err => {
                    console.error('Error al cerrar sesión:', err);
                    // Fallback por si ocurre un error inesperado
                    window.location.href = '/login?logout';
                });
                return;
            }

            // Si no tiene data-vista no es un módulo navegable, ignorar
            const vistaId = link.getAttribute('data-vista');
            if (!vistaId) return;

            e.preventDefault();

            // Marcar enlace activo
            navLinks.forEach(l => l.classList.remove('active'));
            link.classList.add('active');

            cargarVista(vistaId);

            // Cerrar menú móvil en pantallas pequeñas
            const offcanvasEl = document.getElementById('sidebarMenu');
            if (offcanvasEl && window.innerWidth < 992) {
                const offcanvas = bootstrap.Offcanvas.getInstance(offcanvasEl);
                if (offcanvas) offcanvas.hide();
            }
        });
    });

    // Carga inicial por defecto
    cargarVista('dashboard');
});

// Fin de la lógica principal de admin.js.
