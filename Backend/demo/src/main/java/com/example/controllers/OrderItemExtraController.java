package com.example.controllers;

import com.example.services.OrderItemExtraService;
import com.example.services.OrderItemService;
import com.example.services.OrderService;
import com.example.dtos.OrderItemExtraDTO;
import com.example.models.OrderItemExtra;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/order-item-extras")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderItemExtraController {

    private final OrderItemExtraService orderItemExtraService;
    private final OrderItemService orderItemService;
    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    public OrderItemExtraController(OrderItemExtraService orderItemExtraService,
                                    OrderItemService orderItemService,
                                    OrderService orderService,
                                    SecurityUtils securityUtils) {
        this.orderItemExtraService = orderItemExtraService;
        this.orderItemService = orderItemService;
        this.orderService = orderService;
        this.securityUtils = securityUtils;
    }

    private void validarAccesoAOrderItem(Integer orderItemId) {
        if (!securityUtils.isAdmin()) {
            Integer currentUserId = securityUtils.getCurrentUserId();
            if (currentUserId == null) {
                throw new AccessDeniedException("Usuario no autenticado");
            }
            
            OrderItem orderItem = orderItemService.obtenerPorId(orderItemId)
                .orElseThrow(() -> new IllegalArgumentException("El Ã­tem de pedido no existe"));
                
            Order order = orderService.obtenerPorId(orderItem.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe"));
                
            if (!order.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("No tienes permiso para acceder a este Ã­tem");
            }
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderItemExtraDTO>>> listarTodos() {
        if (!securityUtils.isAdmin()) {
            throw new AccessDeniedException("Solo administradores pueden listar todos los extras");
        }
        
        List<OrderItemExtra> extras = orderItemExtraService.listarTodos();
        List<OrderItemExtraDTO> extrasDTO = extras.stream()
            .map(OrderItemExtraDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(extrasDTO));
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<Object> obtenerPorOrderItemId(@PathVariable("orderItemId") Integer orderItemId) {
        try {
            validarAccesoAOrderItem(orderItemId);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
        
        List<OrderItemExtra> extras = orderItemExtraService.obtenerPorOrderItemId(orderItemId);
        List<OrderItemExtraDTO> extrasDTO = extras.stream()
            .map(OrderItemExtraDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(extrasDTO));
    }

    @PostMapping
    public ResponseEntity<Object> crear(@Valid @RequestBody OrderItemExtraDTO orderItemExtraDTO) {
        try {
            validarAccesoAOrderItem(orderItemExtraDTO.getOrderItemId());
            
            OrderItemExtra orderItemExtra = new OrderItemExtra();
            orderItemExtra.setOrderItemId(orderItemExtraDTO.getOrderItemId());
            orderItemExtra.setIngredientId(orderItemExtraDTO.getIngredientId());
            orderItemExtra.setIngredientName(orderItemExtraDTO.getIngredientName());
            orderItemExtra.setExtraCost(orderItemExtraDTO.getExtraCost());

            OrderItemExtra creado = orderItemExtraService.crearOrderItemExtra(orderItemExtra);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new OrderItemExtraDTO(creado), "Creado exitosamente"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/order-item/{orderItemId}")
    public ResponseEntity<Object> eliminarPorOrderItemId(@PathVariable("orderItemId") Integer orderItemId) {
        try {
            validarAccesoAOrderItem(orderItemId);
            
            orderItemExtraService.eliminarPorOrderItemId(orderItemId);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable("id") Integer id) {
        try {
            Optional<OrderItemExtra> extraOpt = orderItemExtraService.obtenerPorId(id);
            if (extraOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            validarAccesoAOrderItem(extraOpt.get().getOrderItemId());
            
            orderItemExtraService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}

