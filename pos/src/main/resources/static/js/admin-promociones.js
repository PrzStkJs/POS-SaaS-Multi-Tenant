/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inteligencia Artificial
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin promociones en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin-promociones.js.

/* ============================================================
   MÓDULO DE GESTIÓN DE PROMOCIONES E IA (VERSIÓN FINAL PRO)
   ============================================================ */

let abortControllerIA = null; 
let promocionesDataGlobal = [];

window.initPromocionesModule = function() {
    console.log("Módulo de Promociones IA Inicializado.");
    cargarPromociones();
    
    configurarBuscadorProducto('codigoProductoBase', 'nombreProductoBase', 'productoBaseId');
    configurarBuscadorProducto('codigoProductoAsociado', 'nombreProductoAsociado', 'productoAsociadoId');
    
    const inputCantidad = document.getElementById('inputCantidadPromos');
    if (inputCantidad) {
        inputCantidad.addEventListener('keydown', function(e) {
            if (['e', 'E', '+', '-', '.'].includes(e.key)) e.preventDefault(); 
        });
    }

    const btnCancelar = document.getElementById('btnCancelarIA');
    if (btnCancelar) {
        btnCancelar.addEventListener('click', function() {
            if (abortControllerIA) {
                abortControllerIA.abort(); 
                console.log("Generación de IA abortada en el frontend.");
                fetch('/api/ia/cancelar-generacion', { method: 'POST' })
                    .then(response => response.json())
                    .then(data => console.log("Servidor notificado:", data.message))
                    .catch(err => console.error("Error al avisar al servidor:", err));
            }
        });
    }

    const filtroSelect = document.getElementById('filtroOrigenPromos');
    if (filtroSelect) {
        filtroSelect.addEventListener('change', function() {
            renderizarTablaPromociones(this.value);
        });
    }

    const btnExcel = document.getElementById('btnExportarExcel');
    if (btnExcel) {
        btnExcel.addEventListener('click', exportarAExcel);
    }
};

/* ============================================================
   SISTEMA DE NOTIFICACIONES PROFESIONALES
   ============================================================ */
function mostrarNotificacionPro(mensaje, tipo) {
    const contenedor = document.getElementById('tablaPromocionesContenedor');
    if (!contenedor) return;

    const alerta = document.createElement('div');
    alerta.className = `alert alert-${tipo} alert-dismissible fade show shadow-sm border-0 mb-3`;
    alerta.innerHTML = `
        <div class="d-flex align-items-center">
            <i class="bi bi-info-circle-fill me-2 fs-5"></i>
            <strong>${mensaje}</strong>
        </div>
        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
    `;
    
    contenedor.parentNode.insertBefore(alerta, contenedor);

    setTimeout(() => {
        if (alerta && alerta.parentNode) {
            alerta.classList.remove('show');
            setTimeout(() => alerta.remove(), 150); 
        }
    }, 6000);
}

/* ============================================================
   CARGAR DATOS DESDE EL BACKEND
   ============================================================ */
function cargarPromociones() {
    const loader = document.getElementById('loaderPromociones');
    const emptyState = document.getElementById('emptyStatePromociones');
    const tablaContenedor = document.getElementById('tablaPromocionesContenedor');

    loader.classList.remove('d-none');
    emptyState.classList.add('d-none');
    tablaContenedor.classList.add('d-none');

    fetch('/api/promociones')
        .then(response => {
            if (!response.ok) throw new Error('Error al cargar promociones');
            return response.json();
        })
        .then(data => {
            promocionesDataGlobal = data;
            
            const filtroSelect = document.getElementById('filtroOrigenPromos');
            if(filtroSelect) filtroSelect.value = 'TODOS';
            
            renderizarTablaPromociones('TODOS');
            loader.classList.add('d-none');

            const fechaTxt = document.getElementById('fechaUltimaConexionIA');
            if (fechaTxt) {
                fechaTxt.innerText = new Date().toLocaleString('es-PE', {
                    day: '2-digit', month: '2-digit', year: 'numeric',
                    hour: '2-digit', minute: '2-digit', hour12: true
                });
            }
        })
        .catch(error => {
            console.error("Error al obtener las promociones:", error);
            loader.classList.add('d-none');
            const tbody = document.getElementById('tbodyPromociones');
            if (tbody) {
                tbody.innerHTML = `
                    <tr>
                        <td colspan="5" class="text-center text-danger py-4">
                            <i class="bi bi-wifi-off me-2 fs-5 d-block mb-2"></i> No se pudo conectar con el servidor.
                        </td>
                    </tr>
                `;
            }
            tablaContenedor.classList.remove('d-none');
        });
}

/* ============================================================
   RENDERIZAR TABLA (APLICANDO FILTROS)
   ============================================================ */
