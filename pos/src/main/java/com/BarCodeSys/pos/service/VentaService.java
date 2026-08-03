/**
 * PROYECTO: Sistema POS BarCodeSys - Módulo de Ventas
 * DESCRIPCIÓN: Servicio de negocio responsable de ejecutar reglas, validaciones y
 *              operaciones de venta.
 *
 * @author Jesús Alexander Perez Huacaychuco
 * @version 1.0.0
 * @date 03/08/2026
 */

/* ============================================================
 *                  PAQUETE E IMPORTACIONES
 * ============================================================ */

// Inicio de paquete e importaciones del archivo.

package com.BarCodeSys.pos.service;

import com.BarCodeSys.pos.dto.FormularioVentaDTO;
import com.BarCodeSys.pos.dto.MovimientoRequestDTO;
import com.BarCodeSys.pos.dto.VentaHistorialDTO;
import com.BarCodeSys.pos.exceptions.RecursoNoEncontradoException;
import com.BarCodeSys.pos.models.DetalleVenta;
import com.BarCodeSys.pos.models.Empresa;
import com.BarCodeSys.pos.models.EstadoVenta;
import com.BarCodeSys.pos.models.Producto;
import com.BarCodeSys.pos.models.QUsuario;
import com.BarCodeSys.pos.models.QVenta;
import com.BarCodeSys.pos.models.Usuario;
import com.BarCodeSys.pos.models.Venta;
import com.BarCodeSys.pos.repository.DetalleVentaRepository;
import com.BarCodeSys.pos.repository.EmpresaRepository;
import com.BarCodeSys.pos.repository.ProductoRepository;
import com.BarCodeSys.pos.repository.UsuarioRepository;
import com.BarCodeSys.pos.repository.VentaRepository;
import com.BarCodeSys.pos.security.SecurityUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Fin de paquete e importaciones del archivo.



/* ============================================================
 *                  DEFINICIÓN PRINCIPAL
 * ============================================================ */

// Inicio de la definición principal de VentaService.

@Service
@Transactional(readOnly = true) // Optimiza rendimiento global para consultas
public class VentaService {

    private static final Logger log = LoggerFactory.getLogger(VentaService.class);
    private static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    // Inyección por Constructor (Inmutables y seguras)
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final ProductoRepository productoRepository;
    private final InventarioService inventarioService;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasarelaPagoFactory pasarelaFactory;
    private final EntityManager entityManager;

    public VentaService(VentaRepository ventaRepository,
                        DetalleVentaRepository detalleVentaRepository,
                        ProductoRepository productoRepository,
                        InventarioService inventarioService,
                        EmpresaRepository empresaRepository,
                        UsuarioRepository usuarioRepository,
                        PasarelaPagoFactory pasarelaFactory,
                        EntityManager entityManager) {
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.productoRepository = productoRepository;
        this.inventarioService = inventarioService;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.pasarelaFactory = pasarelaFactory;
        this.entityManager = entityManager;
    }

    public Venta buscarPorId(Long id) {
        // 🔒 BLINDAJE: Forzamos la búsqueda usando el contexto de seguridad
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        return ventaRepository.findByIdAndEmpresaId(id, idEmpresa)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada o acceso denegado para el ID: " + id));
    }
    public List<DetalleVenta> obtenerDetallesSeguros(Long ventaId) {
        // 1. Reutilizamos tu método blindado que exige que la venta sea de la empresa actual
        Venta venta = buscarPorId(ventaId);
        
        // 2. Si pasa la validación anterior, es seguro devolver los detalles
        return detalleVentaRepository.findByVentaId(venta.getId());
    }

    @Transactional
    public Map<String, Object> procesarVenta(Venta venta, List<DetalleVenta> carrito) {
        return procesarVenta(venta, carrito, null);
    }

