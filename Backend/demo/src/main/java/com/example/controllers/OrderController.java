package com.example.controllers;

import com.example.services.OrderService;
import com.example.services.PromotionService;
import com.example.services.PaymentService;
import com.example.services.CheckoutService;
import com.example.dtos.OrderDTO;
import com.example.dtos.OrderCompleteDTO;
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
@CrossOrigin(origins = "http://localhost:4200")
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

    public OrderController(OrderService orderService, CheckoutService checkoutService, PromotionService promotionService, JdbcTemplate jdbcTemplate, SecurityUtils securityUtils, PaymentService paymentService) {
        this.orderService = orderService;
        this.checkoutService = checkoutService;
        this.promotionService = promotionService;
        this.jdbcTemplate = jdbcTemplate;
        this.securityUtils = securityUtils;
        this.paymentService = paymentService;
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
                    .body(Map.of(MSG_KEY, "La direcciÃ³n es requerida para entregas a domicilio"));
            }
            
            BigDecimal deliveryFee = orderDTO.getDeliveryFee();
            if (deliveryType == Order.DeliveryType.PICKUP) {
                deliveryFee = BigDecimal.ZERO;
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
            order.setEstimatedDelivery(orderDTO.getEstimatedDelivery() != null ? 
                orderDTO.getEstimatedDelivery() : LocalDateTime.now().plusMinutes(45));
            
            Order orderCreado = orderService.crearOrder(order);
            
            incrementarUsoPromocionSafely(promotionAUsar);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new OrderDTO(orderCreado), "Creado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
    }

    private void incrementarUsoPromocionSafely(com.example.models.Promotion promotionAUsar) {
        if (promotionAUsar != null) {
            try {
                promotionService.incrementarUsoPromocion(promotionAUsar.getCode());
            } catch (Exception e) {
                logger.error("Error al incrementar uso de promoción", e);
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
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new OrderDTO(orderCreado), "Pedido procesado exitosamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(e.getMessage()));
        }
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
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(nuevoEstado.toUpperCase());
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
            return ResponseEntity.badRequest().body(Map.of(MSG_KEY, "Estado invÃ¡lido: " + payload.get(STATUS_KEY)));
        } catch (Exception e) {
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
    public ResponseEntity<ApiResponse<List<OrderCompleteDTO>>> listarOrdersCompletos() {
        Integer currentUserId = null;
        if (!securityUtils.isAdmin()) {
            currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        
        List<OrderCompleteDTO> orders = fetchOrders(currentUserId);
        if (orders.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(orders));
        }
        
        List<Integer> orderIds = orders.stream().map(o -> o.getId()).toList();
        Map<Integer, List<OrderCompleteDTO.OrderItemCompleteDTO>> itemsByOrder = new HashMap<>();
        List<Integer> allPizzaItemIds = new ArrayList<>();
        
        fetchOrderItems(orderIds, itemsByOrder, allPizzaItemIds);
        Map<Integer, List<String>> extrasMap = fetchItemExtras(allPizzaItemIds);
        
        assembleOrders(orders, itemsByOrder, extrasMap);
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    private List<OrderCompleteDTO> fetchOrders(Integer currentUserId) {
        String sql = """
            SELECT o.id AS order_id, o.delivery_type, o.status, o.subtotal, o.deliveryFee, o.discount, o.promo_code, o.total, o.notes, o.estimatedDelivery, o.createdAt AS order_date,
                   u.id AS user_id, u.name AS customer_name, u.email AS customer_email, u.phone AS customer_phone,
                   a.id AS address_id, a.line1 AS address_line1, a.city AS address_city, a.district AS address_district, a.reference AS address_reference,
                   pm.id AS payment_method_id, pm.name AS payment_method_name
            FROM orders o
            INNER JOIN users u ON o.userId = u.id
            LEFT JOIN addresses a ON o.addressId = a.id
            LEFT JOIN payment_methods pm ON o.paymentMethodId = pm.id
            """ + (currentUserId != null ? "WHERE o.userId = ? " : "") + """
            ORDER BY o.createdAt DESC LIMIT 100
            """;
        
        List<Object> queryArgs = new ArrayList<>();
        if (currentUserId != null) queryArgs.add(currentUserId);
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OrderCompleteDTO dto = new OrderCompleteDTO();
            dto.setId(rs.getInt("order_id"));
            dto.setUserId(rs.getInt("user_id"));
            dto.setUserName(rs.getString("customer_name"));
            dto.setUserEmail(rs.getString("customer_email"));
            dto.setUserPhone(rs.getString("customer_phone"));
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
            dto.setCreatedAt(rs.getTimestamp("order_date").toLocalDateTime());
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

    private List<String> obtenerExtrasPorItem(Integer orderItemId) {
        String sql = "SELECT ingredientName FROM order_item_extras WHERE orderItemId = ? ORDER BY id";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("ingredientName"), orderItemId);
    }
    
    @GetMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<OrderCompleteDTO.OrderItemCompleteDTO>>> obtenerItemsCompletos(@PathVariable("id") Integer id) {
        String sql = "SELECT oi.id, oi.pizzaId, oi.extraId, oi.item_type, p.name AS pizza_name, e.name AS extra_name, oi.sizeId, s.name AS size_name, oi.quantity, oi.unitPrice, oi.lineTotal FROM order_items oi LEFT JOIN pizzas p ON oi.pizzaId = p.id LEFT JOIN extras e ON oi.extraId = e.id LEFT JOIN sizes s ON oi.sizeId = s.id WHERE oi.orderId = ? ORDER BY oi.id";
        
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
                item.setExtras(obtenerExtrasPorItem(rs.getInt("id")));
            }
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("unitPrice"));
            item.setLineTotal(rs.getBigDecimal("lineTotal"));
            return item;
        }, id);
        
        return ResponseEntity.ok(ApiResponse.success(items));
    }
}

