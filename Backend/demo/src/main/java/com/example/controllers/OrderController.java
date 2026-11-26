package com.example.controllers;

import com.example.services.OrderService;
import com.example.services.PromotionService;
import com.example.services.PaymentService;
import com.example.services.CheckoutService;
import com.example.services.EmailService;
import com.example.services.InventoryService;
import com.example.services.ProductLossService;
import com.example.services.StockAlertNotificationService;
import com.example.services.StoreSettingsService;
import com.example.dtos.OrderDTO;
import com.example.dtos.OrderCompleteDTO;
import com.example.dtos.OrderEmailDispatchResultDTO;
import com.example.dtos.CheckoutRequestDTO;
import com.example.models.Order;
import com.example.models.Payment;
import com.example.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private static final String MSG_KEY = "message";
    private static final String STATUS_KEY = "status";
    private static final String ITEM_TYPE_EXTRA = "EXTRA";
    private static final String FIELD_EXTRA_ID = "extraId";
    private static final String FIELD_EXTRA_NAME = "extra_name";
    private static final String FIELD_PIZZA_ID = "pizzaId";
    private static final String FIELD_SIZE_ID = "sizeId";

    private final OrderService orderService;
    private final CheckoutService checkoutService;
    private final PromotionService promotionService;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityUtils securityUtils;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final ProductLossService productLossService;
    private final EmailService emailService;
    private final StockAlertNotificationService stockAlertNotificationService;
    private final StoreSettingsService storeSettingsService;

    public OrderController(OrderService orderService,
                           CheckoutService checkoutService,
                           PromotionService promotionService,
                           JdbcTemplate jdbcTemplate,
                           SecurityUtils securityUtils,
                           PaymentService paymentService,
                           InventoryService inventoryService,
                           ProductLossService productLossService,
                           EmailService emailService,
                           StockAlertNotificationService stockAlertNotificationService,
                           @org.springframework.beans.factory.annotation.Autowired(required = false) StoreSettingsService storeSettingsService) {
        this.orderService = orderService;
        this.checkoutService = checkoutService;
        this.promotionService = promotionService;
        this.jdbcTemplate = jdbcTemplate;
        this.securityUtils = securityUtils;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
        this.productLossService = productLossService;
        this.emailService = emailService;
        this.stockAlertNotificationService = stockAlertNotificationService;
        this.storeSettingsService = storeSettingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDTO>>> listarOrders() {
        List<Order> orders;

        if (securityUtils.isAdmin()) {
            orders = orderService.listarOrders();
        } else {
            Integer userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            orders = orderService.obtenerPorUserId(userId);
        }

        List<OrderDTO> ordersDTO = orders.stream()
            .map(OrderDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(ordersDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerOrderPorId(@PathVariable("id") Integer id) {
        Optional<Order> orderOpt = orderService.obtenerPorId(id);

        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, "Order no encontrado"));
        }

        Order order = orderOpt.get();

        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !order.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MSG_KEY, "No tienes permiso para ver este pedido"));
            }
        }

        OrderDTO orderDTO = new OrderDTO(order);
        return ResponseEntity.ok(ApiResponse.success(orderDTO));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Object> obtenerOrdersPorUserId(@PathVariable("userId") Integer userId) {
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MSG_KEY, "No tienes permiso para ver estos pedidos"));
            }
        }

        List<Order> orders = orderService.obtenerPorUserId(userId);
        List<OrderDTO> ordersDTO = orders.stream()
            .map(OrderDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(ordersDTO));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> obtenerOrdersPorStatus(@PathVariable("status") String status) {
        if (!securityUtils.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("Solo administradores pueden listar pedidos por estado globalmente");
        }
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderService.obtenerPorStatus(orderStatus);
            List<OrderDTO> ordersDTO = orders.stream()
                .map(OrderDTO::new)
                .toList();
            return ResponseEntity.ok(ApiResponse.success(ordersDTO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<Object> crearOrder(@Valid @RequestBody OrderDTO orderDTO) {
        try {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (!securityUtils.isAdmin()) {
                if (currentUserId == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(MSG_KEY, "Usuario no autenticado"));
                }
                orderDTO.setUserId(currentUserId);
            } else if (orderDTO.getUserId() == null) {
                orderDTO.setUserId(currentUserId);
            }

            Order.DeliveryType deliveryType = Order.DeliveryType.DELIVERY;
            if (orderDTO.getDeliveryType() != null) {
                deliveryType = Order.DeliveryType.valueOf(orderDTO.getDeliveryType().toUpperCase());
            }

            if (deliveryType == Order.DeliveryType.DELIVERY && orderDTO.getAddressId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MSG_KEY, "La direcci\u00f3n es requerida para entregas a domicilio"));
            }

            BigDecimal deliveryFee;
            if (deliveryType == Order.DeliveryType.PICKUP) {
                deliveryFee = BigDecimal.ZERO;
            } else if (storeSettingsService != null) {
                deliveryFee = storeSettingsService.getDeliveryFee();
            } else {
                deliveryFee = orderDTO.getDeliveryFee() != null ? orderDTO.getDeliveryFee() : BigDecimal.ZERO;
            }

            BigDecimal discount = BigDecimal.ZERO;
            String promoCode = orderDTO.getPromoCode();
            Integer promotionId = null;
            com.example.models.Promotion promotionAUsar = resolveValidPromotion(promoCode, orderDTO.getUserId());

            if (promotionAUsar != null) {
                discount = promotionService.calcularDescuento(promotionAUsar, orderDTO.getSubtotal());
                promoCode = promotionAUsar.getCode();
                promotionId = promotionAUsar.getId();
            } else {
                promoCode = null;
            }

            Order order = new Order();
            order.setUserId(orderDTO.getUserId());
            order.setAddressId(orderDTO.getAddressId());
            order.setStatus(Order.OrderStatus.PENDING);
            order.setDeliveryType(deliveryType);
            order.setPaymentMethodId(orderDTO.getPaymentMethodId());
            order.setSubtotal(orderDTO.getSubtotal());
            order.setDeliveryFee(deliveryFee);
            order.setDiscount(discount);
            order.setTotal(orderDTO.getSubtotal().add(deliveryFee).subtract(discount));
            order.setPromoCode(promoCode);
            order.setPromotionId(promotionId);
            order.setNotes(orderDTO.getNotes());
            order.setStockSettled("PENDING");
            order.setEstimatedDelivery(orderDTO.getEstimatedDelivery() != null ?
                orderDTO.getEstimatedDelivery() : LocalDateTime.now().plusMinutes(45));

            Order orderCreado = orderService.crearOrder(order);

            incrementarUsoPromocionSafely(promotionAUsar);

            OrderDTO responseDto = buildOrderDtoWithEmailDispatch(orderCreado);

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responseDto,
                        Boolean.TRUE.equals(responseDto.getEmailSent())
                                ? "Creado exitosamente y comprobante enviado por correo"
                                : "Creado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    private void incrementarUsoPromocionSafely(com.example.models.Promotion promotionAUsar) {
        if (promotionAUsar != null) {
            try {
                promotionService.incrementarUsoPromocion(promotionAUsar.getCode());
            } catch (Exception e) {
                logger.error("Error al incrementar uso de promoci\u00f3n", e);
            }
        }
    }

    private com.example.models.Promotion resolveValidPromotion(String promoCode, Integer userId) {
        if (promoCode != null && !promoCode.trim().isEmpty()) {
            try {
                var promotionOpt = promotionService.obtenerPorCodigo(promoCode);
                if (promotionOpt.isPresent()) {
                    com.example.models.Promotion promotion = promotionOpt.get();
                    promotionService.verificarReglasDeUsuario(promotion, userId);
                    return promotion;
                }
            } catch (IllegalArgumentException e) {
                // Ignore and fallthrough to return null
            }
        }
        return null;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderDTO>> checkout(@Valid @RequestBody CheckoutRequestDTO req) {
        try {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (!securityUtils.isAdmin()) {
                if (currentUserId == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Usuario no autenticado"));
                }
                if (req.getOrder() != null) {
                    req.getOrder().setUserId(currentUserId);
                }
            } else if (req.getOrder() != null && req.getOrder().getUserId() == null) {
                req.getOrder().setUserId(currentUserId);
            }

            Order orderCreado = checkoutService.checkout(req);
            OrderDTO responseDto = buildOrderDtoWithEmailDispatch(orderCreado);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(responseDto,
                        Boolean.TRUE.equals(responseDto.getEmailSent())
                                ? "Pedido confirmado y comprobante enviado por correo"
                                : "Pedido procesado exitosamente"));
        } catch (com.example.exceptions.InsufficientStockException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    private OrderDTO buildOrderDtoWithEmailDispatch(Order orderCreado) {
        OrderDTO dto = new OrderDTO(orderCreado);
        dto.setEmailSent(false);
        dto.setAdminEmailSent(false);
        if (orderCreado == null || orderCreado.getId() == null) {
            return dto;
        }
        try {
            if (emailService != null) {
                OrderEmailDispatchResultDTO mailResult = emailService.sendOrderCreatedEmails(orderCreado.getId());
                dto.setEmailSent(mailResult.customerEmailSent());
                dto.setEmailSentTo(mailResult.customerEmail());
                dto.setAdminEmailSent(mailResult.adminEmailSent());
            }
            if (stockAlertNotificationService != null) {
                // Las alertas de stock se consolidan en la ventana de 1 minuto (60s) para enviarse juntas en un solo correo
                stockAlertNotificationService.synchronizeAlertsAsync();
            }
        } catch (Exception mailEx) {
            logger.warn("El pedido #{} fue confirmado, pero ocurri\u00f3 un aviso al despachar correos: {}",
                    orderCreado.getId(), mailEx.getMessage());
        }
        return dto;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarOrder(@PathVariable("id") Integer id, @Valid @RequestBody OrderDTO orderDTO) {
        try {
            Order.DeliveryType deliveryType = Order.DeliveryType.DELIVERY;
            if (orderDTO.getDeliveryType() != null) {
                deliveryType = Order.DeliveryType.valueOf(orderDTO.getDeliveryType().toUpperCase());
            }

            Order order = new Order();
            order.setUserId(orderDTO.getUserId());
            order.setAddressId(orderDTO.getAddressId());
            order.setStatus(Order.OrderStatus.valueOf(orderDTO.getStatus()));
            order.setDeliveryType(deliveryType);
            order.setPaymentMethodId(orderDTO.getPaymentMethodId());
            order.setSubtotal(orderDTO.getSubtotal());
            order.setDeliveryFee(orderDTO.getDeliveryFee());
            order.setTotal(orderDTO.getTotal());
            order.setNotes(orderDTO.getNotes());
            order.setEstimatedDelivery(orderDTO.getEstimatedDelivery());

            Order orderActualizado = orderService.actualizarOrder(id, order);
            return ResponseEntity.ok(ApiResponse.success(new OrderDTO(orderActualizado)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Object> cambiarEstado(@PathVariable("id") Integer id, @RequestBody Map<String, String> payload) {
        try {
            String nuevoEstado = payload.get(STATUS_KEY);
            if (nuevoEstado == null) {
                return ResponseEntity.badRequest().body(Map.of(MSG_KEY, "El campo 'status' es requerido"));
            }

            Optional<Order> orderOpt = orderService.obtenerPorId(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, "Pedido no encontrado"));
            }

            Order order = orderOpt.get();
            Order.OrderStatus previousStatus = order.getStatus();
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(nuevoEstado.toUpperCase());

            if (newStatus == Order.OrderStatus.CANCELLED
                    && previousStatus != Order.OrderStatus.CANCELLED
                    && "DEDUCTED".equalsIgnoreCase(order.getStockSettled())) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (previousStatus == Order.OrderStatus.PENDING || previousStatus == Order.OrderStatus.CONFIRMED) {
                    inventoryService.returnOrderStock(order, currentUserId);
                    order.setStockSettled("RETURNED");
                } else if (previousStatus == Order.OrderStatus.PREPARING || previousStatus == Order.OrderStatus.OUT_FOR_DELIVERY) {
                    productLossService.recordCancelledOrderLoss(order, currentUserId);
                    order.setStockSettled("LOSS_RECORDED");
                }
            }

            order.setStatus(newStatus);
            Order orderActualizado = orderService.actualizarOrder(id, order);

            Optional<Payment> paymentOpt = paymentService.obtenerPorOrderId(id);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                if (newStatus == Order.OrderStatus.PENDING) {
                    payment.setStatus(Payment.PaymentStatus.PENDING);
                } else if (newStatus == Order.OrderStatus.CONFIRMED || newStatus == Order.OrderStatus.PREPARING ||
                           newStatus == Order.OrderStatus.OUT_FOR_DELIVERY || newStatus == Order.OrderStatus.DELIVERED) {
                    payment.setStatus(Payment.PaymentStatus.PAID);
                } else if (newStatus == Order.OrderStatus.CANCELLED) {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                }
                paymentService.actualizarPayment(payment.getId(), payment);
            }

            return ResponseEntity.ok(ApiResponse.success(new OrderDTO(orderActualizado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(MSG_KEY, "Estado inv\u00e1lido: " + payload.get(STATUS_KEY)));
        } catch (Exception e) {
            logger.error("Error al cambiar estado del pedido #{}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(MSG_KEY, "Error al actualizar el estado"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarOrder(@PathVariable("id") Integer id) {
        try {
            orderService.eliminarOrder(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Order eliminado correctamente")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @GetMapping("/complete")
    public ResponseEntity<ApiResponse<List<OrderCompleteDTO>>> listarOrdersCompletos(
            @RequestParam(value = "limit", defaultValue = "1000") int limit) {
        Integer currentUserId = null;
        if (!securityUtils.isAdmin()) {
            currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }

        int safeLimit = Math.max(1, Math.min(limit, 5000));
        List<OrderCompleteDTO> orders = fetchOrders(currentUserId, safeLimit);
        if (orders.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(orders));
        }

        List<Integer> orderIds = orders.stream().map(OrderCompleteDTO::getId).toList();
        Map<Integer, List<OrderCompleteDTO.OrderItemCompleteDTO>> itemsByOrder = new HashMap<>();
        List<Integer> allPizzaItemIds = new ArrayList<>();

        fetchOrderItems(orderIds, itemsByOrder, allPizzaItemIds);
        Map<Integer, List<String>> extrasMap = fetchItemExtras(allPizzaItemIds);

        assembleOrders(orders, itemsByOrder, extrasMap);

        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    private List<OrderCompleteDTO> fetchOrders(Integer currentUserId, int safeLimit) {
        String sql = """
            SELECT o.id AS order_id, o.delivery_type, o.status, o.subtotal, o.deliveryFee, o.discount, o.promo_code, o.total, o.notes, o.estimatedDelivery, o.createdAt AS order_date,
                   o.wants_boleta, o.boleta_tipo_comprobante, o.boleta_serie, o.boleta_numero, o.boleta_cliente_dni, o.boleta_cliente_nombre, o.boleta_cliente_direccion,
                   o.boleta_pdf_url, o.boleta_xml_url, o.boleta_cdr_url, o.boleta_enlace, o.boleta_hash, o.boleta_qr, o.boleta_estado, o.boleta_error,
                   o.boleta_total_gravada, o.boleta_total_igv, o.boleta_emitida_at,
                   u.id AS user_id, u.name AS customer_name, u.email AS customer_email, u.phone AS customer_phone, u.dni AS customer_dni,
                   a.id AS address_id, a.line1 AS address_line1, a.city AS address_city, a.district AS address_district, a.reference AS address_reference,
                   pm.id AS payment_method_id, pm.name AS payment_method_name
            FROM orders o
            INNER JOIN users u ON o.userId = u.id
            LEFT JOIN addresses a ON o.addressId = a.id
            LEFT JOIN payment_methods pm ON o.paymentMethodId = pm.id
            """ + (currentUserId != null ? "WHERE o.userId = ? " : "") + """
            ORDER BY o.createdAt DESC OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY
            """;

        List<Object> queryArgs = new ArrayList<>();
        if (currentUserId != null) queryArgs.add(currentUserId);
        queryArgs.add(safeLimit);

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OrderCompleteDTO dto = new OrderCompleteDTO();
            dto.setId(rs.getInt("order_id"));
            dto.setUserId(rs.getInt("user_id"));
            dto.setUserName(rs.getString("customer_name"));
            dto.setUserEmail(rs.getString("customer_email"));
            dto.setUserPhone(rs.getString("customer_phone"));
            dto.setUserDni(rs.getString("customer_dni"));
            dto.setAddressId(rs.getObject("address_id") != null ? rs.getInt("address_id") : null);
            dto.setAddressLine1(rs.getString("address_line1"));
            dto.setAddressCity(rs.getString("address_city"));
            dto.setAddressDistrict(rs.getString("address_district"));
            dto.setAddressReference(rs.getString("address_reference"));
            dto.setDeliveryType(rs.getString("delivery_type"));
            dto.setStatus(rs.getString(STATUS_KEY));
            dto.setPaymentMethodId(rs.getObject("payment_method_id") != null ? rs.getInt("payment_method_id") : null);
            dto.setPaymentMethodName(rs.getString("payment_method_name"));
            dto.setSubtotal(rs.getBigDecimal("subtotal"));
            dto.setDeliveryFee(rs.getBigDecimal("deliveryFee"));
            dto.setDiscount(rs.getBigDecimal("discount") != null ? rs.getBigDecimal("discount") : BigDecimal.ZERO);
            dto.setPromoCode(rs.getString("promo_code"));
            dto.setTotal(rs.getBigDecimal("total"));
            dto.setNotes(rs.getString("notes"));
            dto.setEstimatedDelivery(rs.getTimestamp("estimatedDelivery") != null ? rs.getTimestamp("estimatedDelivery").toLocalDateTime() : null);
            dto.setCreatedAt(rs.getTimestamp("order_date") != null ? rs.getTimestamp("order_date").toLocalDateTime() : null);

            dto.setWantsBoleta(rs.getObject("wants_boleta") != null && rs.getBoolean("wants_boleta"));
            dto.setBoletaTipoComprobante(rs.getObject("boleta_tipo_comprobante") != null ? rs.getInt("boleta_tipo_comprobante") : null);
            String serie = rs.getString("boleta_serie");
            Integer numero = rs.getObject("boleta_numero") != null ? rs.getInt("boleta_numero") : null;
            dto.setBoletaSerie(serie);
            dto.setBoletaNumero(numero);
            if (serie != null && numero != null) {
                dto.setBoletaComprobante(serie + "-" + String.format("%06d", numero));
            }
            dto.setBoletaClienteDni(rs.getString("boleta_cliente_dni"));
            dto.setBoletaClienteNombre(rs.getString("boleta_cliente_nombre"));
            dto.setBoletaClienteDireccion(rs.getString("boleta_cliente_direccion"));
            dto.setBoletaPdfUrl(rs.getString("boleta_pdf_url"));
            dto.setBoletaXmlUrl(rs.getString("boleta_xml_url"));
            dto.setBoletaCdrUrl(rs.getString("boleta_cdr_url"));
            dto.setBoletaEnlace(rs.getString("boleta_enlace"));
            dto.setBoletaHash(rs.getString("boleta_hash"));
            dto.setBoletaQr(rs.getString("boleta_qr"));
            dto.setBoletaEstado(rs.getString("boleta_estado"));
            dto.setBoletaError(rs.getString("boleta_error"));
            dto.setBoletaTotalGravada(rs.getBigDecimal("boleta_total_gravada"));
            dto.setBoletaTotalIgv(rs.getBigDecimal("boleta_total_igv"));
            dto.setBoletaEmitidaAt(rs.getTimestamp("boleta_emitida_at") != null ? rs.getTimestamp("boleta_emitida_at").toLocalDateTime() : null);
            return dto;
        }, queryArgs.toArray());
    }

    private void fetchOrderItems(List<Integer> orderIds, Map<Integer, List<OrderCompleteDTO.OrderItemCompleteDTO>> itemsByOrder, List<Integer> allPizzaItemIds) {
        String itemPlaceholders = orderIds.stream().map(id -> "?").collect(Collectors.joining(","));
        String itemSql = "SELECT oi.orderId, oi.id, oi.pizzaId, oi.extraId, oi.item_type, p.name AS pizza_name, e.name AS extra_name, oi.sizeId, s.name AS size_name, oi.quantity, oi.unitPrice, oi.lineTotal FROM order_items oi LEFT JOIN pizzas p ON oi.pizzaId = p.id LEFT JOIN extras e ON oi.extraId = e.id LEFT JOIN sizes s ON oi.sizeId = s.id WHERE oi.orderId IN (" + itemPlaceholders + ") ORDER BY oi.id";

        jdbcTemplate.query(itemSql, (rs) -> {
            int orderId = rs.getInt("orderId");
            OrderCompleteDTO.OrderItemCompleteDTO item = new OrderCompleteDTO.OrderItemCompleteDTO();
            item.setId(rs.getInt("id"));
            String itemType = rs.getString("item_type");
            item.setItemType(itemType != null ? itemType : "PIZZA");
            if (ITEM_TYPE_EXTRA.equals(itemType)) {
                item.setExtraId(rs.getObject(FIELD_EXTRA_ID) != null ? rs.getInt(FIELD_EXTRA_ID) : null);
                item.setExtraName(rs.getString(FIELD_EXTRA_NAME));
                item.setPizzaName(rs.getString(FIELD_EXTRA_NAME));
            } else {
                item.setPizzaId(rs.getObject(FIELD_PIZZA_ID) != null ? rs.getInt(FIELD_PIZZA_ID) : null);
                item.setPizzaName(rs.getString("pizza_name"));
                item.setSizeId(rs.getObject(FIELD_SIZE_ID) != null ? rs.getInt(FIELD_SIZE_ID) : null);
                item.setSizeName(rs.getString("size_name"));
                allPizzaItemIds.add(item.getId());
            }
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("unitPrice"));
            item.setLineTotal(rs.getBigDecimal("lineTotal"));
            itemsByOrder.computeIfAbsent(orderId, k -> new ArrayList<>()).add(item);
        }, orderIds.toArray());
    }

    private Map<Integer, List<String>> fetchItemExtras(List<Integer> allPizzaItemIds) {
        Map<Integer, List<String>> extrasMap = new HashMap<>();
        if (!allPizzaItemIds.isEmpty()) {
            String extraPlaceholders = allPizzaItemIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String extraSql = "SELECT orderItemId, ingredientName FROM order_item_extras WHERE orderItemId IN (" + extraPlaceholders + ") ORDER BY id";
            jdbcTemplate.query(extraSql, (rs) -> {
                extrasMap.computeIfAbsent(rs.getInt("orderItemId"), k -> new ArrayList<>()).add(rs.getString("ingredientName"));
            }, allPizzaItemIds.toArray());
        }
        return extrasMap;
    }

    private void assembleOrders(List<OrderCompleteDTO> orders, Map<Integer, List<OrderCompleteDTO.OrderItemCompleteDTO>> itemsByOrder, Map<Integer, List<String>> extrasMap) {
        for (OrderCompleteDTO order : orders) {
            List<OrderCompleteDTO.OrderItemCompleteDTO> items = itemsByOrder.getOrDefault(order.getId(), List.of());
            for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
                if (!ITEM_TYPE_EXTRA.equals(item.getItemType())) {
                    item.setExtras(extrasMap.getOrDefault(item.getId(), List.of()));
                }
            }
            order.setItems(items);
        }
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<OrderCompleteDTO.OrderItemCompleteDTO>>> obtenerItemsCompletos(@PathVariable("id") Integer id) {
        Optional<Order> orderOpt = orderService.obtenerPorId(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Pedido no encontrado"));
        }

        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !orderOpt.get().getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("No tienes permiso para ver los \u00edtems de este pedido"));
            }
        }

        String sql = "SELECT oi.id, oi.pizzaId, oi.extraId, oi.item_type, p.name AS pizza_name, e.name AS extra_name, oi.sizeId, s.name AS size_name, oi.quantity, oi.unitPrice, oi.lineTotal FROM order_items oi LEFT JOIN pizzas p ON oi.pizzaId = p.id LEFT JOIN extras e ON oi.extraId = e.id LEFT JOIN sizes s ON oi.sizeId = s.id WHERE oi.orderId = ? ORDER BY oi.id";

        List<Integer> pizzaItemIds = new ArrayList<>();
        List<OrderCompleteDTO.OrderItemCompleteDTO> items = jdbcTemplate.query(sql, (rs, rowNum) -> {
            OrderCompleteDTO.OrderItemCompleteDTO item = new OrderCompleteDTO.OrderItemCompleteDTO();
            item.setId(rs.getInt("id"));
            String itemType = rs.getString("item_type");
            item.setItemType(itemType != null ? itemType : "PIZZA");
            if (ITEM_TYPE_EXTRA.equals(itemType)) {
                item.setExtraId(rs.getObject(FIELD_EXTRA_ID) != null ? rs.getInt(FIELD_EXTRA_ID) : null);
                item.setExtraName(rs.getString(FIELD_EXTRA_NAME));
                item.setPizzaName(rs.getString(FIELD_EXTRA_NAME));
            } else {
                item.setPizzaId(rs.getObject(FIELD_PIZZA_ID) != null ? rs.getInt(FIELD_PIZZA_ID) : null);
                item.setPizzaName(rs.getString("pizza_name"));
                item.setSizeId(rs.getObject(FIELD_SIZE_ID) != null ? rs.getInt(FIELD_SIZE_ID) : null);
                item.setSizeName(rs.getString("size_name"));
                pizzaItemIds.add(item.getId());
            }
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("unitPrice"));
            item.setLineTotal(rs.getBigDecimal("lineTotal"));
            return item;
        }, id);

        Map<Integer, List<String>> extrasMap = fetchItemExtras(pizzaItemIds);
        for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
            if (!ITEM_TYPE_EXTRA.equals(item.getItemType())) {
                item.setExtras(extrasMap.getOrDefault(item.getId(), List.of()));
            }
        }

        return ResponseEntity.ok(ApiResponse.success(items));
    }
}

