package com.example.controllers;

import com.example.services.OrderTrackingService;
import com.example.services.OrderService;
import com.example.dtos.OrderTrackingDTO;
import com.example.models.OrderTracking;
import com.example.models.Order;
import com.example.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-tracking")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderTrackingController {

    private final OrderTrackingService orderTrackingService;
    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public OrderTrackingController(OrderTrackingService orderTrackingService,
                                   OrderService orderService,
                                   SecurityUtils securityUtils) {
        this.orderTrackingService = orderTrackingService;
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
                throw new AccessDeniedException("No tienes permiso para acceder al rastreo de este pedido");
            }
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderTrackingDTO>>> listarOrderTrackings() {
        if (!securityUtils.isAdmin()) {
            throw new AccessDeniedException("Solo administradores pueden listar todos los rastreos");
        }
        
        List<OrderTracking> orderTrackings = orderTrackingService.listarOrderTrackings();
        List<OrderTrackingDTO> orderTrackingsDTO = orderTrackings.stream()
            .map(OrderTrackingDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(orderTrackingsDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerOrderTrackingPorId(@PathVariable("id") Integer id) {
        Optional<OrderTracking> orderTrackingOpt = orderTrackingService.obtenerPorId(id);
        if (orderTrackingOpt.isPresent()) {
            OrderTracking orderTracking = orderTrackingOpt.get();
            try {
                validarAccesoAOrder(orderTracking.getOrderId());
            } catch (AccessDeniedException e) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));
            }
            
            OrderTrackingDTO orderTrackingDTO = new OrderTrackingDTO(orderTracking);
            return ResponseEntity.ok(ApiResponse.success(orderTrackingDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "OrderTracking no encontrado"));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<?> obtenerOrderTrackingsPorOrderId(@PathVariable("orderId") Integer orderId) {
        try {
            validarAccesoAOrder(orderId);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", e.getMessage()));
        }
        
        List<OrderTracking> orderTrackings = orderTrackingService.obtenerPorOrderId(orderId);
        List<OrderTrackingDTO> orderTrackingsDTO = orderTrackings.stream()
            .map(OrderTrackingDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(orderTrackingsDTO));
    }

    @PostMapping
    public ResponseEntity<?> crearOrderTracking(@RequestBody OrderTrackingDTO orderTrackingDTO) {
        try {
            OrderTracking orderTracking = new OrderTracking();
            orderTracking.setOrderId(orderTrackingDTO.getOrderId());
            orderTracking.setStatus(Order.OrderStatus.valueOf(orderTrackingDTO.getStatus()));
            orderTracking.setDescription(orderTrackingDTO.getDescription());
            
            OrderTracking orderTrackingCreado = orderTrackingService.crearOrderTracking(orderTracking);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new OrderTrackingDTO(orderTrackingCreado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarOrderTracking(@PathVariable("id") Integer id) {
        try {
            orderTrackingService.eliminarOrderTracking(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "OrderTracking eliminado correctamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