function renderizarTablaPromociones(filtro) {
    const tbody = document.getElementById('tbodyPromociones');
    const emptyState = document.getElementById('emptyStatePromociones');
    const tablaContenedor = document.getElementById('tablaPromocionesContenedor');

    if (!tbody) return;

    const dataFiltrada = promocionesDataGlobal.filter(promo => {
        if (filtro === 'TODOS') return true;
        return promo.origen === filtro;
    });

    if (!dataFiltrada || dataFiltrada.length === 0) {
        tablaContenedor.classList.add('d-none');
        emptyState.classList.remove('d-none');
        return;
    }

    emptyState.classList.add('d-none');
    tablaContenedor.classList.remove('d-none');
    
    let filasHTML = '';
    dataFiltrada.forEach(promo => {
        const nombreBase = promo.productoBase ? promo.productoBase.nombre : 'Desconocido';
        const nombreSugerido = promo.productoSugerido ? promo.productoSugerido.nombre : 'Desconocido';
        const precio = promo.precioPromocional ? parseFloat(promo.precioPromocional).toFixed(2) : '0.00';
        
        const esIA = promo.origen === 'IA';
        const badgeColor = esIA ? 'bg-warning text-dark' : 'bg-info text-dark';
        const badgeIcon = esIA ? 'bi-robot' : 'bi-person-fill';
        const badgeText = esIA ? 'IA Gemini' : 'Manual';
        const badgeHTML = `<span class="badge ${badgeColor}"><i class="bi ${badgeIcon} me-1"></i>${badgeText}</span>`;

        filasHTML += `
            <tr>
                <td class="ps-4"><span class="fw-bold">${nombreBase}</span></td>
                <td>${nombreSugerido}</td>
                <td class="text-center"><span class="badge bg-light text-success fw-bold">+ S/ ${precio}</span></td>
                <td class="text-center">${badgeHTML}</td>
                <td class="text-center pe-4">
                    <button class="btn btn-sm btn-outline-danger shadow-sm" onclick="eliminarPromocion(${promo.id})">
                        <i class="bi bi-trash"></i>
                    </button>
                </td>
            </tr>
        `;
    });

    tbody.innerHTML = filasHTML;
}

/* ============================================================
   EXPORTAR A EXCEL (CSV UTF-8)
   ============================================================ */
