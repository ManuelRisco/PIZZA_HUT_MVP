package com.example.infrastructure.persistence.entities;

import com.example.domain.model.OrderItemExtra;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_item_extras")
public class OrderItemExtraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer orderItemId;

    @Column(nullable = false)
    private Integer ingredientId;

    @Column(nullable = false, length = 100)
    private String ingredientName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal extraCost = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ==== Conversión a Domain Model ====
    public OrderItemExtra toDomain() {
        return new OrderItemExtra(
            this.id,
            this.orderItemId,
            this.ingredientId,
            this.ingredientName,
            this.extraCost,
            this.createdAt
        );
    }

    // ==== Conversión desde Domain Model ====
    public static OrderItemExtraEntity fromDomain(OrderItemExtra orderItemExtra) {
        OrderItemExtraEntity entity = new OrderItemExtraEntity();
        entity.id = orderItemExtra.getId();
        entity.orderItemId = orderItemExtra.getOrderItemId();
        entity.ingredientId = orderItemExtra.getIngredientId();
        entity.ingredientName = orderItemExtra.getIngredientName();
        entity.extraCost = orderItemExtra.getExtraCost();
        entity.createdAt = orderItemExtra.getCreatedAt();
        return entity;
    }

    // ==== Getters y Setters ====
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Integer orderItemId) {
        this.orderItemId = orderItemId;
    }

    public Integer getIngredientId() {
        return ingredientId;
    }

    public void setIngredientId(Integer ingredientId) {
        this.ingredientId = ingredientId;
    }

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public BigDecimal getExtraCost() {
        return extraCost;
    }

    public void setExtraCost(BigDecimal extraCost) {
        this.extraCost = extraCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
