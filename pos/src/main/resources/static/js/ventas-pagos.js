/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Pagos
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de ventas pagos en el frontend.
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
  obtenerCarrito,
  calcularSubtotal,
  calcularTotalVenta
} from './ventas-carrito.js';

import {
  sincronizarVentasOfflineApi,
  enviarVenta,
  consultarEstadoPagoApi,
  confirmarPagoQrApi,
  obtenerOpcionesPagoApi
} from './ventas-api.js';

import {
  Toast,
  renderizarTicketImpresionUI
} from './ventas-ui.js';

// Fin de imports y dependencias externas.



/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de ventas-pagos.js.

const OFFLINE_STORAGE_KEY = 'ventas_offline_pendientes';

export function inicializarPagos({ onVentaCompletada, onEnfocarInput }) {
  const btnCobrar = document.getElementById('btnCobrar');
  const modalMetodoPagoEl = document.getElementById('modalMetodoPago');
  const modalMetodoPago = bootstrap.Modal.getOrCreateInstance(modalMetodoPagoEl);
  const totalModalPago = document.getElementById('totalModalPago');
  const avisoMetodosPago = document.getElementById('avisoMetodosPago');
  const botonesMetodoPago = document.querySelectorAll('[data-metodo-pago]');

  const modalQrEl = document.getElementById('modalQR');
  const modalQr = bootstrap.Modal.getOrCreateInstance(modalQrEl);
  const btnConfirmarPagoQr = document.getElementById('btnConfirmarPagoQr');

  const modalConfirmarEfectivoEl = document.getElementById('modalConfirmarEfectivo');
  const modalConfirmarEfectivo = bootstrap.Modal.getOrCreateInstance(modalConfirmarEfectivoEl);
  const montoConfirmacionEfectivo = document.getElementById('montoConfirmacionEfectivo');
  const btnConfirmarEfectivoFinal = document.getElementById('btnConfirmarEfectivoFinal');

  let ventaPendienteId = null;
  let totalPendiente = 0;
  let ticketPendiente = null;
  let intervaloPolling = null;

  function guardarVentaOffline(payload) {
    const pendientes = JSON.parse(localStorage.getItem(OFFLINE_STORAGE_KEY)) || [];
    pendientes.push(payload);
    localStorage.setItem(OFFLINE_STORAGE_KEY, JSON.stringify(pendientes));
    console.info(`Venta guardada offline. Total en cola: ${pendientes.length}`);
  }

  async function sincronizarVentasOffline() {
    const pendientes = JSON.parse(localStorage.getItem(OFFLINE_STORAGE_KEY)) || [];
    if (pendientes.length === 0) return;

    console.info(`Intentando sincronizar ${pendientes.length} venta(s) pendiente(s).`);
    try {
      const resultado = await sincronizarVentasOfflineApi(pendientes);
      localStorage.removeItem(OFFLINE_STORAGE_KEY);
      Toast.fire({
        icon: 'success',
        title: `Sincronizado: ${resultado.procesadasExito || pendientes.length} ticket(s) enviado(s).`
      });
    } catch (error) {
      console.error('No se pudo sincronizar la cola offline:', error);
    }
  }

  async function cargarOpcionesPago() {
    try {
      const data = await obtenerOpcionesPagoApi();
      const metodos = Array.isArray(data.metodos) ? data.metodos : [];

      botonesMetodoPago.forEach(btn => {
        const codigo = btn.dataset.metodoPago;
        const metodo = metodos.find(item => item.codigo === codigo);
        const disponible = metodo ? metodo.disponible === true : codigo === 'MANUAL';
        const habilitado = navigator.onLine || codigo === 'MANUAL';
        const activo = disponible && habilitado;

        btn.disabled = !activo;
        btn.classList.toggle('opacity-50', !activo);
        btn.title = metodo ? metodo.descripcion : '';
      });

      if (avisoMetodosPago) {
        const extra = navigator.onLine ? '' : ' (Modo offline: solo efectivo disponible)';
        avisoMetodosPago.textContent = `Pasarela activa de la empresa: ${data.pasarelaActiva || 'MANUAL'}.${extra}`;
      }
    } catch (error) {
      console.error('Error cargando metodos de pago:', error);
      if (avisoMetodosPago) {
        avisoMetodosPago.textContent = 'No se pudieron cargar las pasarelas. Se permitira solo cobro manual.';
      }

      botonesMetodoPago.forEach(btn => {
        const esManual = btn.dataset.metodoPago === 'MANUAL';
        btn.disabled = !esManual;
        btn.classList.toggle('opacity-50', !esManual);
      });
    }
  }

  function clonarCarritoParaTicket(carritoActual) {
    return carritoActual.map(item => ({
      ...item,
      subtotal: () => Number(item.precio) * Number(item.cantidad)
    }));
  }

  function mostrarBaucherManual(totalVenta) {
    const spanMontoBaucher = document.getElementById('montoBaucher');
    if (spanMontoBaucher) spanMontoBaucher.textContent = totalVenta.toFixed(2);

    const modalBaucherEl = document.getElementById('modalBaucherManual');
    if (modalBaucherEl) {
      bootstrap.Modal.getOrCreateInstance(modalBaucherEl).show();
    }
  }

  function obtenerElementosTicket() {
    return {
      tablaItems: document.getElementById('tabla-ticket-items'),
      tSubtotal: document.getElementById('t-subtotal'),
      tIgv: document.getElementById('t-igv'),
      tTotal: document.getElementById('t-total')
    };
  }

  function finalizarVentaExitosamente(totalVenta, carritoTicket, subtotalTicket, despuesDeImprimir = null) {
    const itemsTicket = carritoTicket || clonarCarritoParaTicket(obtenerCarrito());
    const subtotal = subtotalTicket ?? calcularSubtotal();
    const domElements = obtenerElementosTicket();

    if (!domElements.tablaItems || !domElements.tSubtotal || !domElements.tIgv || !domElements.tTotal) {
      Swal.fire({
        icon: 'error',
        title: 'No se pudo preparar el ticket',
        text: 'Faltan elementos de impresion en la vista de ventas.'
      });
      return;
    }

    renderizarTicketImpresionUI(itemsTicket, subtotal, totalVenta, domElements);

    if (modalQrEl?.classList.contains('show')) {
      modalQr.hide();
    }

    let cierreEjecutado = false;
    let temporizadorRespaldo = null;

    const cerrarVenta = () => {
      if (cierreEjecutado) return;

      cierreEjecutado = true;
      window.removeEventListener('afterprint', cerrarVenta);
      if (temporizadorRespaldo) clearTimeout(temporizadorRespaldo);

      ventaPendienteId = null;
      ticketPendiente = null;

      if (onVentaCompletada) onVentaCompletada();
      if (typeof despuesDeImprimir === 'function') despuesDeImprimir();
      if (onEnfocarInput) onEnfocarInput();
    };

    window.addEventListener('afterprint', cerrarVenta, { once: true });

    setTimeout(() => {
      window.print();
      temporizadorRespaldo = setTimeout(cerrarVenta, 1500);
    }, 300);
  }

  async function confirmarPagoQrPendiente() {
    if (!ventaPendienteId) {
      Toast.fire({ icon: 'error', title: 'No hay una venta pendiente' });
      return;
    }

    try {
      btnConfirmarPagoQr.disabled = true;
      await confirmarPagoQrApi(ventaPendienteId);

      if (intervaloPolling) clearInterval(intervaloPolling);
      Toast.fire({ icon: 'success', title: 'Pago confirmado manualmente' });

      const ticket = ticketPendiente;
      finalizarVentaExitosamente(
        ticket?.total ?? totalPendiente,
        ticket?.carrito,
        ticket?.subtotal
      );
    } catch (error) {
      console.error('Error confirmando pago QR:', error);
      Swal.fire({
        icon: 'error',
        title: 'Error',
        text: error.message || 'No se pudo confirmar el pago.'
      });
    } finally {
      btnConfirmarPagoQr.disabled = false;
    }
  }

  function estadoPagoCompletado(estado) {
    const estadoNormalizado = String(estado || '').trim().toUpperCase();
    return ['PAGADO', 'APROBADO', 'APPROVED', 'COMPLETADA'].includes(estadoNormalizado);
  }

  function prepararPayloadVenta(metodoPago, carritoActual, totalVenta) {
    return {
      metodoPago,
      venta: { total: totalVenta },
      carrito: carritoActual.map(item => {
        const detalle = {
          cantidad: item.cantidad,
          precioUnitario: item.precio,
          nombreAlternativo: item.nombreAlternativo,
          esPromocion: item.esPromocion
        };

        if (item.idProducto !== null) {
          detalle.producto = { id: item.idProducto };
        }

        return detalle;
      })
    };
  }

  function mostrarQrPendiente(data, totalVenta) {
    ventaPendienteId = data.venta ? data.venta.id : null;

    if (!ventaPendienteId) {
      throw new Error('El servidor genero un pago QR, pero no devolvio el ID de la venta.');
    }

    const contenedorQr = document.getElementById('contenedorQrAscii');
    const imgQr = document.getElementById('imagenQrMercadoPago');
    const montoQr = document.getElementById('montoQr');

    if (contenedorQr) {
      contenedorQr.classList.add('d-none');
      contenedorQr.textContent = '';
    }

    if (imgQr) {
      imgQr.classList.add('d-none');
      imgQr.removeAttribute('src');
    }

    if (data.pago_info.qr_image && imgQr) {
      imgQr.src = data.pago_info.qr_image.startsWith('data:image')
        ? data.pago_info.qr_image
        : `data:image/png;base64,${data.pago_info.qr_image}`;
      imgQr.classList.remove('d-none');
    } else if (contenedorQr) {
      contenedorQr.textContent = data.pago_info.qr_data || '';
      contenedorQr.classList.remove('d-none');
    }

    if (montoQr) montoQr.textContent = `S/. ${totalVenta.toFixed(2)}`;
    modalQr.show();
  }

  function iniciarPollingPagoQr() {
    if (intervaloPolling) clearInterval(intervaloPolling);

    intervaloPolling = setInterval(async () => {
      if (!ventaPendienteId) {
        clearInterval(intervaloPolling);
        intervaloPolling = null;
        return;
      }

      try {
        const dataEstado = await consultarEstadoPagoApi(ventaPendienteId);
        if (estadoPagoCompletado(dataEstado.estado)) {
          clearInterval(intervaloPolling);
          intervaloPolling = null;
          Toast.fire({ icon: 'success', title: 'Pago detectado automaticamente' });

          const ticket = ticketPendiente;
          finalizarVentaExitosamente(
            ticket?.total ?? totalPendiente,
            ticket?.carrito,
            ticket?.subtotal
          );
        }
      } catch (error) {
        console.error('Error consultando estado del pago:', error);
      }
    }, 3000);
  }

  async function procesarCobro(metodoPago = 'MANUAL') {
    const carritoActual = obtenerCarrito();
    const subtotalVenta = calcularSubtotal();
    const totalVenta = calcularTotalVenta();
    const carritoTicket = clonarCarritoParaTicket(carritoActual);
    const ventaPayload = prepararPayloadVenta(metodoPago, carritoActual, totalVenta);

    ticketPendiente = {
      carrito: carritoTicket,
      subtotal: subtotalVenta,
      total: totalVenta
    };

    if (!navigator.onLine) {
      guardarVentaOffline(ventaPayload);
      Swal.fire({
        icon: 'info',
        title: 'Venta guardada offline',
        text: 'Estas sin conexion. El ticket se guardo en la caja y se enviara automaticamente cuando regrese el internet.',
        confirmButtonColor: '#0d6efd',
        confirmButtonText: 'Entendido'
      }).then(() => {
        if (onVentaCompletada) onVentaCompletada();
      });
      return;
    }

    try {
      const data = await enviarVenta(ventaPayload);
      const tieneQr = data.pago_info && (data.pago_info.qr_data || data.pago_info.qr_image);

      if (tieneQr) {
        mostrarQrPendiente(data, totalVenta);
        iniciarPollingPagoQr();
        return;
      }

      finalizarVentaExitosamente(totalVenta, carritoTicket, subtotalVenta, () => {
        mostrarBaucherManual(totalVenta);
      });
    } catch (error) {
      console.error('Error al enviar la venta:', error);
      if (error.message.includes('Failed to fetch') || !navigator.onLine) {
        guardarVentaOffline(ventaPayload);
        Swal.fire({
          icon: 'info',
          title: 'Venta guardada offline',
          text: 'Se perdio la conexion al enviar. La venta se guardo localmente y se sincronizara luego.',
          confirmButtonColor: '#0d6efd',
          confirmButtonText: 'Entendido'
        }).then(() => {
          if (onVentaCompletada) onVentaCompletada();
        });
      } else {
        Swal.fire({
          icon: 'error',
          title: 'Error al procesar el cobro',
          text: error.message || 'Revisa la consola para mas detalles.'
        });
      }
    } finally {
      if (onEnfocarInput) onEnfocarInput();
    }
  }

  btnCobrar?.addEventListener('click', async () => {
    if (obtenerCarrito().length === 0) return;

    totalPendiente = calcularTotalVenta();
    if (totalModalPago) totalModalPago.textContent = `S/. ${totalPendiente.toFixed(2)}`;

    await cargarOpcionesPago();
    modalMetodoPago.show();
  });

  botonesMetodoPago.forEach(btn => {
    btn.addEventListener('click', () => {
      if (btn.disabled) return;

      modalMetodoPago.hide();
      const metodo = btn.dataset.metodoPago;

      if (metodo === 'MANUAL') {
        if (montoConfirmacionEfectivo) montoConfirmacionEfectivo.textContent = totalPendiente.toFixed(2);
        modalConfirmarEfectivo.show();
        return;
      }

      procesarCobro(metodo);
    });
  });

  btnConfirmarEfectivoFinal?.addEventListener('click', () => {
    modalConfirmarEfectivo.hide();
    procesarCobro('MANUAL');
  });

  btnConfirmarPagoQr?.addEventListener('click', confirmarPagoQrPendiente);

  modalQrEl?.addEventListener('hidden.bs.modal', () => {
    if (intervaloPolling) {
      clearInterval(intervaloPolling);
      intervaloPolling = null;
    }
  });

  window.addEventListener('online', () => {
    Toast.fire({ icon: 'success', title: 'Conexion a Internet restaurada' });
    sincronizarVentasOffline();
    cargarOpcionesPago();
  });

  window.addEventListener('offline', () => {
    Toast.fire({ icon: 'warning', title: 'Sin conexion. Modo offline activado', timer: 5000 });
    cargarOpcionesPago();
  });

  if (navigator.onLine) {
    sincronizarVentasOffline();
  }
}

// Fin de la lógica principal de ventas-pagos.js.
