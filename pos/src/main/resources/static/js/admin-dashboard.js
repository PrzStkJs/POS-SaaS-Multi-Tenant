/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Analítica y Dashboard
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin dashboard en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin-dashboard.js.

// =======================================================
// VARIABLES GLOBALES DEL GRÁFICO
// =======================================================
let graficoInstancia = null;
let tipoGraficoActual = 'bar'; // Por defecto barras
let datosGlobalesGrafico = {
    etiquetas: [], // Ej: ['17 Jun', '18 Jun', '19 Jun']
    valores: []    // Ej: [450.50, 800.00, 120.00]
};

// Variables para controlar peticiones activas y evitar respuestas cruzadas.
let peticionKpisActual = null;
let peticionGraficoActual = null;
// Variables para conservar el rango usado en el ranking de ventas.
let peticionTopVentasActual = null;
let fechaInicioGlobal = null;
let fechaFinGlobal = null;

// =======================================================
// 1. OVERLAY DE CARGA CON BLOQUEO DEL BOTÓN
// =======================================================
function mostrarCargaGrafico(cargando) {
    const canvas = document.getElementById('graficoVentas');
    if (!canvas) return;
    
    const cardBody = canvas.parentElement;
    let overlay = document.getElementById('overlay-cargando-grafico');
    
    // Atrapamos el botón de descarga
    const btnDescargar = document.getElementById('btn-descargar-grafico');

    if (cargando) {
        // Bloqueamos el botón y lo ponemos gris
        if (btnDescargar) {
            btnDescargar.disabled = true;
            btnDescargar.innerHTML = '<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>';
        }

        if (!overlay) {
            overlay = document.createElement('div');
            overlay.id = 'overlay-cargando-grafico';
            overlay.className = 'position-absolute top-0 start-0 w-100 h-100 d-flex flex-column justify-content-center align-items-center bg-white bg-opacity-75 rounded-4';
            overlay.style.zIndex = '10';
            overlay.innerHTML = `
                <div class="spinner-border text-success mb-2" role="status" style="width: 2.5rem; height: 2.5rem;"></div>
                <span class="text-success fw-bold small">Cargando datos...</span>
            `;
            cardBody.classList.add('position-relative');
            cardBody.appendChild(overlay);
        }
        overlay.classList.remove('d-none');
    } else {
        // Desbloqueamos el botón y regresamos su ícono
        if (btnDescargar) {
            btnDescargar.disabled = false;
            btnDescargar.innerHTML = '<i class="bi bi-download"></i>';
        }

        if (overlay) {
            overlay.classList.add('d-none');
        }
    }
}

// =======================================================
// OBTENER DATOS DEL GRÁFICO (Controlado con AbortController)
// =======================================================
function obtenerDatosGrafico(inicio, fin) {
    console.log(`Pidiendo datos REALES para gráfico desde ${inicio} hasta ${fin}`);
    
    // Cancelamos petición de gráfico anterior si existe
    if (peticionGraficoActual) {
        peticionGraficoActual.abort();
    }
    peticionGraficoActual = new AbortController();
    const signalGrafico = peticionGraficoActual.signal;

    // Mostramos overlay de carga
    mostrarCargaGrafico(true);

    fetch(`/api/admin/dashboard/grafico?fechaInicio=${inicio}&fechaFin=${fin}`, { signal: signalGrafico })
        .then(response => {
            if (!response.ok) throw new Error('Error al obtener datos del gráfico');
            return response.json();
        })
        .then(data => {
            datosGlobalesGrafico.etiquetas = data.etiquetas;
            datosGlobalesGrafico.valores = data.valores;
            
            dibujarGrafico(tipoGraficoActual);
            mostrarCargaGrafico(false); // Éxito: ocultamos carga
        })
        .catch(error => {
            // Si cancelamos la petición, no hacemos nada (la nueva petición se encarga)
            if (error.name === 'AbortError') {
                console.log('Petición anterior de gráfico cancelada. Cargando la nueva...');
                return; // No ocultamos el spinner ni mostramos error
            }
            
            // Error real (servidor caído, etc.)
            console.error('Error cargando gráfico:', error);
            datosGlobalesGrafico.etiquetas = ['Error'];
            datosGlobalesGrafico.valores = [0];
            dibujarGrafico(tipoGraficoActual);
            mostrarCargaGrafico(false); // Quitamos spinner al mostrar error
        });
        // Se evita .finally() para no ocultar el spinner de una petición activa más reciente.
}

