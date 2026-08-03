/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de ventas ui en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de ventas-ui.js.

export const Toast = Swal.mixin({
  toast: true,
  position: 'top-end',
  showConfirmButton: false,
  timer: 3000,
  timerProgressBar: true,
  didOpen: (toast) => {
    toast.onmouseenter = Swal.stopTimer;
    toast.onmouseleave = Swal.resumeTimer;
  }
});

function escaparHtml(valor) {
  return String(valor ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

function obtenerSubtotalItem(item) {
  if (typeof item.subtotal === 'function') {
    return Number(item.subtotal());
  }

  return Number(item.precio || 0) * Number(item.cantidad || 0);
}

export function renderizarTablaUI(miCarrito, listaProductos, onRenderCompleto) {
  listaProductos.innerHTML = '';

  if (miCarrito.length === 0) {
    listaProductos.innerHTML = `
      <tr id="filaVacia">
        <td colspan="6" class="text-center text-muted py-4">
          <i class="bi bi-inbox display-5 d-block mb-2"></i>
          Escanea o busca un producto
        </td>
      </tr>`;
    return;
  }

  const fragment = document.createDocumentFragment();

  miCarrito.forEach((item, index) => {
    const nombreBase = escaparHtml(item.nombreAlternativo || item.nombre);
    const codigo = escaparHtml(item.codigo);
    const etiquetaRapida = item.nombreAlternativo
      ? ' <span class="badge bg-warning text-dark text-xs">Rapido</span>'
      : '';
    const etiquetaPromocion = item.esPromocion
      ? ' <span class="badge bg-info text-dark text-xs ms-2">Promo</span>'
      : '';

    const fila = document.createElement('tr');
    fila.classList.add('fila-nueva');
    fila.innerHTML = `
      <td>${codigo}</td>
      <td>${nombreBase}${etiquetaRapida}${etiquetaPromocion}</td>
      <td class="text-end">S/. ${Number(item.precio).toFixed(2)}</td>
      <td class="text-center">
        <div class="d-flex justify-content-center align-items-center gap-1">
          <button class="btn btn-sm btn-outline-secondary btn-menos" data-index="${index}">-</button>
          <span class="fw-bold">${item.cantidad}</span>
          <button class="btn btn-sm btn-outline-secondary btn-mas" data-index="${index}">+</button>
        </div>
      </td>
      <td class="text-end fw-semibold">S/. ${obtenerSubtotalItem(item).toFixed(2)}</td>
      <td class="text-center">
        <button class="btn btn-sm btn-outline-danger btn-eliminar" data-index="${index}">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    `;
    fragment.appendChild(fila);
  });

  listaProductos.appendChild(fragment);

  if (typeof onRenderCompleto === 'function') {
    onRenderCompleto();
  }
}

export function actualizarTotalesUI(cantidadItems, subtotal, igv, total, domElements) {
  const { contadorItems, subtotalEl, igvEl, totalEl, btnCobrar } = domElements;

  contadorItems.textContent = cantidadItems;
  subtotalEl.textContent = `S/. ${subtotal.toFixed(2)}`;
  igvEl.textContent = `S/. ${igv.toFixed(2)}`;
  totalEl.textContent = `S/. ${total.toFixed(2)}`;

  totalEl.classList.add('actualizado');
  setTimeout(() => totalEl.classList.remove('actualizado'), 300);

  btnCobrar.disabled = cantidadItems === 0;
}

export function actualizarListaResumenUI(miCarrito, contenedor) {
  if (!contenedor) return;

  if (miCarrito.length === 0) {
    contenedor.innerHTML = '<p class="text-muted text-center mb-0">Sin productos</p>';
    return;
  }

  let html = '<table class="table table-sm table-borderless mb-0"><tbody>';

  miCarrito.forEach(item => {
    const nombre = item.esPromocion
      ? `Promo - ${item.nombreAlternativo || item.nombre}`
      : item.nombreAlternativo || item.nombre;
    const nombreSeguro = escaparHtml(nombre);

    html += `
      <tr>
        <td class="text-truncate" style="max-width: 130px;" title="${nombreSeguro}">${nombreSeguro}</td>
        <td class="text-center" style="width: 40px; min-width: 40px;">x${item.cantidad}</td>
        <td class="text-end fw-semibold" style="width: 80px; min-width: 80px;">S/. ${obtenerSubtotalItem(item).toFixed(2)}</td>
      </tr>
    `;
  });

  html += '</tbody></table>';
  contenedor.innerHTML = html;
}

export function renderizarTicketImpresionUI(miCarrito, subtotal, totalVenta, domElements) {
  const { tablaItems, tSubtotal, tIgv, tTotal } = domElements;
  tablaItems.innerHTML = '';

  const fragment = document.createDocumentFragment();

  miCarrito.forEach(item => {
    const tr = document.createElement('tr');
    const nombreTicket = escaparHtml(item.nombreAlternativo || item.nombre);

    tr.innerHTML = `
      <td style="text-align: left; padding-bottom: 4px;">${nombreTicket}</td>
      <td style="text-align: center; width: 30px;">x${item.cantidad}</td>
      <td style="text-align: right;">S/. ${obtenerSubtotalItem(item).toFixed(2)}</td>
    `;
    fragment.appendChild(tr);
  });

  tablaItems.appendChild(fragment);
  tSubtotal.textContent = `S/. ${subtotal.toFixed(2)}`;
  tIgv.textContent = `S/. ${(subtotal * 0.18).toFixed(2)}`;
  tTotal.textContent = `S/. ${totalVenta.toFixed(2)}`;
}

export function mostrarCrossSellingUI(script, domElements) {
  const { textoScriptVenta, panelCrossSelling } = domElements;

  textoScriptVenta.textContent = script;
  panelCrossSelling.classList.remove('d-none');
  panelCrossSelling.classList.add('border', 'border-warning', 'border-2');

  setTimeout(() => panelCrossSelling.classList.remove('border-2'), 800);
}

export function ocultarCrossSellingUI(panelCrossSelling) {
  panelCrossSelling.classList.add('d-none');
}

// Fin de la lógica principal de ventas-ui.js.
