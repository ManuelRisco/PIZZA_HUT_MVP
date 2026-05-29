package com.example.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderItemExtra {

    private Integer id;
    private Integer orderItemId;
    private Integer ingredientId;
    private String ingredientName;
    private BigDecimal extraCost;
    private LocalDateTime createdAt;

    // ==== Constructores ====
    public OrderItemExtra() {}

    public OrderItemExtra(Integer id, Integer orderItemId, Integer ingredientId,
                          String ingredientName, BigDecimal extraCost, LocalDateTime createdAt) {
        this.id = id;
        this.orderItemId = orderItemId;
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName;
        this.extraCost = extraCost;
        this.createdAt = createdAt;
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
