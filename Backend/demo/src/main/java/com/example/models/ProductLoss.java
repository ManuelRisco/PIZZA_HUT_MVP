package com.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_losses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductLoss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType itemType;

    private Integer ingredientId;
    private Integer extraId;
    private Integer pizzaId;
    private Integer sizeId;
    private Integer orderId;

    @Column(nullable = false, length = 150)
    private String itemName;

    @Column(nullable = false, length = 20)
    private String unit = "und";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalLossCost = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LossCategory category;

    @Column(length = 255)
    private String reason;

    private Integer reportedByUserId;

    @Column(nullable = false)
    private LocalDateTime lossDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum ItemType {
        INGREDIENT,
        EXTRA,
        PIZZA,
        SIZE
    }

    public enum LossCategory {
        EXPIRED,
        DAMAGED,
        KITCHEN_ERROR,
        CANCELLED_ORDER,
        OTHER
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (lossDate == null) {
            lossDate = LocalDateTime.now();
        }
        if (unit == null || unit.isBlank()) {
            unit = "und";
        }
        if (unitCost == null) {
            unitCost = BigDecimal.ZERO;
        }
        if (totalLossCost == null) {
            totalLossCost = BigDecimal.ZERO;
        }
    }
}
