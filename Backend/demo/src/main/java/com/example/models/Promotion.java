package com.example.models;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promo_active", columnList = "is_active, deletedAt"),
    @Index(name = "idx_promo_dates", columnList = "start_date, end_date")
})
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "final_price", precision = 10, scale = 2)
    private BigDecimal finalPrice;

    @Column(name = "min_purchase", precision = 10, scale = 2)
    private BigDecimal minPurchase;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "applicable_to", nullable = false, length = 20)
    private ApplicableTo applicableTo = ApplicableTo.ALL;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    // ==== Enums ====
    public enum DiscountType {
        PERCENTAGE,     // Descuento porcentual
        FIXED_AMOUNT,   // Monto fijo de descuento
        BUNDLE          // Combo/Paquete a precio especial
    }

    public enum ApplicableTo {
        ALL,       // Aplicable a todo
        PIZZAS,    // Solo pizzas
        EXTRAS,    // Solo extras
        SPECIFIC   // Productos espec\u00edficos definidos en promotion_pizzas/promotion_extras
    }

    // ==== Constructores ====
    public Promotion() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ==== M\u00e9todos de utilidad ====
    public boolean isCurrentlyActive() {
        LocalDateTime now = LocalDateTime.now();
        return isActive && 
               !now.isBefore(startDate) && 
               !now.isAfter(endDate) &&
               deletedAt == null &&
               (usageLimit == null || usageCount < usageLimit);
    }

    public boolean canBeUsed() {
        return isCurrentlyActive();
    }

    public void incrementUsageCount() {
        this.usageCount++;
    }

    public BigDecimal calculateDiscount(BigDecimal orderTotal) {
        if (!canBeUsed()) {
            return BigDecimal.ZERO;
        }

        if (minPurchase != null && orderTotal.compareTo(minPurchase) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        switch (discountType) {
            case PERCENTAGE:
                discount = orderTotal.multiply(discountValue).divide(new BigDecimal("100"));
                if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                    discount = maxDiscount;
                }
                break;
            case FIXED_AMOUNT:
                discount = discountValue;
                if (discount.compareTo(orderTotal) > 0) {
                    discount = orderTotal;
                }
                break;
            case BUNDLE:
                // Para bundles, el descuento se calcula en base al precio final
                if (finalPrice != null) {
                    discount = orderTotal.subtract(finalPrice);
                    if (discount.compareTo(BigDecimal.ZERO) < 0) {
                        discount = BigDecimal.ZERO;
                    }
                }
                break;
        }

        return discount;
    }
    
    /**
     * Calcula el descuento aplicable basado en los items del carrito
     * Solo aplica descuento a los items que corresponden seg\u00fan applicableTo
     */
    public BigDecimal calculateDiscountForItems(java.util.List<java.util.Map<String, Object>> items, BigDecimal orderTotal) {
        if (!canBeUsed()) {
            return BigDecimal.ZERO;
        }

        if (minPurchase != null && orderTotal.compareTo(minPurchase) < 0) {
            return BigDecimal.ZERO;
        }

        // Calcular el total de los items aplicables
        BigDecimal applicableTotal = BigDecimal.ZERO;
        
        for (java.util.Map<String, Object> item : items) {
            String itemType = (String) item.get("type");
            Object unitPriceObj = item.get("unitPrice");
            Object quantityObj = item.get("quantity");
            
            BigDecimal unitPrice = unitPriceObj instanceof Number ? 
                new BigDecimal(unitPriceObj.toString()) : BigDecimal.ZERO;
            int quantity = quantityObj instanceof Number ? 
                ((Number) quantityObj).intValue() : 0;
            
            boolean isApplicable = false;
            
            switch (applicableTo) {
                case ALL:
                    isApplicable = true;
                    break;
                case PIZZAS:
                    isApplicable = "pizza".equalsIgnoreCase(itemType);
                    break;
                case EXTRAS:
                    isApplicable = "extra".equalsIgnoreCase(itemType);
                    break;
                case SPECIFIC:
                    // Para productos espec\u00edficos, se necesitar\u00eda una l\u00f3gica adicional
                    isApplicable = false;
                    break;
            }
            
            if (isApplicable) {
                applicableTotal = applicableTotal.add(unitPrice.multiply(new BigDecimal(quantity)));
            }
        }
        
        // Si no hay items aplicables, no hay descuento
        if (applicableTotal.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        switch (discountType) {
            case PERCENTAGE:
                discount = applicableTotal.multiply(discountValue).divide(new BigDecimal("100"));
                if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
                    discount = maxDiscount;
                }
                break;
            case FIXED_AMOUNT:
                // Para descuento fijo, aplicar solo si hay items aplicables
                discount = discountValue;
                // No puede ser mayor al total aplicable
                if (discount.compareTo(applicableTotal) > 0) {
                    discount = applicableTotal;
                }
                break;
            case BUNDLE:
                if (finalPrice != null) {
                    discount = applicableTotal.subtract(finalPrice);
                    if (discount.compareTo(BigDecimal.ZERO) < 0) {
                        discount = BigDecimal.ZERO;
                    }
                }
                break;
        }

        return discount;
    }

    // ==== Getters y Setters ====
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DiscountType getDiscountType() {
        return discountType;
    }

    public void setDiscountType(DiscountType discountType) {
        this.discountType = discountType;
    }

    public BigDecimal getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(BigDecimal discountValue) {
        this.discountValue = discountValue;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }

    public BigDecimal getMinPurchase() {
        return minPurchase;
    }

    public void setMinPurchase(BigDecimal minPurchase) {
        this.minPurchase = minPurchase;
    }

    public BigDecimal getMaxDiscount() {
        return maxDiscount;
    }

    public void setMaxDiscount(BigDecimal maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public ApplicableTo getApplicableTo() {
        return applicableTo;
    }

    public void setApplicableTo(ApplicableTo applicableTo) {
        this.applicableTo = applicableTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
}
