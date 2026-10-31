package com.example.controllers;

import com.example.services.PaymentMethodService;
import com.example.dtos.PaymentMethodDTO;
import com.example.models.PaymentMethod;

import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payment-methods")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentMethodController {

    private static final String MSG_KEY = "message";
    private static final String MSG_NOT_FOUND = "Método de pago no encontrado";

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentMethodDTO>>> obtenerTodos(
            @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        List<PaymentMethod> paymentMethods = paymentMethodService.obtenerTodos(includeDeleted);
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> crearMetodoPago(@RequestBody PaymentMethodDTO paymentMethodDTO) {
        try {
            if (paymentMethodService.existsByName(paymentMethodDTO.getName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of(MSG_KEY, "El nombre del método de pago ya existe"));
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
                    .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Datos inválidos"));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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
                    .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "Datos inválidos"));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> eliminarMetodoPago(@PathVariable("id") Integer id) {
        try {
            boolean eliminado = paymentMethodService.eliminarMetodoPago(id);
            if (eliminado) {
                return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Método de pago eliminado lógicamente")));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage() != null ? e.getMessage() : "No se pudo eliminar el método de pago"));
        }
    }

    @PatchMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> restaurarMetodoPago(@PathVariable("id") Integer id) {
        boolean restaurado = paymentMethodService.restaurarMetodoPago(id);
        if (restaurado) {
            return ResponseEntity.ok(ApiResponse.success(Map.of(MSG_KEY, "Método de pago restaurado correctamente")));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(MSG_KEY, MSG_NOT_FOUND));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
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
