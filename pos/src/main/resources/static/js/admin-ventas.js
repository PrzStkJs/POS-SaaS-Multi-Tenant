/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Administración
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin ventas en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin-ventas.js.

// ===========================================
// SECCIÓN 1: Variable global para guardar los datos de la tabla
// ===========================================
window.ventasActuales = [];

window.initVentasModule = function() {
    console.log("Módulo de Historial de Ventas Inicializado.");

    const hoy = new Date().toISOString().split('T')[0];
    document.getElementById('filtroFechaInicio').value = hoy;
    document.getElementById('filtroFechaFin').value = hoy;

    cargarHistorialVentas();
};

window.cargarHistorialVentas = function() {
    const ticketId = document.getElementById('filtroTicket')?.value;
    const fechaInicio = document.getElementById('filtroFechaInicio').value;
    const fechaFin = document.getElementById('filtroFechaFin').value;
    
    const estado = document.getElementById('filtroEstado')?.value; 

    const tbody = document.getElementById('bodyTablaVentas');
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4"><div class="spinner-border spinner-border-sm text-primary"></div> Cargando...</td></tr>`;

    let url = `/api/ventas/historial?fechaInicio=${fechaInicio}&fechaFin=${fechaFin}`;
    
    if (ticketId) url += `&ticket=${ticketId}`;
    
    if (estado) url += `&estado=${estado}`; 

    fetch(url)
        .then(response => {
            if (!response.ok) throw new Error('Error en la respuesta');
            return response.json();
        })
        .then(ventas => {
            // ===========================================
            // SECCIÓN 2: Guardar los datos para exportar
            // ===========================================
            window.ventasActuales = ventas;

            tbody.innerHTML = '';
            if (ventas.length === 0) {
                tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No se encontraron ventas.</td></tr>`;
                return;
            }

			ventas.forEach(venta => {
			                const ticketFormateado = String(venta.idTicket).padStart(6, '0');
			                const fecha = new Date(venta.fechaHora).toLocaleString('es-PE', {
			                    year: 'numeric', month: '2-digit', day: '2-digit',
			                    hour: '2-digit', minute:'2-digit'
			                });

			                // Construye la etiqueta visual de acuerdo con el estado de la venta.
			                let badgeEstado = '';
			                if (venta.estado === 'COMPLETADA') {
			                    badgeEstado = '<span class="badge bg-success"><i class="bi bi-check-circle"></i> Completada</span>';
			                } else if (venta.estado === 'PENDIENTE_PAGO') {
			                    badgeEstado = '<span class="badge bg-warning text-dark"><i class="bi bi-clock-history"></i> Pendiente</span>';
			                } else {
			                    badgeEstado = '<span class="badge bg-danger"><i class="bi bi-x-circle"></i> Anulada</span>';
			                }

			                const tr = document.createElement('tr');
			                tr.innerHTML = `
			                    <td class="px-4 py-3 fw-bold text-secondary text-nowrap align-middle">#${ticketFormateado}</td>
			                    <td class="py-3 text-nowrap align-middle">${fecha}</td>
			                    <td class="py-3 text-nowrap align-middle"><i class="bi bi-person-badge me-2"></i>${venta.cajero}</td>
			                    <td class="py-3 text-end fw-bold text-dark text-nowrap align-middle">S/ ${venta.total.toFixed(2)}</td>
			                    <td class="py-3 text-center align-middle">${badgeEstado}</td>
			                    <td class="px-4 py-3 align-middle">
			                        <div class="d-flex justify-content-center align-items-center gap-2">
			                            <button class="btn btn-sm btn-outline-primary" onclick="verDetalleVenta(${venta.idTicket})" title="Ver Detalle">
			                                <i class="bi bi-eye"></i>
			                            </button>
			                            ${venta.estado === 'COMPLETADA' ?
			                              `<button class="btn btn-sm btn-outline-danger" onclick="anularVenta(${venta.idTicket})" title="Anular">
			                                  <i class="bi bi-x-circle"></i>
			                              </button>` : ''}
			                        </div>
			                    </td>
			                `;
			                tbody.appendChild(tr);
			            });
        })
        .catch(error => {
            console.error('Error al cargar historial:', error);
            tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger py-4"><i class="bi bi-exclamation-triangle"></i> Error de conexión.</td></tr>`;
        });
};

// ===========================================
// SECCIÓN 3: Nueva función para exportar a Excel
// ===========================================
window.exportarExcel = function() {
    if (!window.ventasActuales || window.ventasActuales.length === 0) {
        alert("⚠️ No hay datos en la tabla para exportar.");
        return;
    }

    // Formatear datos con cabeceras amigables
    const datosParaExcel = window.ventasActuales.map(venta => {
        const ticketFormateado = String(venta.idTicket).padStart(6, '0');
        const fecha = new Date(venta.fechaHora).toLocaleString('es-PE', {
            year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute:'2-digit'
        });

        return {
            "N° Ticket": `#${ticketFormateado}`,
            "Fecha de Venta": fecha,
            "Cajero Encargado": venta.cajero,
            "Total (S/)": parseFloat(venta.total.toFixed(2)),
            "Estado": venta.estado
        };
    });

    // Crear hoja de cálculo
    const worksheet = XLSX.utils.json_to_sheet(datosParaExcel);

    // Ancho de columnas
    worksheet['!cols'] = [
        { wch: 15 }, // N° Ticket
        { wch: 25 }, // Fecha de Venta
        { wch: 25 }, // Cajero Encargado
        { wch: 15 }, // Total
        { wch: 15 }  // Estado
    ];

    // Libro y descarga
    const workbook = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(workbook, worksheet, "Reporte de Ventas");

    const fechaHoy = new Date().toISOString().split('T')[0];
    const nombreArchivo = `Reporte_Ventas_${fechaHoy}.xlsx`;
    XLSX.writeFile(workbook, nombreArchivo);
};

// ===========================================
// SECCIÓN 4: Funciones existentes (Actualizada)
// ===========================================
window.verDetalleVenta = function(idTicket) {
    const tbody = document.getElementById('bodyDetalleVenta');
    document.getElementById('modalTicketId').textContent = `#${String(idTicket).padStart(6, '0')}`;
    
    tbody.innerHTML = `<tr><td colspan="4" class="text-center py-4"><div class="spinner-border spinner-border-sm text-primary"></div> Cargando detalles...</td></tr>`;

    const modal = new bootstrap.Modal(document.getElementById('modalDetalleVenta'));
    modal.show();

    fetch(`/api/ventas/${idTicket}/detalles`)
        .then(response => {
            if (!response.ok) throw new Error('Error al obtener detalles');
            return response.json();
        })
        .then(detalles => {
            tbody.innerHTML = '';
            
            if (detalles.length === 0) {
                tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-3">No hay productos registrados.</td></tr>`;
                return;
            }

            detalles.forEach(d => {
                let nombreProducto = d.nombreAlternativo ? 
                    `<span class="text-primary fw-semibold">${d.nombreAlternativo}</span> <span class="badge bg-warning text-dark text-xs ms-1">Rápido</span>` : 
                    d.producto.nombre;

                if (d.esPromocion) {
                    nombreProducto += ` <span class="badge bg-info text-dark text-xs ms-2">⭐ Promo</span>`;
                }

                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td class="ps-4 fw-bold">${d.cantidad}</td>
                    <td>${nombreProducto}</td>
                    <td class="text-end">S/ ${d.precioUnitario.toFixed(2)}</td>
                    <td class="text-end pe-4 fw-bold text-dark">S/ ${d.subtotal.toFixed(2)}</td>
                `;
                tbody.appendChild(tr);
            });
        })
        .catch(error => {
            console.error('Error:', error);
            tbody.innerHTML = `<tr><td colspan="4" class="text-center text-danger py-3">Error al cargar los detalles.</td></tr>`;
        });
};

window.anularVenta = function(idTicket) {
    const ticketFormateado = String(idTicket).padStart(6, '0');
    
    // 1. Mostrar Modal de Confirmación
    Swal.fire({
        title: `¿Anular Ticket #${ticketFormateado}?`,
        text: "Esta acción devolverá los productos al inventario y no se puede deshacer.",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#dc3545', // Rojo de Bootstrap (danger)
        cancelButtonColor: '#6c757d',  // Gris de Bootstrap (secondary)
        confirmButtonText: '<i class="bi bi-x-circle"></i> Sí, anular venta',
        cancelButtonText: 'Cancelar',
        reverseButtons: true // Pone el botón de cancelar a la izquierda
    }).then((result) => {
        // 2. Si el usuario confirma, hacemos el fetch
        if (result.isConfirmed) {
            
            // Modal de carga mientras responde el servidor
            Swal.fire({
                title: 'Procesando...',
                text: 'Anulando la venta y restaurando stock',
                allowEscapeKey: false,
                allowOutsideClick: false,
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            fetch(`/api/ventas/${idTicket}/anular`, {
                method: 'PUT'
            })
            .then(async response => {
                if (!response.ok) {
                    const errText = await response.text();
                    throw new Error(errText || 'Error al anular la venta');
                }
                return response.json();
            })
            .then(() => {
                // 3. Modal de Éxito
                Swal.fire({
                    title: '¡Anulada!',
                    text: 'La venta ha sido anulada y el stock devuelto correctamente.',
                    icon: 'success',
                    confirmButtonColor: '#198754' // Verde de Bootstrap (success)
                });
                cargarHistorialVentas(); // Recargamos la tabla
            })
            .catch(error => {
                console.error('Error:', error);
                // 4. Modal de Error
                Swal.fire({
                    title: 'Error',
                    text: `No se pudo anular: ${error.message}`,
                    icon: 'error',
                    confirmButtonColor: '#0d6efd' // Azul
                });
            });
        }
    });
};

// Fin de la lógica principal de admin-ventas.js.
