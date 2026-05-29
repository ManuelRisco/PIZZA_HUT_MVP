package com.example.service;

import com.example.domain.model.Promotion;
import com.example.domain.repository.PromotionRepository;
import com.example.domain.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PromotionService {

    @Autowired
    private PromotionRepository promotionRepository;
    
    @Autowired
    private OrderRepository orderRepository;

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
        if (promotionRepository.existsByCode(promotion.getCode())) {
            throw new IllegalArgumentException("Ya existe una promoción con ese código");
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
            throw new IllegalArgumentException("Promoción no encontrada");
        }

        Promotion promotion = promocionOpt.get();
        promotion.setDeletedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    public Promotion activarPromocion(Integer id) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("Promoción no encontrada");
        }

        Promotion promotion = promocionOpt.get();
        promotion.setIsActive(true);
        return promotionRepository.save(promotion);
    }

    public Promotion desactivarPromocion(Integer id) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("Promoción no encontrada");
        }

        Promotion promotion = promocionOpt.get();
        promotion.setIsActive(false);
        return promotionRepository.save(promotion);
    }

    public BigDecimal calcularDescuento(String code, BigDecimal orderTotal) {
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);
        
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException("Código de promoción no válido");
        }

        Promotion promotion = promocionOpt.get();
        
        if (!promotion.canBeUsed()) {
            throw new IllegalArgumentException("Esta promoción no está disponible actualmente");
        }
        
        // Validar compra mínima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            throw new IllegalArgumentException(
                String.format("Compra mínima requerida: S/ %.2f. Tu subtotal actual: S/ %.2f", 
                    promotion.getMinPurchase(), orderTotal)
            );
        }

        return promotion.calculateDiscount(orderTotal);
    }
    
    /**
     * Valida si un usuario puede usar una promoción específica
     * Incluye validación de primera compra
     */
    public Map<String, Object> validarPromocionParaUsuario(String code, BigDecimal orderTotal, Integer userId) {
        Map<String, Object> result = new HashMap<>();
        
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);
        
        if (promocionOpt.isEmpty()) {
            result.put("valid", false);
            result.put("message", "Código de promoción no válido");
            return result;
        }

        Promotion promotion = promocionOpt.get();
        
        // Validar que la promoción esté activa
        if (!promotion.canBeUsed()) {
            result.put("valid", false);
            result.put("message", "Esta promoción no está disponible actualmente");
            return result;
        }
        
        // Validar compra mínima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            result.put("valid", false);
            result.put("message", String.format(
                "Compra mínima requerida: S/ %.2f. Tu subtotal actual: S/ %.2f", 
                promotion.getMinPurchase(), orderTotal)
            );
            result.put("minPurchase", promotion.getMinPurchase());
            result.put("currentTotal", orderTotal);
            return result;
        }
        
        // Validar si es promoción de primera compra
        String promoName = promotion.getName().toLowerCase();
        if (promoName.contains("primera compra") || promoName.contains("primer pedido")) {
            long orderCount = orderRepository.countByUserId(userId);
            if (orderCount > 0) {
                result.put("valid", false);
                result.put("message", "Esta promoción es solo para nuevos clientes en su primera compra");
                return result;
            }
        }
        
        BigDecimal discount = promotion.calculateDiscount(orderTotal);
        BigDecimal finalTotal = orderTotal.subtract(discount);
        
        result.put("valid", true);
        result.put("discount", discount);
        result.put("finalTotal", finalTotal);
        result.put("message", String.format("Promoción aplicada: -S/ %.2f", discount));
        
        return result;
    }
    
    /**
     * Valida una promoción considerando los items del carrito
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
            result.put("message", "Código de promoción no válido");
            return result;
        }

        Promotion promotion = promocionOpt.get();
        
        // Validar que la promoción esté activa
        if (!promotion.canBeUsed()) {
            result.put("valid", false);
            result.put("message", "Esta promoción no está disponible actualmente");
            return result;
        }
        
        // Validar compra mínima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            result.put("valid", false);
            result.put("message", String.format(
                "Compra mínima requerida: S/ %.2f. Tu subtotal actual: S/ %.2f", 
                promotion.getMinPurchase(), orderTotal)
            );
            result.put("minPurchase", promotion.getMinPurchase());
            result.put("currentTotal", orderTotal);
            return result;
        }
        
        // Validar si es promoción de primera compra
        String promoName = promotion.getName().toLowerCase();
        if (promoName.contains("primera compra") || promoName.contains("primer pedido")) {
            long orderCount = orderRepository.countByUserId(userId);
            if (orderCount > 0) {
                result.put("valid", false);
                result.put("message", "Esta promoción es solo para nuevos clientes en su primera compra");
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
                    applicableToText = "productos específicos";
                    break;
                default:
                    applicableToText = "productos";
            }
            
            result.put("valid", false);
            result.put("message", String.format(
                "Esta promoción solo aplica a %s. Agrega productos válidos para usar este código.",
                applicableToText
            ));
            return result;
        }
        
        BigDecimal finalTotal = orderTotal.subtract(discount);
        
        result.put("valid", true);
        result.put("discount", discount);
        result.put("finalTotal", finalTotal);
        result.put("message", String.format("Promoción aplicada: -S/ %.2f", discount));
        result.put("applicableTo", promotion.getApplicableTo().toString());
        
        return result;
    }

    public void incrementarUsoPromocion(String code) {
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);
        if (promocionOpt.isPresent()) {
            Promotion promotion = promocionOpt.get();
            promotion.incrementUsageCount();
            promotionRepository.save(promotion);
        }
    }

    public List<Promotion> obtenerPromocionesProximasAVencer(int dias) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(dias);
        return promotionRepository.findExpiringPromotions(now, futureDate);
    }

    public boolean existePorCodigo(String code) {
        return promotionRepository.existsByCode(code);
    }
}
