package com.example.controllers;

import com.example.services.OrderItemService;
import com.example.services.OrderService;
import com.example.dtos.OrderItemDTO;
import com.example.models.OrderItem;
import com.example.models.Order;
import com.example.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/order-items")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderItemController {

    private static final String MSG_KEY = "message";
    private static final String ITEM_TYPE_PIZZA = "PIZZA";
    private static final String ERROR_NOT_FOUND = "OrderItem no encontrado";

    private final OrderItemService orderItemService;
    private final com.example.services.PizzaService pizzaService;
    private final com.example.services.ExtraService extraService;
    private final com.example.services.SizeService sizeService;
    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public OrderItemController(OrderItemService orderItemService,
                               com.example.services.PizzaService pizzaService,
                               com.example.services.ExtraService extraService,
                               com.example.services.SizeService sizeService,
                               OrderService orderService,
                               SecurityUtils securityUtils) {
        this.orderItemService = orderItemService;
        this.pizzaService = pizzaService;
        this.extraService = extraService;
        this.sizeService = sizeService;
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    private void validarAccesoAOrder(Integer orderId) {
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw new AccessDeniedException("Usuario no autenticado");
            }
            
            Order order = orderService.obtenerPorId(orderId)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe"));
                
            if (!order.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("No tienes permiso para acceder a este pedido");
            }
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderItemDTO>>> listarOrderItems() {
        if (!securityUtils.isAdmin()) {
            throw new AccessDeniedException("Solo administradores pueden listar todos los items");
        }
        
        List<OrderItem> orderItems = orderItemService.listarOrderItems();
        List<OrderItemDTO> orderItemsDTO = orderItems.stream()
            .map(OrderItemDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(orderItemsDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerOrderItemPorId(@PathVariable("id") Integer id) {
        Optional<OrderItem> orderItemOpt = orderItemService.obtenerPorId(id);
        if (orderItemOpt.isPresent()) {
            OrderItem orderItem = orderItemOpt.get();
            try {
                validarAccesoAOrder(orderItem.getOrderId());
            } catch (AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(MSG_KEY, e.getMessage()));
            }
            
            OrderItemDTO orderItemDTO = new OrderItemDTO(orderItem);
            return ResponseEntity.ok(ApiResponse.success(orderItemDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, ERROR_NOT_FOUND));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Object> obtenerOrderItemsPorOrderId(@PathVariable("orderId") Integer orderId) {
        try {
            validarAccesoAOrder(orderId);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
        
        List<OrderItem> orderItems = orderItemService.obtenerPorOrderId(orderId);
        List<OrderItemDTO> orderItemsDTO = orderItems.stream()
            .map(OrderItemDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(orderItemsDTO));
    }

    @PostMapping
    public ResponseEntity<Object> crearOrderItem(@Valid @RequestBody OrderItemDTO orderItemDTO) {
        try {
            validarAccesoAOrder(orderItemDTO.getOrderId());
            
            OrderItem orderItem = new OrderItem();
            populateOrderItemAndPrices(orderItem, orderItemDTO);

            OrderItem orderItemCreado = orderItemService.crearOrderItem(orderItem);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new OrderItemDTO(orderItemCreado), "Creado exitosamente"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(MSG_KEY, e.getMessage()));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarOrderItem(@PathVariable("id") Integer id, @Valid @RequestBody OrderItemDTO orderItemDTO) {
        try {
            Optional<OrderItem> orderItemOpt = orderItemService.obtenerPorId(id);
            if (orderItemOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MSG_KEY, ERROR_NOT_FOUND));
            }
            validarAccesoAOrder(orderItemOpt.get().getOrderId());
            
            // Si intenta cambiar a otro pedido, validarlo tambien
            if (!orderItemOpt.get().getOrderId().equals(orderItemDTO.getOrderId())) {
                validarAccesoAOrder(orderItemDTO.getOrderId());
            }

            OrderItem orderItem = new OrderItem();
            populateOrderItemAndPrices(orderItem, orderItemDTO);

            OrderItem orderItemActualizado = orderItemService.actualizarOrderItem(id, orderItem);
            return ResponseEntity.ok(ApiResponse.success(new OrderItemDTO(orderItemActualizado)));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(MSG_KEY, e.getMessage()));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarOrderItem(@PathVariable("id") Integer id) {
        try {
            Optional<OrderItem> orderItemOpt = orderItemService.obtenerPorId(id);
            if (orderItemOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MSG_KEY, ERROR_NOT_FOUND));
            }
            validarAccesoAOrder(orderItemOpt.get().getOrderId());
            
            orderItemService.eliminarOrderItem(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "OrderItem eliminado correctamente")));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(MSG_KEY, e.getMessage()));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    private void populateOrderItemAndPrices(OrderItem orderItem, OrderItemDTO orderItemDTO) {
        java.math.BigDecimal unitPrice = java.math.BigDecimal.ZERO;
        java.math.BigDecimal sizeExtra = java.math.BigDecimal.ZERO;
        String itemType = orderItemDTO.getItemType() != null ? orderItemDTO.getItemType().toUpperCase() : ITEM_TYPE_PIZZA;

        if (ITEM_TYPE_PIZZA.equals(itemType) && orderItemDTO.getPizzaId() != null) {
            com.example.models.Pizza pizza = pizzaService.obtenerPorId(orderItemDTO.getPizzaId())
                    .orElseThrow(() -> new IllegalArgumentException("Pizza no encontrada"));
            unitPrice = pizza.getPrice();
            
            if (orderItemDTO.getSizeId() != null) {
                com.example.models.Size size = sizeService.obtenerPorId(orderItemDTO.getSizeId())
                        .orElseThrow(() -> new IllegalArgumentException("TamaÃ±o no encontrado"));
                sizeExtra = size.getExtraCost();
            }
        } else if ("EXTRA".equals(itemType) && orderItemDTO.getExtraId() != null) {
            com.example.models.Extra extra = extraService.obtenerPorId(orderItemDTO.getExtraId())
                    .orElseThrow(() -> new IllegalArgumentException("Extra no encontrado"));
            unitPrice = extra.getPrice();
        }

        java.math.BigDecimal totalUnitPrice = unitPrice.add(sizeExtra);
        java.math.BigDecimal lineTotal = totalUnitPrice.multiply(new java.math.BigDecimal(orderItemDTO.getQuantity()));

        orderItem.setOrderId(orderItemDTO.getOrderId());
        orderItem.setPizzaId(orderItemDTO.getPizzaId());
        orderItem.setExtraId(orderItemDTO.getExtraId());
        orderItem.setItemType(itemType);
        orderItem.setSizeId(orderItemDTO.getSizeId());
        orderItem.setQuantity(orderItemDTO.getQuantity());
        orderItem.setUnitPrice(totalUnitPrice);
        orderItem.setSizeExtra(sizeExtra);
        orderItem.setLineTotal(lineTotal);
    }
}

