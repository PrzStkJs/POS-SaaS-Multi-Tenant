/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Inventario y Productos
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de admin productos en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de admin-productos.js.

/* ============================================================
   MÓDULO DE GESTIÓN DE PRODUCTOS (VERSIÓN FINAL CON TOASTS)
   ============================================================ */

window.initProductosModule = function() {
    console.log("Inicializando Módulo de Productos...");

    // ───── Sistema de Notificaciones Flotantes (Toasts) ─────
    function mostrarNotificacion(mensaje, tipo = 'success') {
        const bgClass = tipo === 'success' ? 'bg-success' : (tipo === 'danger' ? 'bg-danger' : (tipo === 'warning' ? 'bg-warning text-dark' : 'bg-primary'));
        const iconClass = tipo === 'success' ? 'bi-check-circle-fill' : (tipo === 'danger' ? 'bi-x-circle-fill' : 'bi-exclamation-triangle-fill');
        const textClass = tipo === 'warning' ? 'text-dark' : 'text-white';
        const btnCloseClass = tipo === 'warning' ? '' : 'btn-close-white';

        let toastContainer = document.getElementById('toastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'toastContainer';
            toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            toastContainer.style.zIndex = '1100';
            document.body.appendChild(toastContainer);
        }

        const toastEl = document.createElement('div');
        toastEl.className = `toast align-items-center ${textClass} ${bgClass} border-0 mb-2 shadow-lg`;
        toastEl.setAttribute('role', 'alert');
        toastEl.setAttribute('aria-live', 'assertive');
        toastEl.setAttribute('aria-atomic', 'true');
        toastEl.innerHTML = `
            <div class="d-flex">
                <div class="toast-body fw-bold" style="font-size: 0.95rem;">
                    <i class="bi ${iconClass} me-2 fs-5 align-middle"></i> 
                    <span class="align-middle">${mensaje}</span>
                </div>
                <button type="button" class="btn-close ${btnCloseClass} me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        `;
        
        toastContainer.appendChild(toastEl);
        const toast = new bootstrap.Toast(toastEl, { delay: 3500 });
        toast.show();
        
        toastEl.addEventListener('hidden.bs.toast', () => {
            toastEl.remove();
        });
    }

    const tbodyProductos = document.getElementById('listaProductosBody');
    const selectCategoria = document.getElementById('categoriaProducto');
    const contadorProductos = document.getElementById('contadorProductos');
    const buscadorProductosInput = document.getElementById('buscadorProductos'); 
    
    const filtroCategoria = document.getElementById('filtroCategoriaProductos');
    const filtroStock = document.getElementById('filtroStockProductos');
    const btnExportarExcel = document.getElementById('btnExportarExcel');
    
    const btnNuevo = document.getElementById('btnNuevoProducto');
    const modalEl = document.getElementById('modalNuevoProducto');
    
    const inputCodigo = document.getElementById('codigoBarras');
    const btnBuscar = document.getElementById('btnBuscar');
    const inputNombre = document.getElementById('nombreProducto');
    const inputPrecio = document.getElementById('precioVenta');
    const imgProducto = document.getElementById('imagenProducto');
    const estadoBusqueda = document.getElementById('estadoBusqueda');
    const badgeEncontrado = document.getElementById('badgeEncontrado');
    const badgeNoEncontrado = document.getElementById('badgeNoEncontrado');
    const btnGuardar = document.getElementById('btnGuardar');

    let productosGlobal = []; 
    let productosFiltradosActivos = []; // NUEVA VARIABLE PARA LOS DATOS FILTRADOS
    let modalBs = null;
    let modalEditBs = null;
    let modalDeleteBs = null;
    let timeoutBusqueda = null;

	function cargarProductos() {
	        if (!tbodyProductos) return;
	        tbodyProductos.innerHTML = `<tr><td colspan="5" class="text-center text-muted"><div class="spinner-border spinner-border-sm text-green me-2" role="status"></div>Cargando productos...</td></tr>`;

	        fetch('/api/productos')
	            .then(response => {
	                if (!response.ok) throw new Error('No se pudo obtener la lista de productos');
	                return response.json();
	            })
	            .then(productos => {
	                const productosLimpios = productos.filter(p => p.codigoBarras !== 'GENERICO');
	                
	                productosGlobal = productosLimpios; 
	                aplicarFiltrosProductos(); 
	                cargarCategoriasEnFiltro();
	            })
	            .catch(error => {
	                console.error(error);
	                tbodyProductos.innerHTML = `<tr><td colspan="5" class="text-center text-danger">⚠️ Error al cargar los datos del servidor.</td></tr>`;
	            });
	    }

    function renderizarProductos(productos) {
        tbodyProductos.innerHTML = '';
        if (productos.length === 0) {
            tbodyProductos.innerHTML = `<tr><td colspan="5" class="text-center text-muted">No se encontraron productos con los filtros actuales.</td></tr>`;
            if (contadorProductos) contadorProductos.textContent = '0 productos';
            return;
        }

        productos.forEach(prod => {
            const categoriaNombre = (prod.categoria && prod.categoria.nombre) ? prod.categoria.nombre : 'Sin Categoría';
            const precioFormateado = parseFloat(prod.precioVenta).toFixed(2);
            let badgeStockClase = 'bg-success-subtle text-success';
            if (prod.stockActual === 0) badgeStockClase = 'bg-dark-subtle text-dark';
            else if (prod.stockActual <= 10) badgeStockClase = 'bg-danger-subtle text-danger';
            
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="font-monospace fw-semibold text-green">${prod.codigoBarras}</td>
                <td>
                    <div>
                        <span class="fw-bold text-dark d-block">${prod.nombre}</span>
                        <small class="badge bg-light text-muted border">${categoriaNombre}</small>
                    </div>
                </td>
                <td class="text-end fw-bold text-dark">S/. ${precioFormateado}</td>
                <td class="text-center">
                    <span class="badge ${badgeStockClase} rounded-pill fw-semibold">${prod.stockActual} und</span>
                </td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-warning btn-editar" data-id="${prod.id}"><i class="bi bi-pencil-square"></i></button>
                    <button class="btn btn-sm btn-outline-danger ms-1 btn-eliminar" data-id="${prod.id}"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tbodyProductos.appendChild(tr);
        });

        if (contadorProductos) {
            contadorProductos.textContent = `${productos.length} producto${productos.length === 1 ? '' : 's'}`;
        }
    }

    // ───── Filtros Combinados (con almacenamiento en variable global) ─────
    function aplicarFiltrosProductos() {
        const textoBusqueda = buscadorProductosInput ? buscadorProductosInput.value.trim().toLowerCase() : '';
        const idCategoria = filtroCategoria ? filtroCategoria.value : '';
        const estadoStock = filtroStock ? filtroStock.value : '';

        productosFiltradosActivos = productosGlobal.filter(prod => {
            const pasaTexto = textoBusqueda === '' || 
                (prod.codigoBarras && prod.codigoBarras.toLowerCase().includes(textoBusqueda)) ||
                (prod.nombre && prod.nombre.toLowerCase().includes(textoBusqueda));

            const pasaCategoria = idCategoria === '' || (prod.categoria && prod.categoria.id.toString() === idCategoria);

            let pasaStock = true;
            if (estadoStock === 'sin') pasaStock = prod.stockActual === 0;
            if (estadoStock === 'bajo') pasaStock = prod.stockActual > 0 && prod.stockActual <= 10;
            if (estadoStock === 'optimo') pasaStock = prod.stockActual > 10;

            return pasaTexto && pasaCategoria && pasaStock;
        });

        renderizarProductos(productosFiltradosActivos);
    }

    function cargarCategoriasEnSelects() {
        return fetch('/api/categorias')
            .then(response => response.json())
            .then(categorias => {
                let opciones = '<option value="" selected disabled>Selecciona una categoría...</option>';
                categorias.forEach(cat => {
                    opciones += `<option value="${cat.id}">${cat.nombre}</option>`;
                });
                
                if (selectCategoria) selectCategoria.innerHTML = opciones;
                const selectEditCategoria = document.getElementById('editCategoriaProducto');
                if (selectEditCategoria) selectEditCategoria.innerHTML = opciones;
            })
            .catch(err => console.error("Error al cargar categorías:", err));
    }

    function cargarCategoriasEnFiltro() {
        if (!filtroCategoria) return;
        fetch('/api/categorias')
            .then(response => response.json())
            .then(categorias => {
                let opciones = '<option value="">Todas las categorías</option>';
                categorias.forEach(cat => {
                    opciones += `<option value="${cat.id}">${cat.nombre}</option>`;
                });
                filtroCategoria.innerHTML = opciones;
            })
            .catch(err => console.error("Error al cargar categorías filtro:", err));
    }

    function consultarProducto(codigo) {
        if (!codigo) return;
        estadoBusqueda.classList.add('d-none');
        imgProducto.classList.add('d-none');

        const btnOriginalText = btnBuscar.innerHTML;
        btnBuscar.innerHTML = '<span class="spinner-border spinner-border-sm"></span>';
        btnBuscar.disabled = true;

        fetch(`https://world.openfoodfacts.org/api/v2/product/${codigo}.json`)
            .then(r => r.json())
            .then(data => {
                if (data.status === 1 && data.product) {
                    const p = data.product;
                    inputNombre.value = p.product_name || '';
                    if (p.image_url) {
                        imgProducto.querySelector('img').src = p.image_url;
                        imgProducto.classList.remove('d-none');
                    }
                    badgeEncontrado.classList.remove('d-none');
                    badgeNoEncontrado.classList.add('d-none');
                } else {
                    limpiarDatosConsulta();
                }
                estadoBusqueda.classList.remove('d-none');
                inputPrecio.focus();
                validarFormulario();
            })
            .catch(() => {
                limpiarDatosConsulta();
                estadoBusqueda.classList.remove('d-none');
                inputPrecio.focus();
                validarFormulario();
            })
            .finally(() => {
                btnBuscar.innerHTML = btnOriginalText;
                btnBuscar.disabled = false;
            });
    }

    function limpiarDatosConsulta() {
        inputNombre.value = '';
        imgProducto.classList.add('d-none');
        badgeEncontrado.classList.add('d-none');
        badgeNoEncontrado.classList.remove('d-none');
    }

    function validarFormulario() {
        const nombreOk = inputNombre.value.trim() !== '';
        const precioOk = inputPrecio.value.trim() !== '' && parseFloat(inputPrecio.value) > 0;
        btnGuardar.disabled = !(nombreOk && precioOk);
    }

    function limpiarModal() {
        if(inputCodigo) inputCodigo.value = '';
        if(inputNombre) inputNombre.value = '';
        if(inputPrecio) inputPrecio.value = '';
        if(imgProducto) imgProducto.classList.add('d-none');
        if(estadoBusqueda) estadoBusqueda.classList.add('d-none');
        if(btnGuardar) btnGuardar.disabled = true;
        if (selectCategoria) selectCategoria.innerHTML = '';
    }

    // ==========================================
    // LÓGICA DE EDITAR Y ELIMINAR 
    // ==========================================
    tbodyProductos.addEventListener('click', (e) => {
        const btnEditar = e.target.closest('.btn-editar');
        const btnEliminar = e.target.closest('.btn-eliminar');

        if (btnEditar) abrirModalEditar(btnEditar.dataset.id);
        if (btnEliminar) abrirModalEliminar(btnEliminar.dataset.id);
    });

    function abrirModalEditar(id) {
        const prod = productosGlobal.find(p => p.id == id);
        if (!prod) return;

        document.getElementById('editProductoId').value = prod.id;
        document.getElementById('editNombreProducto').value = prod.nombre;
        document.getElementById('editPrecioVenta').value = prod.precioVenta;
        
        cargarCategoriasEnSelects().then(() => {
            if (prod.categoria) {
                document.getElementById('editCategoriaProducto').value = prod.categoria.id;
            }
        });

        const modalEl = document.getElementById('modalEditarProducto');
        modalEditBs = bootstrap.Modal.getOrCreateInstance(modalEl);
        modalEditBs.show();
    }

    function abrirModalEliminar(id) {
        const prod = productosGlobal.find(p => p.id == id);
        if (!prod) return;

        document.getElementById('deleteProductoId').value = prod.id;
        document.getElementById('deleteNombreProducto').textContent = prod.nombre;

        const modalEl = document.getElementById('modalEliminarProducto');
        modalDeleteBs = bootstrap.Modal.getOrCreateInstance(modalEl);
        modalDeleteBs.show();
    }

    // ───── Botón Actualizar Producto ─────
    const btnActualizar = document.getElementById('btnActualizarProducto');
    if (btnActualizar) {
        btnActualizar.addEventListener('click', () => {
            const id = document.getElementById('editProductoId').value;
            const productoEditado = {
                nombre: document.getElementById('editNombreProducto').value.trim(),
                precioVenta: parseFloat(document.getElementById('editPrecioVenta').value),
                categoria: { id: parseInt(document.getElementById('editCategoriaProducto').value) }
            };

            const originalText = btnActualizar.innerHTML;
            btnActualizar.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Guardando...';
            btnActualizar.disabled = true;

            fetch(`/api/productos/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(productoEditado)
            })
            .then(async response => {
                if (!response.ok) throw new Error(await response.text());
                mostrarNotificacion('¡Producto actualizado exitosamente!', 'success');
                modalEditBs.hide();
                cargarProductos();
            })
            .catch(error => mostrarNotificacion('Error al actualizar: ' + error.message, 'danger'))
            .finally(() => {
                btnActualizar.innerHTML = originalText;
                btnActualizar.disabled = false;
            });
        });
    }

    // ───── Botón Confirmar Eliminación ─────
    const btnConfirmarEliminar = document.getElementById('btnConfirmarEliminar');
    if (btnConfirmarEliminar) {
        btnConfirmarEliminar.addEventListener('click', () => {
            const id = document.getElementById('deleteProductoId').value;
            const btnOriginalText = btnConfirmarEliminar.innerHTML;
            btnConfirmarEliminar.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Eliminando...';
            btnConfirmarEliminar.disabled = true;

            fetch(`/api/productos/${id}`, { method: 'DELETE' })
            .then(async response => {
                if (!response.ok) throw new Error(await response.text());
                mostrarNotificacion('¡Producto eliminado correctamente!', 'success');
                modalDeleteBs.hide();
                cargarProductos();
            })
            .catch(error => mostrarNotificacion('Error al eliminar: ' + error.message, 'danger'))
            .finally(() => {
                btnConfirmarEliminar.innerHTML = btnOriginalText;
                btnConfirmarEliminar.disabled = false;
            });
        });
    }

    // ───── Abrir modal nuevo producto ─────
    if (btnNuevo && modalEl) {
        btnNuevo.addEventListener('click', () => {
            modalBs = bootstrap.Modal.getOrCreateInstance(modalEl);
            modalBs.show();
            cargarCategoriasEnSelects();
        });
        modalEl.addEventListener('hidden.bs.modal', limpiarModal);
    }

    if (inputCodigo) {
        inputCodigo.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                consultarProducto(inputCodigo.value.trim());
            }
        });
    }
    
    if (btnBuscar) btnBuscar.addEventListener('click', () => consultarProducto(inputCodigo.value.trim()));
    if (inputNombre) inputNombre.addEventListener('input', validarFormulario);
    if (inputPrecio) inputPrecio.addEventListener('input', validarFormulario);
    
    // ───── Eventos de filtros (Debounce para texto) ─────
    if (buscadorProductosInput) {
        buscadorProductosInput.addEventListener('input', () => {
            clearTimeout(timeoutBusqueda);
            timeoutBusqueda = setTimeout(aplicarFiltrosProductos, 300);
        });
    }
    if (filtroCategoria) filtroCategoria.addEventListener('change', aplicarFiltrosProductos);
    if (filtroStock) filtroStock.addEventListener('change', aplicarFiltrosProductos);

    // ───── Exportar a Excel (usando los datos filtrados activos) ─────
    if (btnExportarExcel) {
        btnExportarExcel.addEventListener('click', () => {
            if (productosFiltradosActivos.length === 0) {
                mostrarNotificacion('No hay datos en la tabla con estos filtros para exportar.', 'warning');
                return;
            }

            const originalText = btnExportarExcel.innerHTML;
            btnExportarExcel.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Exportando...';
            btnExportarExcel.disabled = true;

            setTimeout(() => {
                const datosExcel = productosFiltradosActivos.map(prod => ({
                    'Código de Barras': prod.codigoBarras || 'S/C',
                    'Nombre del Producto': prod.nombre,
                    'Categoría': prod.categoria ? prod.categoria.nombre : 'Sin Categoría',
                    'Precio Venta (S/.)': prod.precioVenta,
                    'Stock Actual': prod.stockActual
                }));

                const worksheet = XLSX.utils.json_to_sheet(datosExcel);
                const workbook = XLSX.utils.book_new();
                XLSX.utils.book_append_sheet(workbook, worksheet, "Productos");
                
                XLSX.writeFile(workbook, "Reporte_Productos.xlsx");

                btnExportarExcel.innerHTML = originalText;
                btnExportarExcel.disabled = false;
            }, 500);
        });
    }

    // ───── Guardar Nuevo Producto ─────
    if (btnGuardar) {
        btnGuardar.addEventListener('click', () => {
            const categoriaId = selectCategoria ? selectCategoria.value : null;
            if (!categoriaId) {
                mostrarNotificacion('Por favor, selecciona una categoría antes de guardar.', 'warning');
                return;
            }

            const producto = {
                codigoBarras: inputCodigo.value.trim(),
                nombre: inputNombre.value.trim(),
                precioVenta: parseFloat(inputPrecio.value),
                stockActual: 0,
                categoria: { id: parseInt(categoriaId) }
            };

            const originalText = btnGuardar.innerHTML;
            btnGuardar.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Guardando...';
            btnGuardar.disabled = true;

            fetch('/api/productos', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(producto)
            })
            .then(async response => {
                if (response.ok) return response.json();
                if (response.status === 409) throw new Error(await response.text());
                throw new Error('Falló la conexión con el backend');
            })
            .then(() => {
                mostrarNotificacion('¡Producto registrado exitosamente!', 'success');
                if (modalBs) modalBs.hide();
                limpiarModal();
                cargarProductos();
            })
            .catch(error => mostrarNotificacion(error.message, 'danger'))
            .finally(() => {
                btnGuardar.innerHTML = originalText;
                validarFormulario();
            });
        });
    }

    // Ejecución inicial
    cargarProductos();
};

// Fin de la lógica principal de admin-productos.js.
