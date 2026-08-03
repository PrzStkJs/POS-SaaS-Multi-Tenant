/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de ventas en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  DEPENDENCIAS DEL MÓDULO
 * ============================================================ */

// Inicio de imports y dependencias externas.

import {
  agregarProductoAlCarrito,
  sumarCantidad,
  restarCantidad,
  eliminarProducto,
  vaciarCarrito,
  calcularSubtotal,
  calcularTotalVenta,
  obtenerCarrito
} from './ventas-carrito.js';

import {
  buscarProductoLocal,
  buscarProductoExterno
} from './ventas-api.js';

import {
  Toast,
  renderizarTablaUI,
  actualizarTotalesUI,
  actualizarListaResumenUI,
  mostrarCrossSellingUI,
  ocultarCrossSellingUI
} from './ventas-ui.js';

import { inicializarPagos } from './ventas-pagos.js';

// Fin de imports y dependencias externas.



/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de ventas.js.

(() => {
  const inputCodigo = document.getElementById('inputCodigo');
  const btnAgregarManual = document.getElementById('btnAgregarManual');
  const listaProductos = document.getElementById('listaProductos');
  const contadorItems = document.getElementById('contadorItems');
  const subtotalEl = document.getElementById('subtotal');
  const igvEl = document.getElementById('igv');
  const totalEl = document.getElementById('total');
  const btnCobrar = document.getElementById('btnCobrar');
  const btnCancelar = document.getElementById('btnCancelarVenta');

  const modalRapidoEl = document.getElementById('modalProductoRapido');
  const modalRapido = bootstrap.Modal.getOrCreateInstance(modalRapidoEl);
  const codigoRapido = document.getElementById('codigoRapido');
  const nombreRapido = document.getElementById('nombreRapido');
  const precioRapido = document.getElementById('precioRapido');
  const btnAgregarRapido = document.getElementById('btnAgregarRapido');
  const estadoBusquedaRapida = document.getElementById('estadoBusquedaRapida');
  const badgeEncontradoRapido = document.getElementById('badgeEncontradoRapido');
  const badgeNoEncontradoRapido = document.getElementById('badgeNoEncontradoRapido');
  const imagenRapida = document.getElementById('imagenRapida');

  const panelCrossSelling = document.getElementById('panelCrossSelling');
  const textoScriptVenta = document.getElementById('textoScriptVenta');
  const btnAgregarSugerencia = document.getElementById('btnAgregarSugerencia');

  let sugerenciaActual = null;
  let temporizadorBusquedaRapida;
  let controladorBusqueda;

  function agregarProducto(idProducto, codigo, nombre, precio, nombreAlternativo = null, esPromocion = false) {
    agregarProductoAlCarrito(idProducto, codigo, nombre, precio, nombreAlternativo, esPromocion);
    renderizarTabla();
  }

  function renderizarTabla() {
    const miCarrito = obtenerCarrito();
    renderizarTablaUI(miCarrito, listaProductos, asignarEventosFilas);

    if (miCarrito.length === 0) {
      evaluarCrossSelling(null);
    }

    actualizarTotales();
    actualizarListaResumen();
  }

  function asignarEventosFilas() {
    document.querySelectorAll('.btn-menos').forEach(btn => {
      btn.addEventListener('click', function () {
        restarCantidad(this.getAttribute('data-index'));
        renderizarTabla();
      });
    });

    document.querySelectorAll('.btn-mas').forEach(btn => {
      btn.addEventListener('click', function () {
        sumarCantidad(this.getAttribute('data-index'));
        renderizarTabla();
      });
    });

    document.querySelectorAll('.btn-eliminar').forEach(btn => {
      btn.addEventListener('click', function () {
        eliminarProducto(this.getAttribute('data-index'));
        renderizarTabla();
      });
    });
  }

  function actualizarTotales() {
    const miCarrito = obtenerCarrito();
    const subtotal = calcularSubtotal();
    const total = calcularTotalVenta();
    const igv = total - subtotal;

    actualizarTotalesUI(miCarrito.length, subtotal, igv, total, {
      contadorItems,
      subtotalEl,
      igvEl,
      totalEl,
      btnCobrar
    });
  }

  function actualizarListaResumen() {
    const contenedor = document.getElementById('listaResumen');
    if (contenedor) {
      actualizarListaResumenUI(obtenerCarrito(), contenedor);
    }
  }

  function evaluarCrossSelling(producto) {
    if (!producto || !producto.sugerenciaCrossSelling) {
      ocultarCrossSellingUI(panelCrossSelling);
      sugerenciaActual = null;
      return;
    }

    const sugerencia = producto.sugerenciaCrossSelling;
    sugerenciaActual = {
      idProducto: sugerencia.id,
      codigo: sugerencia.codigo,
      nombre: sugerencia.nombre,
      precio: sugerencia.precioPromocional,
      script: `"${sugerencia.script}"`
    };

    mostrarCrossSellingUI(sugerenciaActual.script, {
      textoScriptVenta,
      panelCrossSelling
    });
  }

  async function procesarCodigoSistema(codigo) {
    if (!codigo) return;

    try {
      const producto = await buscarProductoLocal(codigo);
      if (!producto) {
        Toast.fire({ icon: 'error', title: 'Producto no encontrado' });
        inputCodigo.select();
        return;
      }

      agregarProducto(producto.id, codigo, producto.nombre, producto.precioVenta);
      evaluarCrossSelling(producto);
      inputCodigo.value = '';
      inputCodigo.focus();
    } catch (error) {
      console.error('Error en la peticion:', error);
      Swal.fire({
        icon: 'error',
        title: 'Error de conexion',
        text: 'No se pudo conectar con la base de datos.'
      });
    }
  }

  function reiniciarModalRapido() {
    codigoRapido.value = '';
    nombreRapido.value = '';
    precioRapido.value = '';
    nombreRapido.disabled = true;
    precioRapido.disabled = true;
    btnAgregarRapido.disabled = true;
    estadoBusquedaRapida.classList.add('d-none');
    imagenRapida.classList.add('d-none');

    if (controladorBusqueda) controladorBusqueda.abort();
    clearTimeout(temporizadorBusquedaRapida);
    setTimeout(() => codigoRapido.focus(), 200);
  }

  async function procesarValidacionCodigoRapido(signalEntrada = null) {
    const codigo = codigoRapido.value.trim();
    if (!codigo) return;

    estadoBusquedaRapida.classList.remove('d-none');
    badgeEncontradoRapido.classList.add('d-none');
    badgeNoEncontradoRapido.classList.remove('d-none');
    badgeNoEncontradoRapido.textContent = 'Buscando codigo...';

    imagenRapida.classList.add('d-none');
    nombreRapido.value = '';

    if (controladorBusqueda) {
      controladorBusqueda.abort();
    }

    controladorBusqueda = new AbortController();
    const signal = signalEntrada || controladorBusqueda.signal;

    try {
      const prodLocal = await buscarProductoLocal(codigo, signal);

      if (prodLocal) {
        badgeEncontradoRapido.classList.add('d-none');
        badgeNoEncontradoRapido.classList.remove('d-none');
        badgeNoEncontradoRapido.textContent = `El codigo ya existe como: ${prodLocal.nombre}`;

        nombreRapido.value = prodLocal.nombre;
        nombreRapido.disabled = true;
        precioRapido.disabled = true;
        btnAgregarRapido.disabled = true;
        return;
      }

      nombreRapido.disabled = false;
      precioRapido.disabled = false;
      btnAgregarRapido.disabled = false;

      const resultado = await buscarProductoExterno(codigo);
      if (resultado.encontrado) {
        nombreRapido.value = resultado.nombre;
        if (resultado.imagen) {
          imagenRapida.querySelector('img').src = resultado.imagen;
          imagenRapida.classList.remove('d-none');
        }

        badgeEncontradoRapido.classList.remove('d-none');
        badgeNoEncontradoRapido.classList.add('d-none');
        badgeEncontradoRapido.textContent = 'Encontrado en Internet';
        precioRapido.focus();
      } else {
        badgeEncontradoRapido.classList.add('d-none');
        badgeNoEncontradoRapido.classList.remove('d-none');
        badgeNoEncontradoRapido.textContent = 'Codigo libre para ingreso manual';
        nombreRapido.focus();
      }
    } catch (error) {
      if (error.name === 'AbortError') return;

      console.error('Error en la validacion:', error);
      badgeNoEncontradoRapido.textContent = 'Error de conexion';
      estadoBusquedaRapida.classList.remove('d-none');
    }
  }

  function manejarEntradaCodigoRapido() {
    clearTimeout(temporizadorBusquedaRapida);
    const codigoActual = codigoRapido.value.trim();

    if (codigoActual === '') {
      estadoBusquedaRapida.classList.add('d-none');
      imagenRapida.classList.add('d-none');
      nombreRapido.value = '';
      nombreRapido.disabled = true;
      precioRapido.disabled = true;
      btnAgregarRapido.disabled = true;
      if (controladorBusqueda) controladorBusqueda.abort();
      return;
    }

    if (nombreRapido.disabled || estadoBusquedaRapida.classList.contains('d-none')) {
      estadoBusquedaRapida.classList.remove('d-none');
      badgeEncontradoRapido.classList.add('d-none');
      badgeNoEncontradoRapido.classList.remove('d-none');
      badgeNoEncontradoRapido.textContent = 'Buscando codigo...';
    }

    temporizadorBusquedaRapida = setTimeout(() => {
      procesarValidacionCodigoRapido();
    }, 500);
  }

  function agregarProductoRapido() {
    const codigo = codigoRapido.value.trim();
    const nombre = nombreRapido.value.trim().toUpperCase();
    const precio = parseFloat(precioRapido.value);

    if (!codigo || !nombre || Number.isNaN(precio) || precio <= 0) {
      Toast.fire({
        icon: 'error',
        title: 'Complete todos los campos con datos validos'
      });
      return;
    }

    agregarProducto(null, codigo, 'PRODUCTO GENERICO', precio, nombre);
    evaluarCrossSelling(null);
    modalRapido.hide();
    inputCodigo.focus();
  }

  function cancelarVentaActual() {
    if (obtenerCarrito().length === 0) {
      inputCodigo.focus();
      return;
    }

    Swal.fire({
      title: 'Cancelar la venta',
      text: 'Se eliminaran todos los productos escaneados.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#dc3545',
      cancelButtonColor: '#6c757d',
      confirmButtonText: 'Si, cancelar',
      cancelButtonText: 'Volver'
    }).then((result) => {
      if (result.isConfirmed) {
        vaciarCarrito();
        renderizarTabla();
        evaluarCrossSelling(null);
        Toast.fire({ icon: 'info', title: 'Venta cancelada' });
      }

      inputCodigo.focus();
    });
  }

  function aceptarSugerencia() {
    if (!sugerenciaActual) return;

    agregarProducto(
      sugerenciaActual.idProducto,
      sugerenciaActual.codigo,
      sugerenciaActual.nombre,
      sugerenciaActual.precio,
      null,
      true
    );

    panelCrossSelling.classList.add('d-none');
    sugerenciaActual = null;
    inputCodigo.focus();
  }

  inputCodigo.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      procesarCodigoSistema(inputCodigo.value.trim());
    }
  });

  btnAgregarManual.addEventListener('click', () => {
    procesarCodigoSistema(inputCodigo.value.trim());
  });

  modalRapidoEl.addEventListener('show.bs.modal', reiniciarModalRapido);
  codigoRapido.addEventListener('input', manejarEntradaCodigoRapido);
  codigoRapido.addEventListener('keydown', (event) => {
    if (event.key === 'Enter') {
      event.preventDefault();
      clearTimeout(temporizadorBusquedaRapida);
      if (controladorBusqueda) controladorBusqueda.abort();
      procesarValidacionCodigoRapido();
    }
  });

  btnAgregarRapido.addEventListener('click', agregarProductoRapido);
  btnCancelar.addEventListener('click', cancelarVentaActual);
  btnAgregarSugerencia.addEventListener('click', aceptarSugerencia);

  document.addEventListener('keydown', (event) => {
    if (event.key === 'F4' && sugerenciaActual) {
      event.preventDefault();
      aceptarSugerencia();
    }
  });

  inicializarPagos({
    onVentaCompletada: () => {
      vaciarCarrito();
      renderizarTabla();
      evaluarCrossSelling(null);
      inputCodigo.focus();
    },
    onEnfocarInput: () => {
      inputCodigo.focus();
    }
  });

  inputCodigo.focus();
})();

// Fin de la lógica principal de ventas.js.
