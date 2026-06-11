package com.example.infrastructure.controller;

import com.example.service.PaymentMethodService;
import com.example.domain.dto.PaymentMethodDTO;
import com.example.domain.model.PaymentMethod;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentMethodDTO>> obtenerTodos() {
        List<PaymentMethod> paymentMethods = paymentMethodService.obtenerTodos();
        List<PaymentMethodDTO> dtos = paymentMethods.stream()
                .map(PaymentMethodDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable("id") Integer id) {
        Optional<PaymentMethod> paymentMethod = paymentMethodService.obtenerPorId(id);
        if (paymentMethod.isPresent()) {
            return ResponseEntity.ok(new PaymentMethodDTO(paymentMethod.get()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Método de pago no encontrado"));
    }

    @GetMapping("/active")
    public ResponseEntity<List<PaymentMethodDTO>> obtenerActivos() {
        List<PaymentMethod> paymentMethods = paymentMethodService.obtenerActivos();
        List<PaymentMethodDTO> dtos = paymentMethods.stream()
                .map(PaymentMethodDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<?> crearMetodoPago(@RequestBody PaymentMethodDTO paymentMethodDTO) {
        try {
            if (paymentMethodService.existsByName(paymentMethodDTO.getName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("message", "El nombre del método de pago ya existe"));
            }

            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setName(paymentMethodDTO.getName());
            paymentMethod.setDescription(paymentMethodDTO.getDescription());
            paymentMethod.setActive(paymentMethodDTO.isActive());
            paymentMethod.setDisplayOrder(paymentMethodDTO.getDisplayOrder());

            PaymentMethod nuevoMetodo = paymentMethodService.crearMetodoPago(paymentMethod);
            return ResponseEntity.ok(new PaymentMethodDTO(nuevoMetodo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarMetodoPago(@PathVariable("id") Integer id, @RequestBody PaymentMethodDTO paymentMethodDTO) {
        try {
            PaymentMethod paymentMethodActualizado = new PaymentMethod();
            paymentMethodActualizado.setName(paymentMethodDTO.getName());
            paymentMethodActualizado.setDescription(paymentMethodDTO.getDescription());
            paymentMethodActualizado.setActive(paymentMethodDTO.isActive());
            paymentMethodActualizado.setDisplayOrder(paymentMethodDTO.getDisplayOrder());

            PaymentMethod paymentMethod = paymentMethodService.actualizarMetodoPago(id, paymentMethodActualizado);
            if (paymentMethod != null) {
                return ResponseEntity.ok(new PaymentMethodDTO(paymentMethod));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Método de pago no encontrado"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarMetodoPago(@PathVariable("id") Integer id) {
        boolean eliminado = paymentMethodService.eliminarMetodoPago(id);
        if (eliminado) {
            return ResponseEntity.ok(Map.of("message", "Método de pago eliminado correctamente"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Método de pago no encontrado"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> cambiarEstado(
            @PathVariable("id") Integer id, 
            @RequestParam("active") boolean active) {
        PaymentMethod paymentMethod = paymentMethodService.cambiarEstado(id, active);
        if (paymentMethod != null) {
            return ResponseEntity.ok(new PaymentMethodDTO(paymentMethod));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Método de pago no encontrado"));
    }
}