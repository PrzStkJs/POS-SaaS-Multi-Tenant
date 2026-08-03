/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Módulo JavaScript encargado de manejar la interacción, estado y comunicación
 *              de ventas carrito en el frontend.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  LÓGICA PRINCIPAL
 * ============================================================ */

// Inicio de la lógica principal de ventas-carrito.js.

const TASA_IGV = 0.18;

export class ItemCarrito {
  constructor(idProducto, codigo, nombre, precio, cantidad = 1, nombreAlternativo = null, esPromocion = false) {
    this.idProducto = idProducto;
    this.codigo = codigo;
    this.nombre = nombre;
    this.precio = Number(precio);
    this.cantidad = Number(cantidad);
    this.nombreAlternativo = nombreAlternativo;
    this.esPromocion = esPromocion;
  }

  subtotal() {
    return this.precio * this.cantidad;
  }
}

let carrito = [];

export function agregarProductoAlCarrito(idProducto, codigo, nombre, precio, nombreAlternativo = null, esPromocion = false) {
  const existente = carrito.find(item => item.codigo === codigo && item.esPromocion === esPromocion);

  if (existente) {
    existente.cantidad++;
    return carrito;
  }

  carrito.push(new ItemCarrito(idProducto, codigo, nombre, precio, 1, nombreAlternativo, esPromocion));
  return carrito;
}

export function sumarCantidad(index) {
  if (carrito[index]) {
    carrito[index].cantidad++;
  }

  return carrito;
}

export function restarCantidad(index) {
  if (!carrito[index]) {
    return carrito;
  }

  if (carrito[index].cantidad > 1) {
    carrito[index].cantidad--;
  } else {
    eliminarProducto(index);
  }

  return carrito;
}

export function eliminarProducto(index) {
  carrito.splice(index, 1);
  return carrito;
}

export function vaciarCarrito() {
  carrito = [];
  return carrito;
}

export function calcularSubtotal() {
  return carrito.reduce((sum, item) => sum + item.subtotal(), 0);
}

export function calcularIGV() {
  return calcularSubtotal() * TASA_IGV;
}

export function calcularTotalVenta() {
  const subtotal = calcularSubtotal();
  return Number((subtotal * (1 + TASA_IGV)).toFixed(2));
}

export function obtenerCarrito() {
  return [...carrito];
}

// Fin de la lógica principal de ventas-carrito.js.
