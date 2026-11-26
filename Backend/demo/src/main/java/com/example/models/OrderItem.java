package com.example.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer orderId;
    private Integer pizzaId;
    private Integer extraId;
    @Column(name = "item_type")
    private String itemType;
    private Integer sizeId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal sizeExtra;
    private BigDecimal lineTotal;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal lineCost = BigDecimal.ZERO;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public OrderItem(Integer id, Integer orderId, Integer pizzaId, Integer extraId,
            String itemType, Integer sizeId, Integer quantity, BigDecimal unitPrice,
            BigDecimal sizeExtra, BigDecimal lineTotal,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.orderId = orderId;
        this.pizzaId = pizzaId;
        this.extraId = extraId;
        this.itemType = itemType;
        this.sizeId = sizeId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.sizeExtra = sizeExtra;
        this.lineTotal = lineTotal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (sizeExtra == null) sizeExtra = BigDecimal.ZERO;
        if (unitCost == null) unitCost = BigDecimal.ZERO;
        if (lineCost == null) lineCost = BigDecimal.ZERO;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
