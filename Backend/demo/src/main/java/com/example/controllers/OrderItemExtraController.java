package com.example.controllers;

import com.example.services.OrderItemExtraService;
import com.example.dtos.OrderItemExtraDTO;
import com.example.models.OrderItemExtra;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/order-item-extras")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderItemExtraController {

    private final OrderItemExtraService orderItemExtraService;

    public OrderItemExtraController(OrderItemExtraService orderItemExtraService) {
        this.orderItemExtraService = orderItemExtraService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderItemExtraDTO>>> listarTodos() {
        List<OrderItemExtra> extras = orderItemExtraService.listarTodos();
        List<OrderItemExtraDTO> extrasDTO = extras.stream()
            .map(OrderItemExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(extrasDTO));
    }

    @GetMapping("/order-item/{orderItemId}")
    public ResponseEntity<ApiResponse<List<OrderItemExtraDTO>>> obtenerPorOrderItemId(@PathVariable("orderItemId") Integer orderItemId) {
        List<OrderItemExtra> extras = orderItemExtraService.obtenerPorOrderItemId(orderItemId);
        List<OrderItemExtraDTO> extrasDTO = extras.stream()
            .map(OrderItemExtraDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(extrasDTO));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderItemExtraDTO>> crear(@RequestBody OrderItemExtraDTO orderItemExtraDTO) {
        try {
            OrderItemExtra orderItemExtra = new OrderItemExtra();
            orderItemExtra.setOrderItemId(orderItemExtraDTO.getOrderItemId());
            orderItemExtra.setIngredientId(orderItemExtraDTO.getIngredientId());
            orderItemExtra.setIngredientName(orderItemExtraDTO.getIngredientName());
            orderItemExtra.setExtraCost(orderItemExtraDTO.getExtraCost());

            OrderItemExtra creado = orderItemExtraService.crearOrderItemExtra(orderItemExtra);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new OrderItemExtraDTO(creado), "Creado exitosamente"));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/order-item/{orderItemId}")
    public ResponseEntity<Void> eliminarPorOrderItemId(@PathVariable("orderItemId") Integer orderItemId) {
        try {
            orderItemExtraService.eliminarPorOrderItemId(orderItemId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Integer id) {
        try {
            orderItemExtraService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
