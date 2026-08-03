/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin stock en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin-stock.js.

// ===================================================
// MÓDULO DE GESTIÓN DE STOCK (con variable global)
// ===================================================
window.initStockModule = function() {
    console.log("Inicializando Módulo de Stock e Inventarios...");

    const tbodyStock = document.getElementById('listaStockBody');
    const kpiTotal = document.getElementById('kpi-total-productos');
    const kpiBajo = document.getElementById('kpi-stock-bajo');
    const kpiNegativo = document.getElementById('kpi-stock-negativo');

    const filtroStockSelect = document.getElementById('filtroStock');
    const buscadorStockInput = document.getElementById('buscadorStock'); 
    const ordenarPrecioSelect = document.getElementById('ordenarPrecio');
    const btnExportarExcel = document.getElementById('btnExportarExcel');

    const modalStockEl = document.getElementById('modalAjusteStock');
    const btnGuardarStock = document.getElementById('btnGuardarStock');
    let modalStockBs = modalStockEl ? bootstrap.Modal.getOrCreateInstance(modalStockEl) : null;

    const modalMovManualEl = document.getElementById('modalMovimientoManual');
    const inputCodigoKardex = document.getElementById('codigoBarrasKardex'); 
    const textNombreProducto = document.getElementById('nombreProductoKardex'); 
    const btnGuardarMovimiento = document.getElementById('btnGuardarMovimiento');
    let modalMovManualBs = modalMovManualEl ? bootstrap.Modal.getOrCreateInstance(modalMovManualEl) : null;

    window.inventarioGlobal = []; 
    let productoKardexId = null;
    let timeoutBusqueda = null; 

	function cargarStock() {
	        if (!tbodyStock) return;
	        tbodyStock.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-green me-2" role="status"></div>Cargando inventario...</td></tr>`;

	        fetch('/api/productos')
	            .then(response => {
	                if (!response.ok) throw new Error('No se pudo obtener el inventario');
	                return response.json();
	            })
	            .then(productos => {
	                const inventarioLimpio = productos.filter(p => p.codigoBarras !== 'GENERICO');
	                
	                window.inventarioGlobal = inventarioLimpio; 
	                aplicarFiltros(); 
	            })
	            .catch(error => {
	                console.error(error);
	                tbodyStock.innerHTML = `<tr><td colspan="6" class="text-center text-danger">⚠️ Error al conectar con la base de datos.</td></tr>`;
	            });
	    }

    function renderizarStock(productos) {
        tbodyStock.innerHTML = '';
        let contBajo = 0;
        let contNegativo = 0;

        if (productos.length === 0) {
            tbodyStock.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No hay artículos que coincidan con el filtro.</td></tr>`;
            if(kpiTotal) kpiTotal.textContent = 0;
            if(kpiBajo) kpiBajo.textContent = 0;
            if(kpiNegativo) kpiNegativo.textContent = 0;
            return;
        }

        productos.forEach(prod => {
            if (prod.stockActual < 0) contNegativo++;
            else if (prod.stockActual > 0 && prod.stockActual <= 10) contBajo++;

            let colorStock = 'text-dark', bgStock = 'bg-light';
            if (prod.stockActual < 0) {
                colorStock = 'text-danger fw-bold'; bgStock = 'bg-danger-subtle';
            } else if (prod.stockActual <= 10) {
                colorStock = 'text-warning fw-bold'; bgStock = 'bg-warning-subtle';
            } else {
                colorStock = 'text-success fw-bold'; bgStock = 'bg-success-subtle';
            }
            
            const categoriaNombre = prod.categoria ? prod.categoria.nombre : 'Sin Categoría';
            const precioFormateado = parseFloat(prod.precioVenta).toFixed(2);

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="font-monospace text-muted">${prod.codigoBarras}</td>
                <td class="fw-semibold text-dark">${prod.nombre}</td>
                <td><span class="badge bg-secondary">${categoriaNombre}</span></td>
                <td class="text-end fw-bold">S/. ${precioFormateado}</td>
                <td class="text-center">
                    <span class="badge ${bgStock} ${colorStock} border border-light p-2" style="font-size: 0.95rem;">${prod.stockActual}</span>
                </td>
                <td class="text-center">
                    <button class="btn btn-sm btn-light border" title="Ajustar Stock" data-id="${prod.id}">
                        <i class="bi bi-arrow-repeat text-green"></i>
                    </button>
                </td>
            `;
            tbodyStock.appendChild(tr);
        });

        if(kpiTotal) kpiTotal.textContent = productos.length;
        if(kpiBajo) kpiBajo.textContent = contBajo;
        if(kpiNegativo) kpiNegativo.textContent = contNegativo;
    }

    function aplicarFiltros() {
        const filtroSeleccionado = filtroStockSelect ? filtroStockSelect.value : 'todos';
        const textoBusqueda = buscadorStockInput ? buscadorStockInput.value.trim().toLowerCase() : '';
        const ordenSeleccionado = ordenarPrecioSelect ? ordenarPrecioSelect.value : 'defecto';

        let productosFiltrados = window.inventarioGlobal.filter(prod => {
            const coincideTexto = textoBusqueda === '' || 
                (prod.codigoBarras && prod.codigoBarras.toLowerCase().includes(textoBusqueda)) ||
                (prod.nombre && prod.nombre.toLowerCase().includes(textoBusqueda));

            let coincideEstado = true;
            const stock = prod.stockActual || 0;

            if (filtroSeleccionado === 'positivos') coincideEstado = stock > 0;
            else if (filtroSeleccionado === 'bajos') coincideEstado = stock > 0 && stock <= 10;
            else if (filtroSeleccionado === 'agotados') coincideEstado = stock === 0;
            else if (filtroSeleccionado === 'negativos') coincideEstado = stock < 0;

            return coincideTexto && coincideEstado;
        });

        if (ordenSeleccionado === 'mayor') {
            productosFiltrados.sort((a, b) => (parseFloat(b.precioVenta) || 0) - (parseFloat(a.precioVenta) || 0));
        } else if (ordenSeleccionado === 'menor') {
            productosFiltrados.sort((a, b) => (parseFloat(a.precioVenta) || 0) - (parseFloat(b.precioVenta) || 0));
        }

        renderizarStock(productosFiltrados);
    }

    function buscarProductoKardex(codigo) {
        if (!codigo) {
            textNombreProducto.innerHTML = 'Producto Seleccionado: <span class="text-muted fw-normal">Ninguno</span>';
            productoKardexId = null;
            return;
        }
        textNombreProducto.innerHTML = 'Producto Seleccionado: <span class="text-primary">Buscando...</span>';
        
        fetch('/api/productos') 
            .then(res => res.json())
            .then(productos => {
                const producto = productos.find(p => p.codigoBarras === codigo);
                if (producto) {
                    textNombreProducto.innerHTML = `Producto Seleccionado: <span class="fw-bold text-success">${producto.nombre}</span>`;
                    productoKardexId = producto.id;
                    document.getElementById('cantidadMovimiento').focus(); 
                } else {
                    textNombreProducto.innerHTML = `Producto Seleccionado: <span class="fw-bold text-danger">No encontrado en BD</span>`;
                    productoKardexId = null;
                }
            })
            .catch(() => {
                textNombreProducto.innerHTML = `Producto Seleccionado: <span class="text-danger">Error de red</span>`;
            });
    }

    if (ordenarPrecioSelect) ordenarPrecioSelect.addEventListener('change', aplicarFiltros);
    if (filtroStockSelect) filtroStockSelect.addEventListener('change', aplicarFiltros);
    if (buscadorStockInput) buscadorStockInput.addEventListener('input', aplicarFiltros);

    if (tbodyStock) {
        tbodyStock.addEventListener('click', (e) => {
            const btn = e.target.closest('button[title="Ajustar Stock"]');
            if (btn) {
                const productoId = btn.getAttribute('data-id');
                const fila = btn.closest('tr');
                const codigo = fila.querySelector('td:nth-child(1)').textContent.trim();
                const nombre = fila.querySelector('td:nth-child(2)').textContent.trim();
                const stockActual = fila.querySelector('td:nth-child(5) span').textContent.trim();

                document.getElementById('idProductoAjuste').value = productoId;
                document.getElementById('codigoProductoAjuste').textContent = codigo;
                document.getElementById('nombreProductoAjuste').textContent = nombre;
                document.getElementById('stockActualVisual').value = stockActual;
                document.getElementById('nuevoStockInput').value = stockActual; 

                modalStockBs.show();
            }
        });
    }

    if (btnGuardarStock) {
        btnGuardarStock.addEventListener('click', () => {
            const id = document.getElementById('idProductoAjuste').value;
            const nuevoStock = parseInt(document.getElementById('nuevoStockInput').value);
            const stockActual = parseInt(document.getElementById('stockActualVisual').value);

            if (isNaN(nuevoStock)) {
                mostrarNotificacion("Por favor, ingresa un valor numérico.", "warning");
                return;
            }

            let tipoMovimiento = "", diferencia = 0;
            if (nuevoStock > stockActual) {
                tipoMovimiento = "ENTRADA"; diferencia = nuevoStock - stockActual;
            } else if (nuevoStock < stockActual) {
                tipoMovimiento = "SALIDA"; diferencia = stockActual - nuevoStock;
            } else {
                mostrarNotificacion("El stock ingresado es igual al actual.", "warning");
                return;
            }

            const payload = {
                productoId: id, tipo: tipoMovimiento, cantidad: diferencia,
                motivo: "AJUSTE_INVENTARIO", descripcion: `Ajuste manual de stock (De ${stockActual} a ${nuevoStock})`
            };

            btnGuardarStock.disabled = true;
            btnGuardarStock.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Guardando...';

            fetch(`/api/inventario/movimiento`, {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            })
            .then(async r => {
                if (!r.ok) throw new Error((await r.json()).error || "Error");
                return r.json();
            })
            .then(() => {
                mostrarNotificacion("Stock ajustado correctamente.", "success");
                modalStockBs.hide();
                cargarStock(); 
            })
            .catch(err => mostrarNotificacion(`Problema: ${err.message}`, "danger"))
            .finally(() => {
                btnGuardarStock.disabled = false;
                btnGuardarStock.textContent = 'Actualizar Inventario';
            });
        });
    }

    if (modalMovManualEl) {
        modalMovManualEl.addEventListener('shown.bs.modal', () => inputCodigoKardex.focus());
    }

    if (inputCodigoKardex) {
        inputCodigoKardex.addEventListener('input', () => {
            clearTimeout(timeoutBusqueda);
            const codigo = inputCodigoKardex.value.trim();
            timeoutBusqueda = setTimeout(() => buscarProductoKardex(codigo), 300);
        });
        inputCodigoKardex.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                clearTimeout(timeoutBusqueda);
                buscarProductoKardex(inputCodigoKardex.value.trim());
            }
        });
    }

    if (btnGuardarMovimiento) {
        btnGuardarMovimiento.addEventListener('click', () => {
            const cantidad = parseInt(document.getElementById('cantidadMovimiento').value);
            const motivo = document.getElementById('motivoMovimiento').value;
            const descripcion = document.getElementById('descripcionMovimiento').value.trim();
            const tipo = document.querySelector('input[name="btnTipoMovimiento"]:checked')?.value;

            if (!productoKardexId) {
                mostrarNotificacion("Escanea o busca primero un producto válido.", "warning"); 
                return;
            }
            if (isNaN(cantidad) || cantidad <= 0) {
                mostrarNotificacion("Ingresa una cantidad mayor a 0.", "warning"); 
                return;
            }

            btnGuardarMovimiento.disabled = true;
            btnGuardarMovimiento.textContent = 'Guardando...';

            const payload = { productoId: productoKardexId, tipo, cantidad, motivo, descripcion };

            fetch('/api/inventario/movimiento', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload)
            })
            .then(res => { if (!res.ok) throw new Error('Error'); return res.json(); })
            .then(() => {
                mostrarNotificacion('Movimiento registrado con éxito.', 'success');
                inputCodigoKardex.value = '';
                textNombreProducto.innerHTML = 'Producto Seleccionado: <span class="text-muted fw-normal">Ninguno</span>';
                productoKardexId = null;
                document.getElementById('cantidadMovimiento').value = '';
                document.getElementById('descripcionMovimiento').value = '';
                modalMovManualBs.hide();
                cargarStock();
            })
            .catch(() => mostrarNotificacion('Error al registrar el movimiento', 'danger'))
            .finally(() => {
                btnGuardarMovimiento.disabled = false;
                btnGuardarMovimiento.textContent = 'Registrar Cambios';
            });
        });
    }

    if (btnExportarExcel) {
        btnExportarExcel.addEventListener('click', async () => {
            const textoOriginal = btnExportarExcel.innerHTML;
            btnExportarExcel.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>Generando...';
            btnExportarExcel.disabled = true;

            try {
                const workbook = new ExcelJS.Workbook();
                const worksheet = workbook.addWorksheet('Reporte de Stock');
                worksheet.columns = [
                    { header: 'CÓDIGO', key: 'codigo', width: 15 },
                    { header: 'PRODUCTO', key: 'producto', width: 40 },
                    { header: 'CATEGORÍA', key: 'categoria', width: 25 },
                    { header: 'PRECIO VENTA', key: 'precio', width: 15 },
                    { header: 'STOCK FÍSICO', key: 'stock', width: 20 }
                ];
                
                const headerRow = worksheet.getRow(1);
                headerRow.font = { name: 'Arial', bold: true, color: { argb: 'FFFFFFFF' } };
                headerRow.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF198754' } };
                headerRow.alignment = { vertical: 'middle', horizontal: 'center' };
                headerRow.height = 25;

                document.querySelectorAll('#listaStockBody tr').forEach(fila => {
                    if (fila.cells.length > 1 && !fila.innerText.includes('Cargando')) {
                        const stockNumero = parseInt(fila.cells[4].innerText.trim()) || 0; 
                        const row = worksheet.addRow({
                            codigo: fila.cells[0].innerText.trim(),
                            producto: fila.cells[1].innerText.trim(),
                            categoria: fila.cells[2].innerText.trim(),
                            precio: fila.cells[3].innerText.trim(),
                            stock: stockNumero
                        });
                        row.alignment = { vertical: 'middle', horizontal: 'left' };
                        row.getCell('precio').alignment = { horizontal: 'right' };
                        row.getCell('stock').alignment = { horizontal: 'center' };

                        if (stockNumero < 0) row.getCell('stock').font = { color: { argb: 'FFDC3545' }, bold: true };
                        else if (stockNumero <= 10) row.getCell('stock').font = { color: { argb: 'FFFFA500' }, bold: true };
                    }
                });

                worksheet.eachRow({ includeEmpty: false }, function(row) {
                    row.eachCell({ includeEmpty: false }, function(cell) {
                        cell.border = {
                            top: { style: 'thin' }, left: { style: 'thin' },
                            bottom: { style: 'thin' }, right: { style: 'thin' }
                        };
                    });
                });

                const buffer = await workbook.xlsx.writeBuffer();
                const blob = new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
                saveAs(blob, `Kardex_Stock_${new Date().toISOString().split('T')[0]}.xlsx`);

            } catch (error) {
                console.error(error);
                mostrarNotificacion("Hubo un error al generar el Excel.", "danger");
            } finally {
                btnExportarExcel.innerHTML = textoOriginal;
                btnExportarExcel.disabled = false;
            }
        });
    }

    cargarStock();
};