function exportarAExcel() {
    const filtroActual = document.getElementById('filtroOrigenPromos') ? document.getElementById('filtroOrigenPromos').value : 'TODOS';
    
    const dataFiltrada = promocionesDataGlobal.filter(promo => {
        if (filtroActual === 'TODOS') return true;
        return promo.origen === filtroActual;
    });

    if (dataFiltrada.length === 0) {
        mostrarNotificacionPro("No hay datos para exportar con el filtro actual.", "warning");
        return;
    }

    let csvContent = "Producto Base,Producto Sugerido,Precio Promocional,Origen\n";

    dataFiltrada.forEach(promo => {
        const nombreBase = promo.productoBase ? `"${promo.productoBase.nombre}"` : '"Desconocido"';
        const nombreSugerido = promo.productoSugerido ? `"${promo.productoSugerido.nombre}"` : '"Desconocido"';
        const precio = promo.precioPromocional ? promo.precioPromocional : '0.00';
        const origen = promo.origen === 'IA' ? 'IA Gemini' : 'Manual';
        
        csvContent += `${nombreBase},${nombreSugerido},${precio},${origen}\n`;
    });

    const blob = new Blob(["\uFEFF" + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", `Promociones_Caja_${filtroActual}.csv`); 
    
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
    
    mostrarNotificacionPro("Reporte en Excel descargado correctamente.", "success");
}

/* ============================================================
   BOTÓN: SUGERIR PROMOCIONES MEDIANTE IA (CON TOPE 500)
   ============================================================ */
window.sugerirPromocionesConIA = function() {
    const inputCantidad = document.getElementById('inputCantidadPromos');
    const cantidad = inputCantidad ? parseInt(inputCantidad.value) : 3;
    const btnEjecutar = document.getElementById('btnEjecutarIA');
    const originalHTML = btnEjecutar.innerHTML;

    const TOPE_MAXIMO = 500; 

    if (isNaN(cantidad) || cantidad < 1 || cantidad > TOPE_MAXIMO) {
        mostrarNotificacionPro(`Por favor, ingresa una cantidad válida (entre 1 y ${TOPE_MAXIMO}).`, "warning");
        if (cantidad > TOPE_MAXIMO && inputCantidad) {
            inputCantidad.value = TOPE_MAXIMO;
        }
        return;
    }

    btnEjecutar.disabled = true;
    btnEjecutar.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status"></span> Procesando IA...`;

    abortControllerIA = new AbortController();
    const signal = abortControllerIA.signal;

    fetch(`/api/ia/generar-ofertas?cantidad=${cantidad}`, {
        method: 'POST',
        signal: signal 
    })
    .then(response => {
        if (!response.ok) throw new Error('Error al ejecutar modelo de IA');
        return response.json();
    })
    .then(data => {
        btnEjecutar.disabled = false;
        btnEjecutar.innerHTML = originalHTML;
        
        const modalEl = document.getElementById('modalConfigurarIA');
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) modalInstance.hide();

        if (data.status === "success") {
            mostrarNotificacionPro(`Análisis Exitoso: Solicitaste ${cantidad} ofertas. ${data.message}`, "success");
        } else {
            mostrarNotificacionPro(`Aviso de IA: ${data.message}`, "warning");
        }
        
        cargarPromociones();
    })
    .catch(error => {
        btnEjecutar.disabled = false;
        btnEjecutar.innerHTML = originalHTML;

        if (error.name === 'AbortError') {
            mostrarNotificacionPro("El análisis de IA fue cancelado de forma segura.", "info");
        } else {
            console.error("Error al generar ofertas con IA:", error);
            mostrarNotificacionPro("No se pudieron generar ofertas. Verifica la consola del servidor.", "danger");
        }
    });
};

/* ============================================================
   GUARDAR NUEVA PROMOCIÓN MANUAL
   ============================================================ */
window.guardarPromoManual = function() {
    const baseId = document.getElementById('productoBaseId').value;
    const asociadoId = document.getElementById('productoAsociadoId').value;
    const precioPromo = document.getElementById('inputPrecioPromo').value;

    if (!baseId || !asociadoId) {
        mostrarNotificacionPro("Faltan productos: Escanea los códigos primero.", "warning");
        return;
    }

    const dataNuevaPromo = {
        productoBase: { id: parseInt(baseId) },
        productoSugerido: { id: parseInt(asociadoId) },
        precioPromocional: parseFloat(precioPromo),
        scriptVenta: "¡Lleve también este producto a un precio especial!"
    };

    fetch('/api/promociones', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(dataNuevaPromo)
    })
    .then(response => {
        if (!response.ok) throw new Error('Error al guardar promoción');
        return response.json();
    })
    .then(() => {
        const modalEl = document.getElementById('modalNuevaPromo');
        const modalInstance = bootstrap.Modal.getInstance(modalEl);
        if (modalInstance) modalInstance.hide();

        document.getElementById('formNuevaPromo').reset();
        document.getElementById('nombreProductoBase').innerText = 'Ninguno';
        document.getElementById('nombreProductoBase').className = 'text-muted fw-normal';
        document.getElementById('nombreProductoAsociado').innerText = 'Ninguno';
        document.getElementById('nombreProductoAsociado').className = 'text-muted fw-normal';
        document.getElementById('productoBaseId').value = '';
        document.getElementById('productoAsociadoId').value = '';

        mostrarNotificacionPro("¡Promoción manual guardada exitosamente!", "success");
        cargarPromociones();
    })
    .catch(error => {
        console.error("Error al guardar promoción:", error);
        mostrarNotificacionPro("Error de servidor al guardar la promoción.", "danger");
    });
};

/* ============================================================
   ELIMINAR PROMOCIÓN
   ============================================================ */
window.eliminarPromocion = function(id) {
    if (!confirm("¿Seguro que deseas eliminar esta sugerencia de promoción?")) return;

    fetch(`/api/promociones/${id}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (!response.ok) throw new Error('Error al eliminar');
        mostrarNotificacionPro("Promoción eliminada correctamente.", "info");
        cargarPromociones();
    })
    .catch(error => {
        console.error("Error al eliminar promoción:", error);
        mostrarNotificacionPro("No se pudo eliminar el registro.", "danger");
    });
};

/* ============================================================
   BUSCADOR POR CÓDIGO DE BARRAS
   ============================================================ */
function configurarBuscadorProducto(inputId, nombreSpanId, hiddenId) {
    const inputElement = document.getElementById(inputId);
    if (!inputElement) return;

    inputElement.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            const codigo = this.value.trim();
            if (codigo === '') return;

            const spanNombre = document.getElementById(nombreSpanId);
            spanNombre.innerHTML = '<span class="spinner-border spinner-border-sm text-success" role="status"></span> Buscando...';

            fetch(`/api/ventas/buscar-producto?codigo=${encodeURIComponent(codigo)}`)
                .then(response => {
                    if (!response.ok) throw new Error('Producto no encontrado');
                    return response.json();
                })
                .then(producto => {
                    spanNombre.innerText = producto.nombre;
                    spanNombre.classList.remove('text-danger', 'text-muted');
                    spanNombre.classList.add('text-success', 'fw-bold');
                    document.getElementById(hiddenId).value = producto.id;
                })
                .catch(error => {
                    spanNombre.innerText = 'No encontrado';
                    spanNombre.classList.remove('text-success', 'text-muted');
                    spanNombre.classList.add('text-danger', 'fw-bold');
                    document.getElementById(hiddenId).value = '';
                });
        }
    });
}

// Fin de la lógica principal de admin-promociones.js.
