package com.example.dtos;

import com.example.models.OrderItemExtra;
import java.math.BigDecimal;

public class OrderItemExtraDTO {

    private Integer id;
    private Integer orderItemId;
    private Integer ingredientId;
    private String ingredientName;
    private BigDecimal extraCost;

    // ==== Constructores ====
    public OrderItemExtraDTO() {}

    public OrderItemExtraDTO(OrderItemExtra orderItemExtra) {
        this.id = orderItemExtra.getId();
        this.orderItemId = orderItemExtra.getOrderItemId();
        this.ingredientId = orderItemExtra.getIngredientId();
        this.ingredientName = orderItemExtra.getIngredientName();
        this.extraCost = orderItemExtra.getExtraCost();
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
}
