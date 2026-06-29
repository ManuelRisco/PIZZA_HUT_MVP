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
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;

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
        
        // Si es ADMIN, ver todos los pedidos
        if (securityUtils.isAdmin()) {
            orders = orderService.listarOrders();
        } else {
            // Si es CUSTOMER, solo ver sus propios pedidos
            Integer userId = securityUtils.getCurrentUserId();
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            orders = orderService.obtenerPorUserId(userId);
        }
        
        List<OrderDTO> ordersDTO = orders.stream()
            .map(OrderDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(ordersDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerOrderPorId(@PathVariable("id") Integer id) {
        Optional<Order> orderOpt = orderService.obtenerPorId(id);
        
        if (orderOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Order no encontrado"));
        }
        
        Order order = orderOpt.get();
        
        // Validar que el cliente solo pueda ver sus propios pedidos
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !order.getUserId().equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para ver este pedido"));
            }
        }
        
        OrderDTO orderDTO = new OrderDTO(order);
        return ResponseEntity.ok(ApiResponse.success(orderDTO));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> obtenerOrdersPorUserId(@PathVariable("userId") Integer userId) {
        // Validar que el cliente solo pueda ver sus propios pedidos
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null || !userId.equals(currentUserId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para ver estos pedidos"));
            }
        }
        
        List<Order> orders = orderService.obtenerPorUserId(userId);
        List<OrderDTO> ordersDTO = orders.stream()
            .map(OrderDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(ordersDTO));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> obtenerOrdersPorStatus(@PathVariable("status") String status) {
        try {
            Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            List<Order> orders = orderService.obtenerPorStatus(orderStatus);
            List<OrderDTO> ordersDTO = orders.stream()
                .map(OrderDTO::new)
                .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(ordersDTO));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<?> crearOrder(@RequestBody OrderDTO orderDTO) {
        try {
            // Validar deliveryType
            Order.DeliveryType deliveryType = Order.DeliveryType.DELIVERY; // Default
            if (orderDTO.getDeliveryType() != null) {
                deliveryType = Order.DeliveryType.valueOf(orderDTO.getDeliveryType().toUpperCase());
            }
            
            // Si es DELIVERY, validar que tenga addressId
            if (deliveryType == Order.DeliveryType.DELIVERY && orderDTO.getAddressId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "La direcci\u00f3n es requerida para entregas a domicilio"));
            }
            
            // Si es PICKUP, el delivery fee debe ser 0
            BigDecimal deliveryFee = orderDTO.getDeliveryFee();
            if (deliveryType == Order.DeliveryType.PICKUP) {
                deliveryFee = BigDecimal.ZERO;
            }
            
            // Calcular descuento si hay código promocional
            BigDecimal discount = BigDecimal.ZERO;
            String promoCode = orderDTO.getPromoCode();
            Integer promotionId = null;
            com.example.models.Promotion promotionAUsar = null;
            
            if (promoCode != null && !promoCode.trim().isEmpty()) {
                try {
                    // Obtener la promoción una sola vez
                    var promotionOpt = promotionService.obtenerPorCodigo(promoCode);
                    if (promotionOpt.isPresent()) {
                        promotionAUsar = promotionOpt.get();
                        BigDecimal subtotal = orderDTO.getSubtotal();
                        discount = promotionService.calcularDescuento(promotionAUsar, subtotal);
                        promotionId = promotionAUsar.getId();
                    }
                } catch (IllegalArgumentException e) {
                    // Si el código es inválido, ignorar el descuento pero no fallar
                    discount = BigDecimal.ZERO;
                    promoCode = null;
                    promotionId = null;
                    promotionAUsar = null;
                }
            }
            
            Order order = new Order();
            order.setUserId(orderDTO.getUserId());
            order.setAddressId(orderDTO.getAddressId());
            order.setStatus(Order.OrderStatus.PENDING); // Estado inicial
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
            
            // Incrementar el contador de uso de la promoción si se aplicó
            if (promotionAUsar != null) {
                try {
                    promotionService.incrementarUsoPromocion(promotionAUsar);
                } catch (RuntimeException e) { throw e; } catch (Exception e) {
                    // Log pero no fallar el pedido si no se puede incrementar
                    System.err.println("Error al incrementar uso de promoción: " + e.getMessage());
                }
            }
            
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new OrderDTO(orderCreado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderDTO>> checkout(@RequestBody CheckoutRequestDTO req) {
        try {
            Order orderCreado = checkoutService.checkout(req);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new OrderDTO(orderCreado), "Pedido procesado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarOrder(@PathVariable("id") Integer id, @RequestBody OrderDTO orderDTO) {
        try {
            // Validar deliveryType
            Order.DeliveryType deliveryType = Order.DeliveryType.DELIVERY; // Default
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
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> cambiarEstado(@PathVariable("id") Integer id, @RequestBody Map<String, String> payload) {
        try {
            String nuevoEstado = payload.get("status");
            if (nuevoEstado == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "El campo 'status' es requerido"));
            }

            Optional<Order> orderOpt = orderService.obtenerPorId(id);
            if (orderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Pedido no encontrado"));
            }

            Order order = orderOpt.get();
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(nuevoEstado.toUpperCase());
            order.setStatus(newStatus);
            Order orderActualizado = orderService.actualizarOrder(id, order);
            
            // Actualizar el estado del pago seg\u00fan el estado del pedido
            Optional<Payment> paymentOpt = paymentService.obtenerPorOrderId(id);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                
                // PENDING â†’ el pago queda en PENDING
                if (newStatus == Order.OrderStatus.PENDING) {
                    payment.setStatus(Payment.PaymentStatus.PENDING);
                    paymentService.actualizarPayment(payment.getId(), payment);
                }
                // CONFIRMED, PREPARING, OUT_FOR_DELIVERY, DELIVERED â†’ el pago pasa a PAID
                else if (newStatus == Order.OrderStatus.CONFIRMED || 
                         newStatus == Order.OrderStatus.PREPARING || 
                         newStatus == Order.OrderStatus.OUT_FOR_DELIVERY || 
                         newStatus == Order.OrderStatus.DELIVERED) {
                    payment.setStatus(Payment.PaymentStatus.PAID);
                    paymentService.actualizarPayment(payment.getId(), payment);
                }
                // CANCELLED â†’ el pago pasa a FAILED
                else if (newStatus == Order.OrderStatus.CANCELLED) {
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    paymentService.actualizarPayment(payment.getId(), payment);
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success(new OrderDTO(orderActualizado)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Estado inv\u00e1lido: " + payload.get("status")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Error al actualizar el estado"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarOrder(@PathVariable("id") Integer id) {
        try {
            orderService.eliminarOrder(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Order eliminado correctamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
    
    @GetMapping("/complete")
    public ResponseEntity<ApiResponse<List<OrderCompleteDTO>>> listarOrdersCompletos() {
        // Filtrar por usuario si es CUSTOMER
        Integer currentUserId = null;
        
        if (!securityUtils.isAdmin()) {
            currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
        }
        
        String sql = """
            SELECT 
                o.id AS order_id,
                o.delivery_type,
                o.status,
                o.subtotal,
                o.deliveryFee,
                o.discount,
                o.promo_code,
                o.total,
                o.notes,
                o.estimatedDelivery,
                o.createdAt AS order_date,
                
                u.id AS user_id,
                u.name AS customer_name,
                u.email AS customer_email,
                u.phone AS customer_phone,
                
                a.id AS address_id,
                a.line1 AS address_line1,
                a.city AS address_city,
                a.district AS address_district,
                a.reference AS address_reference,
                
                pm.id AS payment_method_id,
                pm.name AS payment_method_name
                
            FROM orders o
            INNER JOIN users u ON o.userId = u.id
            LEFT JOIN addresses a ON o.addressId = a.id
            LEFT JOIN payment_methods pm ON o.paymentMethodId = pm.id
            """ + (currentUserId != null ? "WHERE o.userId = ? " : "") + """
            ORDER BY o.createdAt DESC
            """;
        
        // Ejecutar query con parámetros (previene SQL injection)
        List<Object> queryArgs = new ArrayList<>();
        if (currentUserId != null) {
            queryArgs.add(currentUserId);
        }
        
        List<OrderCompleteDTO> orders = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
            dto.setStatus(rs.getString("status"));
            
            dto.setPaymentMethodId(rs.getObject("payment_method_id") != null ? rs.getInt("payment_method_id") : null);
            dto.setPaymentMethodName(rs.getString("payment_method_name"));
            
            dto.setSubtotal(rs.getBigDecimal("subtotal"));
            dto.setDeliveryFee(rs.getBigDecimal("deliveryFee"));
            dto.setDiscount(rs.getBigDecimal("discount") != null ? rs.getBigDecimal("discount") : BigDecimal.ZERO);
            dto.setPromoCode(rs.getString("promo_code"));
            dto.setTotal(rs.getBigDecimal("total"));
            dto.setNotes(rs.getString("notes"));
            dto.setEstimatedDelivery(rs.getTimestamp("estimatedDelivery") != null ? 
                rs.getTimestamp("estimatedDelivery").toLocalDateTime() : null);
            dto.setCreatedAt(rs.getTimestamp("order_date").toLocalDateTime());
            
            return dto;
        }, queryArgs.toArray());
        
        if (orders.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(orders));
        }
        
        // === QUERY 2: Todos los items de TODOS los pedidos (1 sola query en vez de N) ===
        List<Integer> orderIds = orders.stream().map(dto -> dto.getId()).toList();
        String itemPlaceholders = orderIds.stream().map(id -> "?").collect(Collectors.joining(","));
        
        String itemSql = "SELECT "
            + "oi.orderId, oi.id, oi.pizzaId, oi.extraId, oi.item_type, "
            + "p.name AS pizza_name, e.name AS extra_name, "
            + "oi.sizeId, s.name AS size_name, "
            + "oi.quantity, oi.unitPrice, oi.lineTotal "
            + "FROM order_items oi "
            + "LEFT JOIN pizzas p ON oi.pizzaId = p.id "
            + "LEFT JOIN extras e ON oi.extraId = e.id "
            + "LEFT JOIN sizes s ON oi.sizeId = s.id "
            + "WHERE oi.orderId IN (" + itemPlaceholders + ") "
            + "ORDER BY oi.id";
        
        Map<Integer, List<OrderCompleteDTO.OrderItemCompleteDTO>> itemsByOrder = new HashMap<>();
        List<Integer> allPizzaItemIds = new ArrayList<>();
        
        jdbcTemplate.query(itemSql, (rs) -> {
            int orderId = rs.getInt("orderId");
            OrderCompleteDTO.OrderItemCompleteDTO item = new OrderCompleteDTO.OrderItemCompleteDTO();
            item.setId(rs.getInt("id"));
            
            String itemType = rs.getString("item_type");
            item.setItemType(itemType != null ? itemType : "PIZZA");
            
            if ("EXTRA".equals(itemType)) {
                item.setExtraId(rs.getObject("extraId") != null ? rs.getInt("extraId") : null);
                item.setExtraName(rs.getString("extra_name"));
                item.setPizzaName(rs.getString("extra_name"));
            } else {
                item.setPizzaId(rs.getObject("pizzaId") != null ? rs.getInt("pizzaId") : null);
                item.setPizzaName(rs.getString("pizza_name"));
                item.setSizeId(rs.getObject("sizeId") != null ? rs.getInt("sizeId") : null);
                item.setSizeName(rs.getString("size_name"));
                allPizzaItemIds.add(item.getId());
            }
            
            item.setQuantity(rs.getInt("quantity"));
            item.setUnitPrice(rs.getBigDecimal("unitPrice"));
            item.setLineTotal(rs.getBigDecimal("lineTotal"));
            
            itemsByOrder.computeIfAbsent(orderId, k -> new ArrayList<>()).add(item);
        }, orderIds.toArray());
        
        // === QUERY 3: Todos los extras de TODOS los items pizza (1 sola query en vez de N*M) ===
        Map<Integer, List<String>> extrasMap = new HashMap<>();
        if (!allPizzaItemIds.isEmpty()) {
            String extraPlaceholders = allPizzaItemIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String extraSql = "SELECT orderItemId, ingredientName "
                + "FROM order_item_extras "
                + "WHERE orderItemId IN (" + extraPlaceholders + ") "
                + "ORDER BY id";
            
            jdbcTemplate.query(extraSql, (rs) -> {
                extrasMap.computeIfAbsent(rs.getInt("orderItemId"), k -> new ArrayList<>())
                    .add(rs.getString("ingredientName"));
            }, allPizzaItemIds.toArray());
        }
        
        // === ENSAMBLAJE: Asociar extras → items → orders (en memoria, 0 queries adicionales) ===
        for (OrderCompleteDTO order : orders) {
            List<OrderCompleteDTO.OrderItemCompleteDTO> items = 
                itemsByOrder.getOrDefault(order.getId(), List.of());
            for (OrderCompleteDTO.OrderItemCompleteDTO item : items) {
                if (!"EXTRA".equals(item.getItemType())) {
                    item.setExtras(extrasMap.getOrDefault(item.getId(), List.of()));
                }
            }
            order.setItems(items);
        }
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    
    private List<String> obtenerExtrasPorItem(Integer orderItemId) {
        String sql = """
            SELECT ingredientName
            FROM order_item_extras
            WHERE orderItemId = ?
            ORDER BY id
            """;
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("ingredientName"), orderItemId);
    }
    
    @GetMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<OrderCompleteDTO.OrderItemCompleteDTO>>> obtenerItemsCompletos(@PathVariable("id") Integer id) {
        String sql = """
            SELECT 
                oi.id,
                oi.pizzaId,
                oi.extraId,
                oi.item_type,
                p.name AS pizza_name,
                e.name AS extra_name,
                oi.sizeId,
                s.name AS size_name,
                oi.quantity,
                oi.unitPrice,
                oi.lineTotal
            FROM order_items oi
            LEFT JOIN pizzas p ON oi.pizzaId = p.id
            LEFT JOIN extras e ON oi.extraId = e.id
            LEFT JOIN sizes s ON oi.sizeId = s.id
            WHERE oi.orderId = ?
            ORDER BY oi.id
            """;
        
        List<OrderCompleteDTO.OrderItemCompleteDTO> items = jdbcTemplate.query(sql, (rs, rowNum) -> {
            OrderCompleteDTO.OrderItemCompleteDTO item = new OrderCompleteDTO.OrderItemCompleteDTO();
            item.setId(rs.getInt("id"));
            
            String itemType = rs.getString("item_type");
            item.setItemType(itemType != null ? itemType : "PIZZA");
            
            if ("EXTRA".equals(itemType)) {
                item.setExtraId(rs.getObject("extraId") != null ? rs.getInt("extraId") : null);
                item.setExtraName(rs.getString("extra_name"));
                item.setPizzaName(rs.getString("extra_name")); // Usar extra_name como pizzaName para compatibilidad
            } else {
                item.setPizzaId(rs.getObject("pizzaId") != null ? rs.getInt("pizzaId") : null);
                item.setPizzaName(rs.getString("pizza_name"));
                item.setSizeId(rs.getObject("sizeId") != null ? rs.getInt("sizeId") : null);
                item.setSizeName(rs.getString("size_name"));
                // Obtener los ingredientes extras de este item (solo para pizzas)
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
