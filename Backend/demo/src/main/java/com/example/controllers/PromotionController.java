package com.example.controllers;

import com.example.dtos.PromotionDTO;
import com.example.models.Promotion;
import com.example.services.PromotionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.example.dtos.ApiResponse;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;

@RestController
@RequestMapping("/api/promociones")
@CrossOrigin(origins = "http://localhost:4200")
public class PromotionController {

    private static final String MSG_KEY = "message";

    private final PromotionService promotionService;
    private final SecurityUtils securityUtils;

    public PromotionController(PromotionService promotionService, SecurityUtils securityUtils) {
        this.promotionService = promotionService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> listarTodas() {
        List<Promotion> promotions = promotionService.listarPromociones();
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/activas")
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> listarActivas() {
        List<Promotion> promotions = promotionService.listarPromocionesActivas();
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/activas/{tipo}")
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> listarActivasPorTipo(@PathVariable Promotion.ApplicableTo tipo) {
        List<Promotion> promotions = promotionService.listarPromocionesActivasPorTipo(tipo);
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromotionDTO>> obtenerPorId(@PathVariable Integer id) {
        Optional<Promotion> promotion = promotionService.obtenerPorId(id);
        return promotion.map(p -> ResponseEntity.ok(ApiResponse.success(new PromotionDTO(p))))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/codigo/{code}")
    public ResponseEntity<ApiResponse<PromotionDTO>> obtenerPorCodigo(@PathVariable String code) {
        Optional<Promotion> promotion = promotionService.obtenerPorCodigo(code);
        return promotion.map(p -> ResponseEntity.ok(ApiResponse.success(new PromotionDTO(p))))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/validar")
    public ResponseEntity<Object> validarPromocion(@RequestBody Map<String, Object> request) {
        try {
            String code = (String) request.get("code");
            BigDecimal orderTotal = new BigDecimal(request.get("orderTotal").toString());
            
            Integer userId = request.containsKey("userId") ? 
                Integer.parseInt(request.get("userId").toString()) : null;
            
            // PREVENCIÃ“N DE IDOR: Forzar que el usuario solo pueda validar para sÃ­ mismo
            if (!securityUtils.isAdmin()) {
                Integer currentUserId = securityUtils.getCurrentUserId();
                if (currentUserId == null) {
                    throw new AccessDeniedException("Usuario no autenticado");
                }
                userId = currentUserId; // Forzar el ID del token
            }
            
            // Obtener items si estÃ¡n disponibles
            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, Object>> items = 
                (java.util.List<java.util.Map<String, Object>>) request.get("items");
            
            if (items != null && !items.isEmpty()) {
                // ValidaciÃ³n con items del carrito (calcula descuento solo sobre items aplicables)
                Map<String, Object> result = promotionService.validarPromocionConItems(
                    code, orderTotal, userId, items
                );
                return ResponseEntity.ok(ApiResponse.success(result));
            } else if (userId != null) {
                // ValidaciÃ³n completa con verificaciÃ³n de primera compra
                Map<String, Object> result = promotionService.validarPromocionParaUsuario(code, orderTotal, userId);
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                // ValidaciÃ³n bÃ¡sica sin userId (para preview)
                BigDecimal discount = promotionService.calcularDescuento(code, orderTotal);
                return ResponseEntity.ok(ApiResponse.success(Map.of(
                    "valid", true,
                    "discount", discount,
                    "finalTotal", orderTotal.subtract(discount))
                ));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "valid", false,
                MSG_KEY, e.getMessage()
            ));
        }
    }

    @PostMapping
    public ResponseEntity<Object> crear(@RequestBody PromotionDTO promotionDTO) {
        try {
            Promotion promotion = new Promotion();
            promotion.setCode(promotionDTO.getCode());
            promotion.setName(promotionDTO.getName());
            promotion.setDescription(promotionDTO.getDescription());
            if (promotionDTO.getDiscountType() != null) {
                promotion.setDiscountType(Promotion.DiscountType.valueOf(promotionDTO.getDiscountType()));
            }
            promotion.setDiscountValue(promotionDTO.getDiscountValue());
            promotion.setFinalPrice(promotionDTO.getFinalPrice());
            promotion.setMinPurchase(promotionDTO.getMinPurchase());
            promotion.setMaxDiscount(promotionDTO.getMaxDiscount());
            if (promotionDTO.getIsActive() != null) promotion.setIsActive(promotionDTO.getIsActive());
            promotion.setStartDate(promotionDTO.getStartDate());
            promotion.setEndDate(promotionDTO.getEndDate());
            promotion.setUsageLimit(promotionDTO.getUsageLimit());
            if (promotionDTO.getApplicableTo() != null) {
                promotion.setApplicableTo(Promotion.ApplicableTo.valueOf(promotionDTO.getApplicableTo()));
            }

            Promotion nuevaPromocion = promotionService.crearPromocion(promotion);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(new PromotionDTO(nuevaPromocion), "Creado exitosamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizar(@PathVariable Integer id, @RequestBody PromotionDTO promotionDTO) {
        try {
            Promotion promotion = new Promotion();
            promotion.setCode(promotionDTO.getCode());
            promotion.setName(promotionDTO.getName());
            promotion.setDescription(promotionDTO.getDescription());
            if (promotionDTO.getDiscountType() != null) {
                promotion.setDiscountType(Promotion.DiscountType.valueOf(promotionDTO.getDiscountType()));
            }
            promotion.setDiscountValue(promotionDTO.getDiscountValue());
            promotion.setFinalPrice(promotionDTO.getFinalPrice());
            promotion.setMinPurchase(promotionDTO.getMinPurchase());
            promotion.setMaxDiscount(promotionDTO.getMaxDiscount());
            if (promotionDTO.getIsActive() != null) promotion.setIsActive(promotionDTO.getIsActive());
            promotion.setStartDate(promotionDTO.getStartDate());
            promotion.setEndDate(promotionDTO.getEndDate());
            promotion.setUsageLimit(promotionDTO.getUsageLimit());
            if (promotionDTO.getApplicableTo() != null) {
                promotion.setApplicableTo(Promotion.ApplicableTo.valueOf(promotionDTO.getApplicableTo()));
            }

            Promotion promocionActualizada = promotionService.actualizarPromocion(id, promotion);
            return ResponseEntity.ok(ApiResponse.success(new PromotionDTO(promocionActualizada)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> eliminar(@PathVariable Integer id) {
        try {
            promotionService.eliminarPromocion(id);
            return ResponseEntity.ok().body(Map.of(MSG_KEY, "PromociÃ³n eliminada correctamente", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) { throw e; } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error al eliminar promociÃ³n: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<Object> activar(@PathVariable Integer id) {
        try {
            Promotion promocionActualizada = promotionService.activarPromocion(id);
            return ResponseEntity.ok(ApiResponse.success(new PromotionDTO(promocionActualizada)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Object> desactivar(@PathVariable Integer id) {
        try {
            Promotion promocionActualizada = promotionService.desactivarPromocion(id);
            return ResponseEntity.ok(ApiResponse.success(new PromotionDTO(promocionActualizada)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/proximas-vencer")
    public ResponseEntity<ApiResponse<List<PromotionDTO>>> proximasAVencer(@RequestParam(defaultValue = "7") int dias) {
        List<Promotion> promotions = promotionService.obtenerPromocionesProximasAVencer(dias);
        List<PromotionDTO> dtos = promotions.stream()
            .map(PromotionDTO::new)
            .toList();
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
}

