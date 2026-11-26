package com.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions", indexes = {
    @Index(name = "idx_promo_active", columnList = "is_active, deletedAt"),
    @Index(name = "idx_promo_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    public enum DiscountType {
        PERCENTAGE,
        FIXED_AMOUNT,
        BUNDLE
    }

    public enum ApplicableTo {
        ALL,
        PIZZAS,
        EXTRAS,
        SPECIFIC
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

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

    public BigDecimal calculateDiscountForItems(java.util.List<java.util.Map<String, Object>> items, BigDecimal orderTotal) {
        if (!canBeUsed()) {
            return BigDecimal.ZERO;
        }

        if (minPurchase != null && orderTotal.compareTo(minPurchase) < 0) {
            return BigDecimal.ZERO;
        }

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
                    isApplicable = false;
                    break;
            }

            if (isApplicable) {
                applicableTotal = applicableTotal.add(unitPrice.multiply(new BigDecimal(quantity)));
            }
        }

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
                discount = discountValue;
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
}
