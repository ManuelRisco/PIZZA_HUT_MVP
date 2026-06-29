package com.example.controllers;

import com.example.services.OrderItemService;
import com.example.dtos.OrderItemDTO;
import com.example.models.OrderItem;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-items")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderItemController {

    private final OrderItemService orderItemService;

    public OrderItemController(OrderItemService orderItemService) {
        this.orderItemService = orderItemService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderItemDTO>>> listarOrderItems() {
        List<OrderItem> orderItems = orderItemService.listarOrderItems();
        List<OrderItemDTO> orderItemsDTO = orderItems.stream()
            .map(OrderItemDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(orderItemsDTO));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerOrderItemPorId(@PathVariable("id") Integer id) {
        Optional<OrderItem> orderItemOpt = orderItemService.obtenerPorId(id);
        if (orderItemOpt.isPresent()) {
            OrderItemDTO orderItemDTO = new OrderItemDTO(orderItemOpt.get());
            return ResponseEntity.ok(ApiResponse.success(orderItemDTO));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "OrderItem no encontrado"));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<OrderItemDTO>>> obtenerOrderItemsPorOrderId(@PathVariable("orderId") Integer orderId) {
        List<OrderItem> orderItems = orderItemService.obtenerPorOrderId(orderId);
        List<OrderItemDTO> orderItemsDTO = orderItems.stream()
            .map(OrderItemDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(orderItemsDTO));
    }

    @PostMapping
    public ResponseEntity<?> crearOrderItem(@RequestBody OrderItemDTO orderItemDTO) {
        try {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderItemDTO.getOrderId());
            orderItem.setPizzaId(orderItemDTO.getPizzaId());
            orderItem.setExtraId(orderItemDTO.getExtraId());
            orderItem.setItemType(orderItemDTO.getItemType() != null ? orderItemDTO.getItemType() : "PIZZA");
            orderItem.setSizeId(orderItemDTO.getSizeId());
            orderItem.setQuantity(orderItemDTO.getQuantity());
            orderItem.setUnitPrice(orderItemDTO.getUnitPrice());
            orderItem.setSizeExtra(orderItemDTO.getSizeExtra());
            orderItem.setLineTotal(orderItemDTO.getLineTotal());
            
            OrderItem orderItemCreado = orderItemService.crearOrderItem(orderItem);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new OrderItemDTO(orderItemCreado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarOrderItem(@PathVariable("id") Integer id, @RequestBody OrderItemDTO orderItemDTO) {
        try {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(orderItemDTO.getOrderId());
            orderItem.setPizzaId(orderItemDTO.getPizzaId());
            orderItem.setExtraId(orderItemDTO.getExtraId());
            orderItem.setItemType(orderItemDTO.getItemType() != null ? orderItemDTO.getItemType() : "PIZZA");
            orderItem.setSizeId(orderItemDTO.getSizeId());
            orderItem.setQuantity(orderItemDTO.getQuantity());
            orderItem.setUnitPrice(orderItemDTO.getUnitPrice());
            orderItem.setSizeExtra(orderItemDTO.getSizeExtra());
            orderItem.setLineTotal(orderItemDTO.getLineTotal());
            
            OrderItem orderItemActualizado = orderItemService.actualizarOrderItem(id, orderItem);
            return ResponseEntity.ok(ApiResponse.success(new OrderItemDTO(orderItemActualizado)));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarOrderItem(@PathVariable("id") Integer id) {
        try {
            orderItemService.eliminarOrderItem(id);
            return ResponseEntity.ok(ApiResponse.success(Map.of("message", "OrderItem eliminado correctamente")));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}
