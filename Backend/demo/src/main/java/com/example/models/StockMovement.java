package com.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ItemType itemType;

    private Integer ingredientId;
    private Integer extraId;
    private Integer sizeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MovementType movementType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal previousStock;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal newStock;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost = BigDecimal.ZERO;

    @Column(length = 30)
    private String referenceType;

    private Integer referenceId;

    @Column(length = 255)
    private String reason;

    private Integer userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum ItemType {
        INGREDIENT,
        EXTRA,
        SIZE
    }

    public enum MovementType {
        PURCHASE,
        SALE,
        LOSS,
        ADJUSTMENT,
        RETURN
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (unitCost == null) {
            unitCost = BigDecimal.ZERO;
        }
        if (totalCost == null) {
            totalCost = BigDecimal.ZERO;
        }
    }
}