/**
 * Dibuja o redibuja el gráfico con Chart.js o muestra la tabla según el tipo.
 */
function dibujarGrafico(tipo) {
    const ctx = document.getElementById('graficoVentas');
    const tablaContainer = document.getElementById('contenedor-tabla-grafico');

    // Vista de tabla
    if (tipo === 'table') {
        ctx.classList.add('d-none');
        tablaContainer.classList.remove('d-none');
        construirTablaVentas();
        return;
    }

    // Vista de gráfico (canvas visible, tabla oculta)
    ctx.classList.remove('d-none');
    tablaContainer.classList.add('d-none');

    // Destruir instancia anterior para evitar superposiciones
    if (graficoInstancia) {
        graficoInstancia.destroy();
    }

    const colorFondo = tipo === 'pie'
        ? ['#198754', '#20c997', '#0dcaf0', '#fd7e14', '#ffc107', '#6f42c1']
        : '#198754';
    const colorBorde = tipo === 'pie' ? '#ffffff' : '#146c43';

    graficoInstancia = new Chart(ctx, {
        type: tipo,
        data: {
            labels: datosGlobalesGrafico.etiquetas,
            datasets: [{
                label: 'Ingresos S/',
                data: datosGlobalesGrafico.valores,
                backgroundColor: colorFondo,
                borderColor: colorBorde,
                borderWidth: tipo === 'pie' ? 2 : 0,
                borderRadius: tipo === 'bar' ? 4 : 0,
                fill: tipo === 'line'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: tipo === 'pie', position: 'right' }
            },
            scales: tipo === 'pie' ? {} : {
                y: { beginAtZero: true, grid: { borderDash: [4, 4] } },
                x: { grid: { display: false } }
            }
        }
    });
}

/**
 * Manejador de los botones de cambio de vista.
 */
window.cambiarTipoGrafico = function(tipo, botonHtml) {
    tipoGraficoActual = tipo;

    const botones = document.querySelectorAll('#controles-grafico button');
    botones.forEach(btn => btn.classList.remove('active'));
    botonHtml.classList.add('active');

    dibujarGrafico(tipo);
};

/**
 * Llena la tabla de datos en la vista de tabla.
 */