    @Transactional
    public Map<String, Object> procesarVenta(
            Venta venta,
            List<DetalleVenta> carrito,
            String metodoPagoSolicitado) {

        validarSolicitudVenta(venta, carrito);

        // 🔒 BLINDAJE EXTREMO: Forzamos la empresa y usuario sacándolos del Token.
        Long idEmpresa = SecurityUtils.getCurrentEmpresaId();
        Long idUsuario = SecurityUtils.getCurrentUsuarioId();
        
        Empresa empresaActual = empresaRepository.findById(idEmpresa)
                .orElseThrow(() -> new RecursoNoEncontradoException("Empresa no encontrada"));
        Usuario usuarioActual = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
                
        venta.setEmpresa(empresaActual);
        venta.setUsuario(usuarioActual);
        venta.setFechaHora(LocalDateTime.now());
        venta.setTotal(BigDecimal.ZERO);

        Venta ventaGuardada = ventaRepository.save(venta);
        BigDecimal subtotalCalculado = registrarDetallesVenta(ventaGuardada, carrito);
        BigDecimal totalConIgv = calcularTotalConIgv(subtotalCalculado);

        ventaGuardada.setTotal(totalConIgv);

        String nombrePasarela = resolverPasarelaParaVenta(ventaGuardada.getEmpresa(), metodoPagoSolicitado);
        ventaGuardada.setPasarelaUsada(nombrePasarela);
        ventaGuardada.setMetodoPago(resolverMetodoPagoRegistrado(nombrePasarela));

        PasarelaPagoService servicioPago = pasarelaFactory.obtenerPasarela(nombrePasarela);
        Map<String, Object> respuestaPago = servicioPago.procesarPago(ventaGuardada, obtenerTokenPasarela(ventaGuardada, nombrePasarela));
        String estadoPago = (String) respuestaPago.get("estado");

        if ("ERROR".equals(estadoPago)) {
            String mensajeError = (String) respuestaPago.get("mensaje");
            throw new RuntimeException("Fallo en la pasarela de pago: " + mensajeError);
        }

        if ("PENDIENTE".equals(estadoPago)) {
            ventaGuardada.setEstado(EstadoVenta.PENDIENTE_PAGO);
            log.info("Venta pendiente de pago. No se descuenta stock. Venta ID: {}", ventaGuardada.getId());
        } else {
            ventaGuardada.setEstado(EstadoVenta.COMPLETADA);
            log.info("Venta completada. Descontando stock. Venta ID: {}", ventaGuardada.getId());
            aplicarDescuentoStock(ventaGuardada, carrito);
        }

        ventaRepository.save(ventaGuardada);

        Map<String, Object> respuestaFinal = new HashMap<>();
        respuestaFinal.put("venta", ventaGuardada);
        respuestaFinal.put("pago_info", respuestaPago);
        return respuestaFinal;
    }

    @Transactional
    public Map<String, Object> procesarSincronizacionMasiva(List<FormularioVentaDTO> ventasOffline) {
        int procesadasExito = 0;
        int procesadasError = 0;

        for (FormularioVentaDTO request : ventasOffline) {
            try {
                procesarVenta(request.getVenta(), request.getCarrito(), "MANUAL");
                procesadasExito++;
            } catch (Exception e) {
                log.error("Error al sincronizar una venta offline: {}", e.getMessage(), e);
                procesadasError++;
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("mensaje", "Sincronizacion masiva ejecutada");
        resultado.put("totalRecibidas", ventasOffline.size());
        resultado.put("procesadasExito", procesadasExito);
        resultado.put("errores", procesadasError);
        return resultado;
    }

    @Transactional
    public void confirmarVentaDesdeWebhook(Long ventaId) {
        // Nota: Mantenemos findById clásico porque el Webhook no trae token de usuario/empresa.
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Venta no encontrada"));

        if (venta.getEstado() == EstadoVenta.COMPLETADA) {
            log.warn("La venta #{} ya estaba pagada. Se ignora el webhook.", ventaId);
            return;
        }

        if (venta.getEstado() == EstadoVenta.PENDIENTE_PAGO) {
            venta.setEstado(EstadoVenta.COMPLETADA);
            ventaRepository.save(venta);

            List<DetalleVenta> carrito = detalleVentaRepository.findByVentaId(venta.getId());
            aplicarDescuentoStock(venta, carrito);

            log.info("Pago confirmado por webhook. Venta #{} completada y stock descontado.", ventaId);
        }
    }

    @Transactional
    public Venta confirmarVentaManual(Long ventaId) {
        // 🔒 BLINDAJE: Reutilizamos buscarPorId que ya tiene el filtro de Empresa por Token
        Venta venta = buscarPorId(ventaId);

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new RuntimeException("No se puede confirmar una venta anulada.");
        }

        if (venta.getEstado() == EstadoVenta.COMPLETADA) {
            return venta;
        }

        venta.setEstado(EstadoVenta.COMPLETADA);
        ventaRepository.save(venta);

        List<DetalleVenta> carrito = detalleVentaRepository.findByVentaId(venta.getId());
        aplicarDescuentoStock(venta, carrito);
        return venta;
    }

    @Transactional
    public void aplicarDescuentoStock(Venta venta, List<DetalleVenta> carrito) {
        Long idEmpresa = venta.getEmpresa().getId();
        Long idUsuario = venta.getUsuario() != null ? venta.getUsuario().getId() : null;

        for (DetalleVenta detalle : carrito) {
            if (esProductoRapido(detalle)) {
                continue;
            }

            MovimientoRequestDTO movimiento = new MovimientoRequestDTO();
            movimiento.setProductoId(detalle.getProducto().getId());
            movimiento.setTipo("SALIDA");
            movimiento.setCantidad(detalle.getCantidad());
            movimiento.setMotivo("VENTA");
            movimiento.setDescripcion("Ticket/Venta #" + venta.getId());

            inventarioService.registrarMovimientoAutomatizado(movimiento, idEmpresa, idUsuario);
        }
    }

    @Transactional
    public Venta anularVenta(Long ventaId) {
        // 🔒 BLINDAJE: Reutilizamos buscarPorId que ya tiene el filtro de Empresa por Token
        Venta venta = buscarPorId(ventaId);

        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new RuntimeException("La venta ya se encuentra anulada.");
        }

        if (venta.getEstado() == EstadoVenta.COMPLETADA) {
            List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(venta.getId());
            devolverStockDeVentaAnulada(venta, detalles);
        }

        venta.setEstado(EstadoVenta.ANULADA);
        return ventaRepository.save(venta);
    }

