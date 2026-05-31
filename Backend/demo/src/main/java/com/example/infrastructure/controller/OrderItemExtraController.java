package com.example.infrastructure.controller;

import com.example.service.OrderItemExtraService;
import com.example.domain.dto.OrderItemExtraDTO;
import com.example.domain.model.OrderItemExtra;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-item-extras")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderItemExtraController {

    @Autowired
    private OrderItemExtraService orderItemExtraService;

    @GetMapping
    public ResponseEntity<List<OrderItemExtraDTO>> listarTodos() {
        List<OrderItemExtra> extras = orderItemExtraService.listarTodos();
        List<OrderItemExtraDTO> extrasDTO = extras.stream()
            .map(OrderItemExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(extrasDTO);
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<List<OrderItemExtraDTO>> obtenerPorOrderItemId(@PathVariable("orderItemId") Integer orderItemId) {
        List<OrderItemExtra> extras = orderItemExtraService.obtenerPorOrderItemId(orderItemId);
        List<OrderItemExtraDTO> extrasDTO = extras.stream()
            .map(OrderItemExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(extrasDTO);
    }

    @PostMapping
    public ResponseEntity<OrderItemExtraDTO> crear(@RequestBody OrderItemExtraDTO orderItemExtraDTO) {
        try {
            OrderItemExtra orderItemExtra = new OrderItemExtra();
            orderItemExtra.setOrderItemId(orderItemExtraDTO.getOrderItemId());
            orderItemExtra.setIngredientId(orderItemExtraDTO.getIngredientId());
            orderItemExtra.setIngredientName(orderItemExtraDTO.getIngredientName());
            orderItemExtra.setExtraCost(orderItemExtraDTO.getExtraCost());

            OrderItemExtra creado = orderItemExtraService.crearOrderItemExtra(orderItemExtra);
            return ResponseEntity.status(HttpStatus.CREATED).body(new OrderItemExtraDTO(creado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/order-item/{orderItemId}")
    public ResponseEntity<Void> eliminarPorOrderItemId(@PathVariable("orderItemId") Integer orderItemId) {
        try {
            orderItemExtraService.eliminarPorOrderItemId(orderItemId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Integer id) {
        try {
            orderItemExtraService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}