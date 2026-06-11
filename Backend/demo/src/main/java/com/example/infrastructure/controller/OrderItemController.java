package com.example.infrastructure.controller;

import com.example.service.OrderItemService;
import com.example.domain.dto.OrderItemDTO;
import com.example.domain.model.OrderItem;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<List<OrderItemDTO>> listarOrderItems() {
        List<OrderItem> orderItems = orderItemService.listarOrderItems();
        List<OrderItemDTO> orderItemsDTO = orderItems.stream()
            .map(OrderItemDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(orderItemsDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerOrderItemPorId(@PathVariable("id") Integer id) {
        Optional<OrderItem> orderItemOpt = orderItemService.obtenerPorId(id);
        if (orderItemOpt.isPresent()) {
            OrderItemDTO orderItemDTO = new OrderItemDTO(orderItemOpt.get());
            return ResponseEntity.ok(orderItemDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "OrderItem no encontrado"));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderItemDTO>> obtenerOrderItemsPorOrderId(@PathVariable("orderId") Integer orderId) {
        List<OrderItem> orderItems = orderItemService.obtenerPorOrderId(orderId);
        List<OrderItemDTO> orderItemsDTO = orderItems.stream()
            .map(OrderItemDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(orderItemsDTO);
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
            return ResponseEntity.status(HttpStatus.CREATED).body(new OrderItemDTO(orderItemCreado));
        } catch (Exception e) {
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
            return ResponseEntity.ok(new OrderItemDTO(orderItemActualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarOrderItem(@PathVariable("id") Integer id) {
        try {
            orderItemService.eliminarOrderItem(id);
            return ResponseEntity.ok(Map.of("message", "OrderItem eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}