    public List<VentaHistorialDTO> buscarHistorial(
            Long ticket,
            Long cajeroId,
            String estado,
            LocalDateTime inicio,
            LocalDateTime fin) {

        Long empresaId = SecurityUtils.getCurrentEmpresaId();
        
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        QVenta qVenta = QVenta.venta;
        QUsuario qUsuario = QUsuario.usuario;

        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qVenta.empresa.id.eq(empresaId));
        builder.and(qVenta.fechaHora.between(inicio, fin));

        if (ticket != null) builder.and(qVenta.id.eq(ticket));
        if (cajeroId != null) builder.and(qVenta.usuario.id.eq(cajeroId));

        if (estado != null && !estado.trim().isEmpty()) {
            builder.and(qVenta.estado.stringValue().equalsIgnoreCase(estado));
        }

        return queryFactory
                .select(Projections.constructor(VentaHistorialDTO.class,
                        qVenta.id,
                        qVenta.fechaHora,
                        qVenta.total,
                        qUsuario.username,
                        qVenta.estado.stringValue()
                ))
                .from(qVenta)
                .join(qVenta.usuario, qUsuario)
                .where(builder)
                .orderBy(qVenta.fechaHora.desc())
                .fetch();
    }

    private BigDecimal registrarDetallesVenta(Venta ventaGuardada, List<DetalleVenta> carrito) {
        BigDecimal subtotalCalculado = BigDecimal.ZERO;

        for (DetalleVenta detalle : carrito) {
            validarDetalleVenta(detalle);

            Producto productoDB = resolverProductoDetalle(detalle, ventaGuardada.getEmpresa());
            BigDecimal precio = resolverPrecioDetalle(detalle, productoDB);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(detalle.getCantidad())).setScale(2, RoundingMode.HALF_UP);

            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(subtotal);
            detalle.setVenta(ventaGuardada);
            detalle.setProducto(productoDB);

            detalleVentaRepository.save(detalle);
            subtotalCalculado = subtotalCalculado.add(subtotal);
        }

        return subtotalCalculado;
    }

    private Producto resolverProductoDetalle(DetalleVenta detalle, Empresa empresa) {
        if (esProductoRapido(detalle)) {
            return productoRepository
                    .findByCodigoBarrasAndEmpresaId("GENERICO", empresa.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el producto genérico para la empresa."));
        }

        if (detalle.getProducto() == null || detalle.getProducto().getId() == null) {
            throw new RuntimeException("El detalle de venta no tiene producto asociado.");
        }

        // 🔒 BLINDAJE: Verificamos que el producto exista Y pertenezca a la empresa actual
        return productoRepository.findByIdAndEmpresaId(detalle.getProducto().getId(), empresa.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado o no pertenece a tu empresa."));
    }

    private BigDecimal resolverPrecioDetalle(DetalleVenta detalle, Producto productoDB) {
        if (esProductoRapido(detalle) || Boolean.TRUE.equals(detalle.getEsPromocion())) {
            return validarMonto(detalle.getPrecioUnitario(), "El precio unitario debe ser mayor a cero.");
        }

        return validarMonto(productoDB.getPrecioVenta(), "El producto no tiene precio de venta válido.");
    }

    private String resolverPasarelaParaVenta(Empresa empresa, String metodoPagoSolicitado) {
        String pasarelaActiva = normalizarPasarela(empresa.getPasarelaActiva());
        String metodo = normalizarPasarela(metodoPagoSolicitado);

        if (metodo == null) {
            metodo = pasarelaActiva;
        }

        if (metodo == null || "MANUAL".equals(metodo)) {
            return "MANUAL";
        }

        if ("NIUBIZ".equals(metodo)) {
            if (!"NIUBIZ".equals(pasarelaActiva)) {
                throw new RuntimeException("La empresa no tiene Niubiz como pasarela activa.");
            }
            if (!tieneTexto(empresa.getNiubizMerchantId())
                    || !tieneTexto(empresa.getNiubizApiUser())
                    || !tieneTexto(empresa.getNiubizApiPassword())) {
                throw new RuntimeException("La empresa no tiene completas las credenciales de Niubiz.");
            }
            return "NIUBIZ";
        }

        if ("MERCADOPAGO".equals(metodo)) {
            if (!"MERCADOPAGO".equals(pasarelaActiva)) {
                throw new RuntimeException("La empresa no tiene Mercado Pago como pasarela activa.");
            }
            if (!tieneTexto(empresa.getMpAccessToken())) {
                throw new RuntimeException("La empresa no tiene configurado el Access Token de Mercado Pago.");
            }
            return "MERCADOPAGO";
        }

        return "MANUAL";
    }

    private String resolverMetodoPagoRegistrado(String pasarela) {
        if ("NIUBIZ".equals(pasarela)) {
            return "YAPE_QR_NIUBIZ";
        }
        if ("MERCADOPAGO".equals(pasarela)) {
            return "QR_MERCADOPAGO";
        }
        return "MANUAL";
    }

    private String obtenerTokenPasarela(Venta venta, String pasarela) {
        if ("MERCADOPAGO".equalsIgnoreCase(pasarela)) {
            return venta.getEmpresa().getMpAccessToken();
        }
        if ("NIUBIZ".equalsIgnoreCase(pasarela)) {
            return venta.getEmpresa().getNiubizMerchantId();
        }
        return "";
    }

    @Transactional
    private void devolverStockDeVentaAnulada(Venta venta, List<DetalleVenta> detalles) {
        Long idEmpresa = venta.getEmpresa().getId();
        Long idUsuario = venta.getUsuario() != null ? venta.getUsuario().getId() : null;

        for (DetalleVenta detalle : detalles) {
            if (esProductoRapido(detalle)) {
                continue;
            }

            MovimientoRequestDTO movimiento = new MovimientoRequestDTO();
            movimiento.setProductoId(detalle.getProducto().getId());
            movimiento.setTipo("ENTRADA");
            movimiento.setCantidad(detalle.getCantidad());
            movimiento.setMotivo("DEVOLUCION");
            movimiento.setDescripcion("Anulación de Ticket/Venta #" + venta.getId());

            inventarioService.registrarMovimientoAutomatizado(movimiento, idEmpresa, idUsuario);
        }
    }

    private BigDecimal calcularTotalConIgv(BigDecimal subtotal) {
        return subtotal.multiply(BigDecimal.ONE.add(TASA_IGV)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal validarMonto(BigDecimal monto, String mensajeError) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException(mensajeError);
        }
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    private void validarSolicitudVenta(Venta venta, List<DetalleVenta> carrito) {
        if (venta == null) {
            throw new RuntimeException("La solicitud no incluye la venta.");
        }
        if (carrito == null || carrito.isEmpty()) {
            throw new RuntimeException("El carrito no puede estar vacío.");
        }
    }

    private void validarDetalleVenta(DetalleVenta detalle) {
        if (detalle == null) {
            throw new RuntimeException("El carrito contiene un detalle vacío.");
        }
        if (detalle.getCantidad() <= 0) {
            throw new RuntimeException("La cantidad de cada producto debe ser mayor a cero.");
        }
    }

    private boolean esProductoRapido(DetalleVenta detalle) {
        return detalle.getNombreAlternativo() != null && !detalle.getNombreAlternativo().trim().isEmpty();
    }

    private String normalizarPasarela(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }

        String normalizado = valor.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        if ("CASH".equals(normalizado) || "EFECTIVO".equals(normalizado)) {
            return "MANUAL";
        }
        if ("YAPE".equals(normalizado)
                || "PLIN".equals(normalizado)
                || "QR_NIUBIZ".equals(normalizado)
                || "YAPE_QR_NIUBIZ".equals(normalizado)) {
            return "NIUBIZ";
        }
        if ("MP".equals(normalizado)
                || "MERCADO_PAGO".equals(normalizado)
                || "QR_MERCADOPAGO".equals(normalizado)) {
            return "MERCADOPAGO";
        }

        return normalizado;
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
}

// Fin de la definición principal de VentaService.