// ===================================================
// MÓDULO DE OCR PARA FACTURAS (VERSIÓN ROBUSTA CON EVENTOS DIRECTOS)
// ===================================================
window.initOcrModule = function() {
    console.log("Módulo OCR inicializado correctamente");

    function renderizarTablaValidacion(productosLeidos) {
        const tbodyBody = document.getElementById('tablaOcrBody');
        tbodyBody.innerHTML = '';
        
        let opcionesSelect = `<option value="">❌ Seleccionar manualmente...</option>`;
        if (window.inventarioGlobal && window.inventarioGlobal.length > 0) {
            window.inventarioGlobal.forEach(prod => {
                opcionesSelect += `<option value="${prod.id}">${prod.nombre} (${prod.codigoBarras || 'Sin código'})</option>`;
            });
        }

        productosLeidos.forEach((item, index) => {
            let idMatch = "";
            const descLeidaLower = item.descripcion.toLowerCase();
            
            if (window.inventarioGlobal && window.inventarioGlobal.length > 0) {
                const posibleMatch = window.inventarioGlobal.find(p => 
                    descLeidaLower.includes(p.nombre.toLowerCase().substring(0, 5))
                );
                if (posibleMatch) idMatch = posibleMatch.id;
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="text-muted small">${item.descripcion}</td>
                <td>
                    <select class="form-select form-select-sm select-match-producto" data-index="${index}">
                        ${opcionesSelect}
                    </select>
                </td>
                <td>
                    <input type="number" class="form-control form-control-sm text-center input-cantidad" value="${item.cantidad}">
                </td>
            `;
            tbodyBody.appendChild(tr);
            
            const select = tr.querySelector('select');
            
            if (idMatch !== "") {
                select.value = idMatch;
                select.classList.add('border-success', 'text-success');
            } else {
                select.classList.add('border-danger');
            }

            select.addEventListener('change', function() {
                if (this.value !== "") {
                    this.classList.remove('border-danger');
                    this.classList.add('border-success', 'text-success');
                } else {
                    this.classList.remove('border-success', 'text-success');
                    this.classList.add('border-danger');
                }
            });
        });
    }

    document.addEventListener('click', function(e) {
        
        if (e.target && e.target.id === 'btnProcesarIA') {
            console.log("👉 Botón 'Analizar Documento' clickeado!");
            
            const btnProcesar = e.target;
            const inputArchivo = document.getElementById('archivoFactura');
            const loader = document.getElementById('ocrLoader');
            const contenedorResultados = document.getElementById('ocrResultados');

            const file = inputArchivo.files[0];
            if (!file) {
                mostrarNotificacion("Por favor selecciona una foto o PDF de la factura primero.", "warning");
                return;
            }

            const formData = new FormData();
            formData.append("archivo", file);

            loader.classList.remove('d-none');
            contenedorResultados.classList.add('d-none');
            btnProcesar.disabled = true;

            console.log("Enviando petición al backend simulado...");

            fetch('/api/ia/ocr/procesar', {
                method: 'POST',
                body: formData
            })
            .then(async response => {
                if (!response.ok) throw new Error("Error en la respuesta del servidor");
                const textoPlano = await response.text(); 
                console.log("JSON recibido del servidor:", textoPlano);
                return JSON.parse(textoPlano);
            })
            .then(data => {
                renderizarTablaValidacion(data);
                loader.classList.add('d-none');
                contenedorResultados.classList.remove('d-none');
                mostrarNotificacion("Factura analizada correctamente.", "success");
            })
            .catch(err => {
                console.error("❌ Error en Fetch:", err);
                mostrarNotificacion("Error al procesar la factura. Revisa la consola para más detalles.", "danger");
                loader.classList.add('d-none');
            })
            .finally(() => {
                btnProcesar.disabled = false;
            });
        }

		if (e.target && e.target.id === 'btnConfirmarOcr') {
		            console.log("👉 Botón 'Confirmar' clickeado!");
		            
		            const tbodyBody = document.getElementById('tablaOcrBody');
		            const filas = tbodyBody.querySelectorAll('tr');
		            const movimientos = [];
		            let errores = false;

		            filas.forEach(fila => {
		                const selectElement = fila.querySelector('.select-match-producto');
		                const selectId = selectElement.value;
		                const inputCantidad = fila.querySelector('.input-cantidad');
		                const cantidad = parseInt(inputCantidad.value);

		                if (!selectId) {
		                    selectElement.classList.remove('border-success', 'text-success');
		                    selectElement.classList.add('border-danger');
		                    errores = true;
		                } else {
		                    selectElement.classList.remove('border-danger');
		                    selectElement.classList.add('border-success', 'text-success');
		                }

		                if (isNaN(cantidad) || cantidad <= 0) {
		                    inputCantidad.classList.add('border-danger', 'text-danger');
		                    errores = true;
		                } else {
		                    inputCantidad.classList.remove('border-danger', 'text-danger');
		                }

		                if (selectId && !isNaN(cantidad) && cantidad > 0) {
		                    movimientos.push({
		                        productoId: selectId,
		                        tipo: "ENTRADA",
		                        cantidad: cantidad,
		                        motivo: "COMPRA",
		                        descripcion: "Carga automatizada por OCR IA"
		                    });
		                }
		            });

		            if (errores) {
		                mostrarNotificacion("Revisa las casillas en rojo: Hay productos sin emparejar o cantidades inválidas (0).", "warning");
		                return; 
		            }

		            // 🛡️ Deshabilitar botón para evitar doble clic mientras procesa la carga masiva
		            const btnConfirmar = e.target;
		            btnConfirmar.disabled = true;
		            btnConfirmar.textContent = 'Guardando stock...';

		            // Ejecutamos las peticiones de forma controlada
		            (async () => {
		                try {
		                    let exitos = true;
		                    for (const mov of movimientos) {
		                        const response = await fetch('/api/inventario/movimiento', {
		                            method: 'POST',
		                            headers: { 'Content-Type': 'application/json' },
		                            body: JSON.stringify(mov)
		                        });
		                        if (!response.ok) {
		                            exitos = false;
		                            break;
		                        }
		                    }

		                    if (exitos) {
		                        mostrarNotificacion("Factura cargada y stock actualizado exitosamente.", "success");
		                        const modalElement = document.getElementById('modalOcrFactura');
		                        const modal = bootstrap.Modal.getInstance(modalElement);
		                        if (modal) modal.hide();
		                        if (window.initStockModule) window.initStockModule(); 
		                    } else {
		                        mostrarNotificacion("Hubo un problema al registrar algunos productos. Verifica que pertenezcan a tu inventario.", "danger");
		                    }
		                } catch (error) {
		                    console.error(error);
		                    mostrarNotificacion("Error de red al guardar los movimientos.", "danger");
		                } finally {
		                    btnConfirmar.disabled = false;
		                    btnConfirmar.textContent = 'Confirmar y Actualizar Stock';
		                }
		            })();
		        }
    });
};

// ===================================================
// INICIALIZACIÓN AUTOMÁTICA AL CARGAR LA PÁGINA
// ===================================================
document.addEventListener('DOMContentLoaded', function() {
    window.initStockModule();
    window.initOcrModule();
});

// Función global para notificaciones tipo toast (compatible con Bootstrap 5)
function mostrarNotificacion(mensaje, tipo = 'info') {
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'position-fixed bottom-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }

    const colorMap = {
        success: 'bg-success text-white',
        danger: 'bg-danger text-white',
        warning: 'bg-warning text-dark',
        info: 'bg-info text-dark'
    };
    const bgClass = colorMap[tipo] || colorMap.info;

    const toastId = `toast-${Date.now()}`;
    const toastHTML = `
        <div id="${toastId}" class="toast ${bgClass}" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">${mensaje}</div>
                <button type="button" class="btn-close me-2 m-auto" data-bs-dismiss="toast" aria-label="Cerrar"></button>
            </div>
        </div>
    `;
    container.insertAdjacentHTML('beforeend', toastHTML);

    const toastElement = document.getElementById(toastId);
    const bsToast = new bootstrap.Toast(toastElement, { delay: 4000 });
    bsToast.show();

    toastElement.addEventListener('hidden.bs.toast', () => {
        toastElement.remove();
    });
}

// Fin de la lógica principal de admin-stock.js.
