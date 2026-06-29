package com.example.services;

import com.example.models.Promotion;
import com.example.repositories.PromotionRepository;
import com.example.repositories.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    
    private final OrderRepository orderRepository;

    public PromotionService(PromotionRepository promotionRepository, OrderRepository orderRepository) {
        this.promotionRepository = promotionRepository;
        this.orderRepository = orderRepository;
    }

    public List<Promotion> listarPromociones() {
        return promotionRepository.findByDeletedAtIsNull();
    }

    public List<Promotion> listarPromocionesActivas() {
        return promotionRepository.findActivePromotions(LocalDateTime.now());
    }

    public List<Promotion> listarPromocionesActivasPorTipo(Promotion.ApplicableTo tipo) {
        return promotionRepository.findActivePromotionsByType(LocalDateTime.now(), tipo);
    }

    public Optional<Promotion> obtenerPorId(Integer id) {
        if (id == null) {
            return Optional.empty();
        }
        return promotionRepository.findById(id)
            .filter(promotion -> promotion.getDeletedAt() == null);
    }

    public Optional<Promotion> obtenerPorCodigo(String code) {
        return promotionRepository.findByCodeAndDeletedAtIsNull(code);
    }

    public Promotion crearPromocion(Promotion promotion) {
        if (promotionRepository.existsByCodeAndDeletedAtIsNull(promotion.getCode())) {
            throw new IllegalArgumentException("Ya existe una promoción con ese código");
        }
        
        // Si existe inactiva (soft deleted), reactivar
        Optional<Promotion> softDeleted = promotionRepository.findByCode(promotion.getCode());
        if (softDeleted.isPresent() && softDeleted.get().getDeletedAt() != null) {
            Promotion existing = softDeleted.get();
            existing.setDeletedAt(null);
            existing.setName(promotion.getName());
            existing.setDescription(promotion.getDescription());
            existing.setDiscountType(promotion.getDiscountType());
            existing.setDiscountValue(promotion.getDiscountValue());
            existing.setFinalPrice(promotion.getFinalPrice());
            existing.setMinPurchase(promotion.getMinPurchase());
            existing.setMaxDiscount(promotion.getMaxDiscount());
            existing.setIsActive(promotion.getIsActive());
            existing.setStartDate(promotion.getStartDate());
            existing.setEndDate(promotion.getEndDate());
            existing.setUsageLimit(promotion.getUsageLimit());
            existing.setApplicableTo(promotion.getApplicableTo());
            return promotionRepository.save(existing);
        }

        // Validaciones
        if (promotion.getStartDate().isAfter(promotion.getEndDate())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        return promotionRepository.save(promotion);
    }

    public Promotion actualizarPromocion(Integer id, Promotion promocionActualizada) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("Promoción no encontrada");
        }

        Promotion promotion = promocionOpt.get();
        
        // Check code uniqueness if code changed
        if (!promotion.getCode().equals(promocionActualizada.getCode()) &&
            promotionRepository.existsByCodeAndDeletedAtIsNull(promocionActualizada.getCode())) {
            throw new IllegalArgumentException("Ya existe una promoción activa con ese código");
        }
        
        promotion.setCode(promocionActualizada.getCode());
        promotion.setName(promocionActualizada.getName());
        promotion.setDescription(promocionActualizada.getDescription());
        promotion.setDiscountType(promocionActualizada.getDiscountType());
        promotion.setDiscountValue(promocionActualizada.getDiscountValue());
        promotion.setFinalPrice(promocionActualizada.getFinalPrice());
        promotion.setMinPurchase(promocionActualizada.getMinPurchase());
        promotion.setMaxDiscount(promocionActualizada.getMaxDiscount());
        promotion.setIsActive(promocionActualizada.getIsActive());
        promotion.setStartDate(promocionActualizada.getStartDate());
        promotion.setEndDate(promocionActualizada.getEndDate());
        promotion.setUsageLimit(promocionActualizada.getUsageLimit());
        promotion.setApplicableTo(promocionActualizada.getApplicableTo());

        return promotionRepository.save(promotion);
    }

    public void eliminarPromocion(Integer id) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("Promoci\u00f3n no encontrada");
        }

        Promotion promotion = promocionOpt.get();
        promotion.setDeletedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    public Promotion activarPromocion(Integer id) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("Promoci\u00f3n no encontrada");
        }

        Promotion promotion = promocionOpt.get();
        promotion.setIsActive(true);
        return promotionRepository.save(promotion);
    }

    public Promotion desactivarPromocion(Integer id) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("Promoci\u00f3n no encontrada");
        }

        Promotion promotion = promocionOpt.get();
        promotion.setIsActive(false);
        return promotionRepository.save(promotion);
    }

    public BigDecimal calcularDescuento(String code, BigDecimal orderTotal) {
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);
        
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("C\u00f3digo de promoci\u00f3n no v\u00e1lido");
        }

        return calcularDescuento(promocionOpt.get(), orderTotal);
    }

    public BigDecimal calcularDescuento(Promotion promotion, BigDecimal orderTotal) {
        if (!promotion.canBeUsed()) {
            throw new IllegalArgumentException("Esta promoci\u00f3n no est\u00e1 disponible actualmente");
        }
        
        // Validar compra m\u00ednima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            throw new IllegalArgumentException(
                String.format("Compra m\u00ednima requerida: S/ %.2f. Tu subtotal actual: S/ %.2f", 
                    promotion.getMinPurchase(), orderTotal)
            );
        }

        return promotion.calculateDiscount(orderTotal);
    }
    
    /**
     * Valida si un usuario puede usar una promoci\u00f3n espec\u00edfica
     * Incluye validaci\u00f3n de primera compra
     */
    public Map<String, Object> validarPromocionParaUsuario(String code, BigDecimal orderTotal, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);
        
        if (promocionOpt.isEmpty()) {
            result.put("valid", false);
            result.put("message", "C\u00f3digo de promoci\u00f3n no v\u00e1lido");
            return result;
        }

        Promotion promotion = promocionOpt.get();
        
        // Validar que la promoci\u00f3n est\u00e9 activa
        if (!promotion.canBeUsed()) {
            result.put("valid", false);
            result.put("message", "Esta promoci\u00f3n no est\u00e1 disponible actualmente");
            return result;
        }
        
        // Validar compra m\u00ednima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            result.put("valid", false);
            result.put("message", String.format(
                "Compra m\u00ednima requerida: S/ %.2f. Tu subtotal actual: S/ %.2f", 
                promotion.getMinPurchase(), orderTotal)
            );
            result.put("minPurchase", promotion.getMinPurchase());
            result.put("currentTotal", orderTotal);
            return result;
        }
        
        // Validar si es promoci\u00f3n de primera compra
        String promoName = promotion.getName().toLowerCase();
        if (promoName.contains("primera compra") || promoName.contains("primer pedido")) {
            long orderCount = orderRepository.countByUserId(userId);
            if (orderCount > 0) {
                result.put("valid", false);
                result.put("message", "Esta promoci\u00f3n es solo para nuevos clientes en su primera compra");
                return result;
            }
        }
        
        BigDecimal discount = promotion.calculateDiscount(orderTotal);
        BigDecimal finalTotal = orderTotal.subtract(discount);
        
        result.put("valid", true);
        result.put("discount", discount);
        result.put("finalTotal", finalTotal);
        result.put("message", String.format("Promoci\u00f3n aplicada: -S/ %.2f", discount));
        
        return result;
    }
    
    /**
     * Valida una promoci\u00f3n considerando los items del carrito
     * Calcula el descuento solo sobre los items aplicables
     */
    public Map<String, Object> validarPromocionConItems(
            String code, 
            BigDecimal orderTotal, 
            Integer userId, 
            List<Map<String, Object>> items) {
        
        Map<String, Object> result = new HashMap<>();
        
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);
        
        if (promocionOpt.isEmpty()) {
            result.put("valid", false);
            result.put("message", "C\u00f3digo de promoci\u00f3n no v\u00e1lido");
            return result;
        }

        Promotion promotion = promocionOpt.get();
        
        // Validar que la promoci\u00f3n est\u00e9 activa
        if (!promotion.canBeUsed()) {
            result.put("valid", false);
            result.put("message", "Esta promoci\u00f3n no est\u00e1 disponible actualmente");
            return result;
        }
        
        // Validar compra m\u00ednima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            result.put("valid", false);
            result.put("message", String.format(
                "Compra m\u00ednima requerida: S/ %.2f. Tu subtotal actual: S/ %.2f", 
                promotion.getMinPurchase(), orderTotal)
            );
            result.put("minPurchase", promotion.getMinPurchase());
            result.put("currentTotal", orderTotal);
            return result;
        }
        
        // Validar si es promoci\u00f3n de primera compra
        String promoName = promotion.getName().toLowerCase();
        if (promoName.contains("primera compra") || promoName.contains("primer pedido")) {
            long orderCount = orderRepository.countByUserId(userId);
            if (orderCount > 0) {
                result.put("valid", false);
                result.put("message", "Esta promoci\u00f3n es solo para nuevos clientes en su primera compra");
                return result;
            }
        }
        
        // Calcular descuento solo sobre items aplicables
        BigDecimal discount = promotion.calculateDiscountForItems(items, orderTotal);
        
        // Validar que hay descuento aplicable
        if (discount.compareTo(BigDecimal.ZERO) == 0) {
            String applicableToText = "";
            switch (promotion.getApplicableTo()) {
                case PIZZAS:
                    applicableToText = "pizzas";
                    break;
                case EXTRAS:
                    applicableToText = "extras (bebidas, postres, etc.)";
                    break;
                case SPECIFIC:
                    applicableToText = "productos espec\u00edficos";
                    break;
                default:
                    applicableToText = "productos";
            }
            
            result.put("valid", false);
            result.put("message", String.format(
                "Esta promoci\u00f3n solo aplica a %s. Agrega productos v\u00e1lidos para usar este c\u00f3digo.",
                applicableToText
            ));
            return result;
        }
        
        BigDecimal finalTotal = orderTotal.subtract(discount);
        
        result.put("valid", true);
        result.put("discount", discount);
        result.put("finalTotal", finalTotal);
        result.put("message", String.format("Promoci\u00f3n aplicada: -S/ %.2f", discount));
        result.put("applicableTo", promotion.getApplicableTo().toString());
        
        return result;
    }

    public void incrementarUsoPromocion(String code) {
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);
        if (promocionOpt.isPresent()) {
            incrementarUsoPromocion(promocionOpt.get());
        }
    }

    public void incrementarUsoPromocion(Promotion promotion) {
        promotion.incrementUsageCount();
        promotionRepository.save(promotion);
    }

    public List<Promotion> obtenerPromocionesProximasAVencer(int dias) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(dias);
        return promotionRepository.findExpiringPromotions(now, futureDate);
    }

    public boolean existePorCodigo(String code) {
        return promotionRepository.existsByCodeAndDeletedAtIsNull(code);
    }
}



