package com.example.infrastructure.controller;

import com.example.domain.dto.PromotionDTO;
import com.example.domain.model.Promotion;
import com.example.service.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/promociones")
@CrossOrigin(origins = "http://localhost:4200")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public ResponseEntity<List<PromotionDTO>> listarTodas() {
        List<Promotion> promotions = promotionService.listarPromociones();
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/activas")
    public ResponseEntity<List<PromotionDTO>> listarActivas() {
        List<Promotion> promotions = promotionService.listarPromocionesActivas();
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/activas/{tipo}")
    public ResponseEntity<List<PromotionDTO>> listarActivasPorTipo(@PathVariable Promotion.ApplicableTo tipo) {
        List<Promotion> promotions = promotionService.listarPromocionesActivasPorTipo(tipo);
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromotionDTO> obtenerPorId(@PathVariable Integer id) {
        Optional<Promotion> promotion = promotionService.obtenerPorId(id);
        return promotion.map(p -> ResponseEntity.ok(new PromotionDTO(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{code}")
    public ResponseEntity<PromotionDTO> obtenerPorCodigo(@PathVariable String code) {
        Optional<Promotion> promotion = promotionService.obtenerPorCodigo(code);
        return promotion.map(p -> ResponseEntity.ok(new PromotionDTO(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/validar")
    public ResponseEntity<?> validarPromocion(@RequestBody Map<String, Object> request) {
        try {
            String code = (String) request.get("code");
            BigDecimal orderTotal = new BigDecimal(request.get("orderTotal").toString());
            Integer userId = request.containsKey("userId") ? 
                Integer.parseInt(request.get("userId").toString()) : null;
            
            // Obtener items si están disponibles
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> items = 
                (java.util.List<java.util.Map<String, Object>>) request.get("items");
            
            if (items != null && !items.isEmpty()) {
                // Validación con items del carrito (calcula descuento solo sobre items aplicables)
                Map<String, Object> result = promotionService.validarPromocionConItems(
                    code, orderTotal, userId, items
                );
                return ResponseEntity.ok(result);
            } else if (userId != null) {
                // Validación completa con verificación de primera compra
                Map<String, Object> result = promotionService.validarPromocionParaUsuario(code, orderTotal, userId);
                return ResponseEntity.ok(result);
            } else {
                // Validación básica sin userId (para preview)
                BigDecimal discount = promotionService.calcularDescuento(code, orderTotal);
                return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "discount", discount,
                    "finalTotal", orderTotal.subtract(discount)
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                "message", e.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Promotion promotion) {
        try {
            Promotion nuevaPromocion = promotionService.crearPromocion(promotion);
            return ResponseEntity.status(HttpStatus.CREATED).body(new PromotionDTO(nuevaPromocion));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Promotion promotion) {
        try {
            Promotion promocionActualizada = promotionService.actualizarPromocion(id, promotion);
            return ResponseEntity.ok(new PromotionDTO(promocionActualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            promotionService.eliminarPromocion(id);
            return ResponseEntity.ok().body(Map.of("message", "Promoción eliminada correctamente", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al eliminar promoción: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable Integer id) {
        try {
            Promotion promocionActualizada = promotionService.activarPromocion(id);
            return ResponseEntity.ok(new PromotionDTO(promocionActualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable Integer id) {
        try {
            Promotion promocionActualizada = promotionService.desactivarPromocion(id);
            return ResponseEntity.ok(new PromotionDTO(promocionActualizada));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/proximas-vencer")
    public ResponseEntity<List<PromotionDTO>> proximasAVencer(@RequestParam(defaultValue = "7") int dias) {
        List<Promotion> promotions = promotionService.obtenerPromocionesProximasAVencer(dias);
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