function construirTablaVentas() {
    const tbody = document.getElementById('body-tabla-grafico');
    tbody.innerHTML = '';

    for (let i = 0; i < datosGlobalesGrafico.etiquetas.length; i++) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td class="text-start fw-semibold text-muted">${datosGlobalesGrafico.etiquetas[i]}</td>
            <td class="text-success fw-bold">S/ ${datosGlobalesGrafico.valores[i].toFixed(2)}</td>
        `;
        tbody.appendChild(tr);
    }
}

// =======================================================
// 2. FUNCIÓN INTELIGENTE DE DESCARGA (Excel o PNG)
// =======================================================
window.descargarDatos = function() {
    if (!datosGlobalesGrafico.etiquetas || datosGlobalesGrafico.etiquetas.length === 0) {
        alert("No hay datos para descargar");
        return;
    }

    if (tipoGraficoActual === 'table') {
        // Modo tabla: descarga la información como archivo Excel (.xlsx).
        let datosExcel = [];
        for (let i = 0; i < datosGlobalesGrafico.etiquetas.length; i++) {
            datosExcel.push({
                "Fecha / Hora": datosGlobalesGrafico.etiquetas[i],
                "Ingresos Totales (S/)": parseFloat(datosGlobalesGrafico.valores[i])
            });
        }

        const hojaTrabajo = XLSX.utils.json_to_sheet(datosExcel);
        const libroTrabajo = XLSX.utils.book_new();
        XLSX.utils.book_append_sheet(libroTrabajo, hojaTrabajo, "Reporte de Ingresos");
        XLSX.writeFile(libroTrabajo, "Reporte_Ingresos_POS.xlsx");

    } else {
        // Modo gráfico: descarga la visualización como imagen PNG.
        if (graficoInstancia) {
            const canvas = document.getElementById('graficoVentas');
            const ctxCopia = canvas.getContext('2d');
            ctxCopia.save();
            ctxCopia.globalCompositeOperation = 'destination-over';
            ctxCopia.fillStyle = 'white';
            ctxCopia.fillRect(0, 0, canvas.width, canvas.height);
            
            const link = document.createElement('a');
            link.download = `Grafico_Ingresos_${tipoGraficoActual}.png`;
            link.href = canvas.toDataURL('image/png');
            link.click();
            
            ctxCopia.restore();
        }
    }
};

// =======================================================
// 3. ACTUALIZAR TOP VENTAS (LO MÁS VENDIDO) 
// =======================================================
window.actualizarTopVentas = function() {
    if (!fechaInicioGlobal || !fechaFinGlobal) return;
    
    const limite = document.getElementById('select-top-ventas').value;
    const ulVentas = document.getElementById('lista-top-ventas');
    
    // Mostramos estado de carga inicial
    ulVentas.innerHTML = `
        <li class="list-group-item px-0 py-4 border-0 text-center text-muted">
            <div class="spinner-border spinner-border-sm text-success mb-2" role="status"></div>
            <br><small>Cargando Top ${limite}...</small>
        </li>
    `;

    if (peticionTopVentasActual) {
        peticionTopVentasActual.abort();
    }
    peticionTopVentasActual = new AbortController();

    // Se envían fechas simples para que el backend construya el rango horario.
    fetch(`/api/admin/dashboard/top-ventas?fechaInicio=${fechaInicioGlobal}&fechaFin=${fechaFinGlobal}&limite=${limite}`, { signal: peticionTopVentasActual.signal })
        .then(response => {
            if (!response.ok) throw new Error('Error al obtener Top Ventas');
            return response.json();
        })
        .then(datos => {
            ulVentas.innerHTML = ''; 
            
            if (datos.length === 0) {
                ulVentas.innerHTML = `<li class="list-group-item border-0 text-center text-muted py-4">No hay ventas en este periodo.</li>`;
                return;
            }

            datos.forEach((item, index) => {
                let colorIcono = 'bg-secondary text-white';
                if (index === 0) colorIcono = 'bg-warning text-dark'; // Oro
                if (index === 1) colorIcono = 'bg-secondary bg-opacity-25 text-dark'; // Plata
                if (index === 2) colorIcono = 'bg-danger bg-opacity-25 text-danger'; // Bronce

                const li = document.createElement('li');
                li.className = 'list-group-item px-0 d-flex justify-content-between align-items-center border-bottom border-light py-2';
                li.innerHTML = `
                    <div class="d-flex align-items-center gap-3">
                        <div class="${colorIcono} rounded-circle d-flex align-items-center justify-content-center fw-bold shadow-sm" style="width: 35px; height: 35px; font-size: 0.9rem;">
                            #${index + 1}
                        </div>
                        <div class="overflow-hidden">
                            <h6 class="mb-0 fw-bold text-dark text-truncate" style="max-width: 160px;" title="${item.nombre}">${item.nombre}</h6>
                            <small class="text-muted text-truncate d-block" style="font-size: 0.75rem; max-width: 160px;">${item.categoria || 'General'}</small>
                        </div>
                    </div>
                    <div class="text-end">
                        <span class="badge bg-success bg-opacity-10 text-success rounded-pill px-3 py-2 shadow-sm border border-success border-opacity-25">
                            ${item.cantidadVendida} unds
                        </span>
                    </div>
                `;
                ulVentas.appendChild(li);
            });
        })
        .catch(error => {
            if (error.name !== 'AbortError') {
                console.error('Error en Top Ventas:', error);
                ulVentas.innerHTML = `<li class="list-group-item border-0 text-center text-danger py-4">Error al cargar datos.</li>`;
            }
        });
};

// =======================================================
// 4. ACTUALIZAR STOCK CRÍTICO (INVENTARIO ACTUAL) - NUEVO
// =======================================================
window.actualizarStockCritico = function() {
    const ulStock = document.getElementById('lista-stock-critico');

    fetch('/api/admin/dashboard/stock-critico') 
        .then(response => {
            if (!response.ok) throw new Error('Error al obtener stock crítico');
            return response.json();
        })
        .then(datos => {
            // Registro de diagnóstico para revisar los productos con stock crítico.
            console.log("Datos recibidos de stock:", datos);
            
            ulStock.innerHTML = ''; 

            if (datos.length === 0) {
                ulStock.innerHTML = `
                    <li class="list-group-item border-0 text-center text-muted py-4">
                        <i class="bi bi-check-circle-fill text-success fs-3 d-block mb-2"></i>
                        Inventario saludable
                    </li>`;
                return;
            }

            datos.forEach(item => {
                const esAgotado = item.stockActual <= 0;
                const badgeColor = esAgotado ? 'bg-danger' : 'bg-warning text-dark';
                const textColor = esAgotado ? 'text-danger fw-bold' : 'text-dark fw-semibold';

                const li = document.createElement('li');
                li.className = 'list-group-item px-0 d-flex justify-content-between align-items-center border-bottom border-light py-2';
                li.innerHTML = `
                    <div class="d-flex align-items-center gap-3">
                        <div class="bg-danger bg-opacity-10 rounded-circle d-flex align-items-center justify-content-center text-danger shadow-sm" style="width: 35px; height: 35px;">
                            <i class="bi ${esAgotado ? 'bi-x-octagon-fill' : 'bi-exclamation-triangle-fill'}"></i>
                        </div>
                        <div class="overflow-hidden">
                            <h6 class="mb-0 ${textColor} text-truncate" style="max-width: 160px;" title="${item.nombre}">${item.nombre}</h6>
                            <small class="text-muted text-truncate d-block" style="font-size: 0.75rem;">Mínimo sugerido: ${item.stockMinimo}</small>
                        </div>
                    </div>
                    <div class="text-end">
                        <span class="badge ${badgeColor} rounded-pill px-3 py-2 shadow-sm">
                            ${item.stockActual} en stock
                        </span>
                    </div>
                `;
                ulStock.appendChild(li);
            });
        })
        .catch(error => {
            console.error('Error cargando stock:', error);
            ulStock.innerHTML = `<li class="list-group-item text-danger border-0 text-center py-4">Error al revisar inventario</li>`;
        });
};

// =======================================================
// INICIALIZACIÓN DEL DASHBOARD
// =======================================================
window.initDashboardModule = function() {
    console.log("Iniciando Dashboard POS con Flatpickr...");

    // 1. Fecha actual en cabecera
    const fechaSpan = document.getElementById('fecha-actual');
    if (fechaSpan) {
        const opciones = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
        fechaSpan.textContent = new Date().toLocaleDateString('es-PE', opciones);
    }

    // 2. Función auxiliar para formatear fechas YYYY-MM-DD
    function formatearFechaLocal(fecha) {
        const anio = fecha.getFullYear();
        const mes = String(fecha.getMonth() + 1).padStart(2, '0');
        const dia = String(fecha.getDate()).padStart(2, '0');
        return `${anio}-${mes}-${dia}`;
    }

    const hoyStr = formatearFechaLocal(new Date());

    // 3. Configurar Flatpickr
    const inputFechas = document.getElementById('filtro-rango-fechas');
    if (inputFechas) {
        flatpickr(inputFechas, {
            mode: "range",
            dateFormat: "Y-m-d",
            locale: "es",
            defaultDate: [new Date(), new Date()],
            altInput: true,
            altFormat: "d M Y",
            onClose: function(selectedDates, dateStr, instance) {
                if (selectedDates.length === 2) {
                    const inicio = formatearFechaLocal(selectedDates[0]);
                    const fin = formatearFechaLocal(selectedDates[1]);
                    actualizarKpisPorFecha(inicio, fin);
                } else if (selectedDates.length === 1) {
                    const dia = formatearFechaLocal(selectedDates[0]);
                    actualizarKpisPorFecha(dia, dia);
                }
            }
        });
    }

    // 4. Carga inicial
    actualizarStockCritico(); // <-- NUEVA LÍNEA: carga stock crítico al iniciar
    actualizarKpisPorFecha(hoyStr, hoyStr);
};

// =======================================================
// ACTUALIZAR KPIs Y GRÁFICO (Controlado con AbortController)
// =======================================================
function actualizarKpisPorFecha(inicio, fin) {
    const fechaLimpiaInicio = inicio.split('T')[0];
    const fechaLimpiaFin = fin.split('T')[0];

    console.log(`Consultando API de dashboard: Inicio [${fechaLimpiaInicio}] - Fin [${fechaLimpiaFin}]`);

    fechaInicioGlobal = fechaLimpiaInicio;
    fechaFinGlobal = fechaLimpiaFin;
    actualizarTopVentas(); 

    if (peticionKpisActual) {
        peticionKpisActual.abort();
    }
    peticionKpisActual = new AbortController();
    const signalKpis = peticionKpisActual.signal;

    const montoVentas = document.getElementById('kpi-ventas-monto');
    const cantTransacciones = document.getElementById('kpi-ventas-cantidad');
    const ticketPromedio = document.getElementById('kpi-ticket-promedio');
    const stockCritico = document.getElementById('dash-stock-critico');

    const spinnerHTML = `<span class="spinner-border spinner-border-sm text-secondary" role="status" aria-hidden="true"></span>`;
    if (montoVentas) montoVentas.innerHTML = spinnerHTML;
    if (cantTransacciones) cantTransacciones.innerHTML = spinnerHTML;
    if (ticketPromedio) ticketPromedio.innerHTML = spinnerHTML;
    if (stockCritico) stockCritico.innerHTML = spinnerHTML;

    fetch(`/api/admin/dashboard/kpis?fechaInicio=${fechaLimpiaInicio}&fechaFin=${fechaLimpiaFin}`, { signal: signalKpis })
        .then(async response => {
            if (!response.ok) {
                const errorText = await response.text();
                throw new Error(`Error del servidor: ${response.status} - ${errorText}`);
            }
            return response.json();
        })
        .then(data => {
            if (montoVentas) montoVentas.textContent = data.totalVentas.toFixed(2);
            if (cantTransacciones) cantTransacciones.textContent = data.totalTransacciones;
            if (ticketPromedio) ticketPromedio.textContent = data.ticketPromedio.toFixed(2);
            
            // El backend retorna el total bajo la clave stockCritico.
            if (stockCritico) stockCritico.textContent = data.stockCritico || "0";

            obtenerDatosGrafico(fechaLimpiaInicio, fechaLimpiaFin);
        })
        .catch(error => {
            if (error.name === 'AbortError') return;

            console.error('Error al obtener datos del dashboard:', error);
            if (montoVentas) montoVentas.textContent = "0.00";
            if (cantTransacciones) cantTransacciones.textContent = "0";
            if (ticketPromedio) ticketPromedio.textContent = "0.00";
            if (stockCritico) stockCritico.textContent = "0";

            obtenerDatosGrafico(fechaLimpiaInicio, fechaLimpiaFin);
        });
}

// Fin de la lógica principal de admin-dashboard.js.
