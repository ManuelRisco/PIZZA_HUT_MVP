package com.example.infrastructure.controller;

import com.example.service.OrderTrackingService;
import com.example.domain.dto.OrderTrackingDTO;
import com.example.domain.model.OrderTracking;
import com.example.domain.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-tracking")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderTrackingController {

    @Autowired
    private OrderTrackingService orderTrackingService;

    @GetMapping
    public ResponseEntity<List<OrderTrackingDTO>> listarOrderTrackings() {
        List<OrderTracking> orderTrackings = orderTrackingService.listarOrderTrackings();
        List<OrderTrackingDTO> orderTrackingsDTO = orderTrackings.stream()
            .map(OrderTrackingDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(orderTrackingsDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerOrderTrackingPorId(@PathVariable("id") Integer id) {
        Optional<OrderTracking> orderTrackingOpt = orderTrackingService.obtenerPorId(id);
        if (orderTrackingOpt.isPresent()) {
            OrderTrackingDTO orderTrackingDTO = new OrderTrackingDTO(orderTrackingOpt.get());
            return ResponseEntity.ok(orderTrackingDTO);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "OrderTracking no encontrado"));
        }
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<OrderTrackingDTO>> obtenerOrderTrackingsPorOrderId(@PathVariable("orderId") Integer orderId) {
        List<OrderTracking> orderTrackings = orderTrackingService.obtenerPorOrderId(orderId);
        List<OrderTrackingDTO> orderTrackingsDTO = orderTrackings.stream()
            .map(OrderTrackingDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(orderTrackingsDTO);
    }

    @PostMapping
    public ResponseEntity<?> crearOrderTracking(@RequestBody OrderTrackingDTO orderTrackingDTO) {
        try {
            OrderTracking orderTracking = new OrderTracking();
            orderTracking.setOrderId(orderTrackingDTO.getOrderId());
            orderTracking.setStatus(Order.OrderStatus.valueOf(orderTrackingDTO.getStatus()));
            orderTracking.setDescription(orderTrackingDTO.getDescription());
            
            OrderTracking orderTrackingCreado = orderTrackingService.crearOrderTracking(orderTracking);
            return ResponseEntity.status(HttpStatus.CREATED).body(new OrderTrackingDTO(orderTrackingCreado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarOrderTracking(@PathVariable("id") Integer id) {
        try {
            orderTrackingService.eliminarOrderTracking(id);
            return ResponseEntity.ok(Map.of("message", "OrderTracking eliminado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", e.getMessage()));
        }
    }
}