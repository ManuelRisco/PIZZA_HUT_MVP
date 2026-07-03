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

    private static final String MSG_PROMO_NOT_FOUND = "Promoción no encontrada";
    private static final String MSG_INVALID_CODE = "Código de promoción no válido";
    private static final String MSG_PROMO_NOT_AVAILABLE = "Esta promoción no está disponible actualmente";
    private static final String MSG_NEW_CUSTOMERS_ONLY = "Esta promoción es solo para nuevos clientes en su primera compra";
    private static final String MSG_MIN_PURCHASE = "Compra mínima requerida: S/ %.2f. Tu subtotal actual: S/ %.2f";
    private static final String KEYWORD_FIRST_PURCHASE = "primera compra";
    private static final String KEYWORD_FIRST_ORDER = "primer pedido";
    private static final String KEY_VALID = "valid";
    private static final String KEY_MESSAGE = "message";

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
            throw new IllegalArgumentException(MSG_PROMO_NOT_FOUND);
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
            throw new IllegalArgumentException(MSG_PROMO_NOT_FOUND);
        }

        Promotion promotion = promocionOpt.get();
        promotion.setDeletedAt(LocalDateTime.now());
        promotionRepository.save(promotion);
    }

    public Promotion activarPromocion(Integer id) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException(MSG_PROMO_NOT_FOUND);
        }

        Promotion promotion = promocionOpt.get();
        promotion.setIsActive(true);
        return promotionRepository.save(promotion);
    }

    public Promotion desactivarPromocion(Integer id) {
        Optional<Promotion> promocionOpt = obtenerPorId(id);
        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException(MSG_PROMO_NOT_FOUND);
        }

        Promotion promotion = promocionOpt.get();
        promotion.setIsActive(false);
        return promotionRepository.save(promotion);
    }

    public BigDecimal calcularDescuento(String code, BigDecimal orderTotal) {
        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);

        if (promocionOpt.isEmpty()) {
            throw new IllegalArgumentException(MSG_INVALID_CODE);
        }

        return calcularDescuento(promocionOpt.get(), orderTotal);
    }

    public BigDecimal calcularDescuento(Promotion promotion, BigDecimal orderTotal) {
        if (!promotion.canBeUsed()) {
            throw new IllegalArgumentException(MSG_PROMO_NOT_AVAILABLE);
        }

        // Validar compra mínima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            throw new IllegalArgumentException(
                    String.format(MSG_MIN_PURCHASE,
                            promotion.getMinPurchase(), orderTotal));
        }

        return promotion.calculateDiscount(orderTotal);
    }

    /**
     * Verifica reglas de negocio que dependen del usuario (ej. Primera Compra).
     * Lanza IllegalArgumentException si no se cumplen.
     */
    public void verificarReglasDeUsuario(Promotion promotion, Integer userId) {
        if (userId == null)
            return;

        String promoName = promotion.getName().toLowerCase();
        if (promoName.contains(KEYWORD_FIRST_PURCHASE) || promoName.contains(KEYWORD_FIRST_ORDER)) {
            long orderCount = orderRepository.countByUserId(userId);
            if (orderCount > 0) {
                throw new IllegalArgumentException(MSG_NEW_CUSTOMERS_ONLY);
            }
        }
    }

    /**
     * Valida si un usuario puede usar una promocion especifica
     * Incluye validacion de primera compra
     */
    public Map<String, Object> validarPromocionParaUsuario(String code, BigDecimal orderTotal, Integer userId) {
        Map<String, Object> result = new HashMap<>();

        Optional<Promotion> promocionOpt = obtenerPorCodigo(code);

        if (promocionOpt.isEmpty()) {
            result.put(KEY_VALID, false);
            result.put(KEY_MESSAGE, MSG_INVALID_CODE);
            return result;
        }

        Promotion promotion = promocionOpt.get();

        // Validar que la promoción esté activa
        if (!promotion.canBeUsed()) {
            result.put(KEY_VALID, false);
            result.put(KEY_MESSAGE, MSG_PROMO_NOT_AVAILABLE);
            return result;
        }

        // Validar compra mínima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            result.put(KEY_VALID, false);
            result.put(KEY_MESSAGE, String.format(
                    MSG_MIN_PURCHASE,
                    promotion.getMinPurchase(), orderTotal));
            result.put("minPurchase", promotion.getMinPurchase());
            result.put("currentTotal", orderTotal);
            return result;
        }

        // Validar si es promoción de primera compra
        String promoName = promotion.getName().toLowerCase();
        if (promoName.contains(KEYWORD_FIRST_PURCHASE) || promoName.contains(KEYWORD_FIRST_ORDER)) {
            long orderCount = orderRepository.countByUserId(userId);
            if (orderCount > 0) {
                result.put(KEY_VALID, false);
                result.put(KEY_MESSAGE, MSG_NEW_CUSTOMERS_ONLY);
                return result;
            }
        }

        BigDecimal discount = promotion.calculateDiscount(orderTotal);
        BigDecimal finalTotal = orderTotal.subtract(discount);

        result.put(KEY_VALID, true);
        result.put("discount", discount);
        result.put("finalTotal", finalTotal);
        result.put(KEY_MESSAGE, String.format("Promoción aplicada: -S/ %.2f", discount));

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
            result.put(KEY_VALID, false);
            result.put(KEY_MESSAGE, MSG_INVALID_CODE);
            return result;
        }

        Promotion promotion = promocionOpt.get();

        // Validar que la promoción esté activa
        if (!promotion.canBeUsed()) {
            result.put(KEY_VALID, false);
            result.put(KEY_MESSAGE, MSG_PROMO_NOT_AVAILABLE);
            return result;
        }

        // Validar compra mínima
        if (promotion.getMinPurchase() != null && orderTotal.compareTo(promotion.getMinPurchase()) < 0) {
            result.put(KEY_VALID, false);
            result.put(KEY_MESSAGE, String.format(
                    MSG_MIN_PURCHASE,
                    promotion.getMinPurchase(), orderTotal));
            result.put("minPurchase", promotion.getMinPurchase());
            result.put("currentTotal", orderTotal);
            return result;
        }

        // Validar si es promoción de primera compra
        String promoName = promotion.getName().toLowerCase();
        if (promoName.contains(KEYWORD_FIRST_PURCHASE) || promoName.contains(KEYWORD_FIRST_ORDER)) {
            long orderCount = orderRepository.countByUserId(userId);
            if (orderCount > 0) {
                result.put(KEY_VALID, false);
                result.put(KEY_MESSAGE, MSG_NEW_CUSTOMERS_ONLY);
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

            result.put(KEY_VALID, false);
            result.put(KEY_MESSAGE, String.format(
                    "Esta promoción solo aplica a %s. Agrega productos válidos para usar este código.",
                    applicableToText));
            return result;
        }

        BigDecimal finalTotal = orderTotal.subtract(discount);

        result.put(KEY_VALID, true);
        result.put("discount", discount);
        result.put("finalTotal", finalTotal);
        result.put(KEY_MESSAGE, String.format("Promoción aplicada: -S/ %.2f", discount));
        result.put("applicableTo", promotion.getApplicableTo().toString());

        return result;
    }

    @org.springframework.transaction.annotation.Transactional
    public void incrementarUsoPromocion(String code) {
        // Bloqueo pesimista para evitar race conditions en uso de promos
        Optional<Promotion> promocionOpt = promotionRepository.findByCodeAndDeletedAtIsNullForUpdate(code);
        if (promocionOpt.isPresent()) {
            Promotion promotion = promocionOpt.get();
            // Re-validar si todavía puede usarse DESPUÉS de adquirir el lock
            if (!promotion.canBeUsed()) {
                throw new IllegalStateException(
                        "La promoción alcanzó su límite de uso mientras se procesaba el pedido.");
            }
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
        return promotionRepository.existsByCodeAndDeletedAtIsNull(code);
    }
}
