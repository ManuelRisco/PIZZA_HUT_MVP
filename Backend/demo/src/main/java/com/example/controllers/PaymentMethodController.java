package com.example.controllers;

import com.example.services.PaymentMethodService;
import com.example.dtos.PaymentMethodDTO;
import com.example.models.PaymentMethod;

import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentMethodController {

    private static final String MSG_KEY = "message";
    private static final String MSG_NOT_FOUND = "MÃ©todo de pago no encontrado";

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> obtenerTodos() {
        List<PaymentMethod> paymentMethods = paymentMethodService.obtenerTodos();
        java.util.Set<Integer> inUseIds = paymentMethodService.getPaymentMethodIdsInUse();
        List<PaymentMethodDTO> dtos = paymentMethods.stream()
                .map(pm -> {
                    PaymentMethodDTO dto = new PaymentMethodDTO(pm);
                    dto.setInUse(inUseIds.contains(pm.getId()));
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> obtenerPorId(@PathVariable("id") Integer id) {
        Optional<PaymentMethod> paymentMethod = paymentMethodService.obtenerPorId(id);
        if (paymentMethod.isPresent()) {
            PaymentMethodDTO dto = new PaymentMethodDTO(paymentMethod.get());
            dto.setInUse(paymentMethodService.isInUse(id));
            return ResponseEntity.ok(ApiResponse.success(dto));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> obtenerActivos() {
        List<PaymentMethod> paymentMethods = paymentMethodService.obtenerActivos();
        java.util.Set<Integer> inUseIds = paymentMethodService.getPaymentMethodIdsInUse();
        List<PaymentMethodDTO> dtos = paymentMethods.stream()
                .map(pm -> {
                    PaymentMethodDTO dto = new PaymentMethodDTO(pm);
                    dto.setInUse(inUseIds.contains(pm.getId()));
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @PostMapping
    public ResponseEntity<Object> crearMetodoPago(@RequestBody PaymentMethodDTO paymentMethodDTO) {
        try {
            if (paymentMethodService.existsByName(paymentMethodDTO.getName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(MSG_KEY, "El nombre del mÃ©todo de pago ya existe"));
            }

            PaymentMethod paymentMethod = new PaymentMethod();
            paymentMethod.setName(paymentMethodDTO.getName());
            paymentMethod.setDescription(paymentMethodDTO.getDescription());
            paymentMethod.setActive(paymentMethodDTO.getIsActive());
            paymentMethod.setDisplayOrder(paymentMethodDTO.getDisplayOrder());

            PaymentMethod nuevoMetodo = paymentMethodService.crearMetodoPago(paymentMethod);
            return ResponseEntity.ok(ApiResponse.success(new PaymentMethodDTO(nuevoMetodo)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizarMetodoPago(@PathVariable("id") Integer id, @RequestBody PaymentMethodDTO paymentMethodDTO) {
        try {
            PaymentMethod paymentMethodActualizado = new PaymentMethod();
            paymentMethodActualizado.setName(paymentMethodDTO.getName());
            paymentMethodActualizado.setDescription(paymentMethodDTO.getDescription());
            paymentMethodActualizado.setActive(paymentMethodDTO.getIsActive());
            paymentMethodActualizado.setDisplayOrder(paymentMethodDTO.getDisplayOrder());

            PaymentMethod paymentMethod = paymentMethodService.actualizarMetodoPago(id, paymentMethodActualizado);
            if (paymentMethod != null) {
                PaymentMethodDTO dto = new PaymentMethodDTO(paymentMethod);
                dto.setInUse(paymentMethodService.isInUse(id));
                return ResponseEntity.ok(ApiResponse.success(dto));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminarMetodoPago(@PathVariable("id") Integer id) {
        try {
            boolean eliminado = paymentMethodService.eliminarMetodoPago(id);
            if (eliminado) {
                return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "MÃ©todo de pago eliminado correctamente")));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(MSG_KEY, e.getMessage()));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Object> cambiarEstado(
            @PathVariable("id") Integer id, 
            @RequestParam("active") boolean active) {
        PaymentMethod paymentMethod = paymentMethodService.cambiarEstado(id, active);
        if (paymentMethod != null) {
            return ResponseEntity.ok(ApiResponse.success(new PaymentMethodDTO(paymentMethod)));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
    }
}

