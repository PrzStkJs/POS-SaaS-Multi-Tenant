/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de ventas api en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de ventas-api.js.

const API_VENTAS = '/api/ventas';

async function leerErrorHttp(response, mensajeBase) {
  const contentType = response.headers.get('content-type') || '';

  try {
    if (contentType.includes('application/json')) {
      const data = await response.json();
      const detalle = data.mensaje || data.message || data.error || JSON.stringify(data);
      return `${mensajeBase}: ${detalle}`;
    }

    const texto = await response.text();
    return texto ? `${mensajeBase}: ${texto}` : mensajeBase;
  } catch (error) {
    return mensajeBase;
  }
}

async function validarRespuesta(response, mensajeError) {
  if (response.ok) {
    if (response.status === 204) return null;
    return await response.json();
  }

  throw new Error(await leerErrorHttp(response, mensajeError));
}

export async function buscarProductoLocal(codigo, signal = null) {
  const opciones = signal ? { signal } : {};
  const response = await fetch(`${API_VENTAS}/buscar-producto?codigo=${encodeURIComponent(codigo)}`, opciones);

  if (response.status === 404) {
    return null;
  }

  return await validarRespuesta(response, 'Error al buscar el producto en el servidor');
}

export async function buscarProductoExterno(codigo) {
  try {
    const response = await fetch(`https://world.openfoodfacts.org/api/v2/product/${codigo}.json`);
    if (!response.ok) return { encontrado: false };

    const data = await response.json();
    if (data.status === 1 && data.product && data.product.product_name) {
      return {
        encontrado: true,
        nombre: data.product.product_name,
        imagen: data.product.image_url || null
      };
    }
  } catch (error) {
    console.warn('No se pudo consultar Open Food Facts:', error);
  }

  return { encontrado: false };
}

export async function obtenerOpcionesPagoApi() {
  const response = await fetch(`${API_VENTAS}/opciones-pago`);
  return await validarRespuesta(response, 'Error al obtener las opciones de pago');
}

export async function enviarVenta(payload) {
  const response = await fetch(`${API_VENTAS}/procesar`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });

  return await validarRespuesta(response, 'Error al enviar la venta al servidor');
}

export async function confirmarPagoQrApi(ventaId) {
  const response = await fetch(`${API_VENTAS}/${ventaId}/confirmar-pago`, {
    method: 'POST'
  });

  return await validarRespuesta(response, 'Error al confirmar el pago QR');
}

export async function consultarEstadoPagoApi(ventaId) {
  const response = await fetch(`${API_VENTAS}/${ventaId}/estado`);
  return await validarRespuesta(response, 'Error al consultar el estado del pago');
}

export async function sincronizarVentasOfflineApi(pendientes) {
  const response = await fetch(`${API_VENTAS}/sincronizar-offline`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(pendientes)
  });

  return await validarRespuesta(response, 'Error al sincronizar las ventas offline');
}

// Fin de la lógica principal de ventas-api.js.
