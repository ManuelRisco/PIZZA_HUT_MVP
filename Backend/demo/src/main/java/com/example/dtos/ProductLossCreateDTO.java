package com.example.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductLossCreateDTO {
    private String itemType; // "INGREDIENT", "EXTRA", "PIZZA"
    private Integer ingredientId;
    private Integer extraId;
    private Integer pizzaId;
    private Integer sizeId;
    private BigDecimal quantity;
    private String category; // "EXPIRED", "DAMAGED", "KITCHEN_ERROR", "CANCELLED_ORDER", "OTHER"
    private String reason;
    private Boolean deductStock = true;
    private LocalDateTime lossDate;

    public ProductLossCreateDTO() {}

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public Integer getIngredientId() { return ingredientId; }
    public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

    public Integer getExtraId() { return extraId; }
    public void setExtraId(Integer extraId) { this.extraId = extraId; }

    public Integer getPizzaId() { return pizzaId; }
    public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

    public Integer getSizeId() { return sizeId; }
    public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Boolean getDeductStock() { return deductStock; }
    public void setDeductStock(Boolean deductStock) { this.deductStock = deductStock; }

    public LocalDateTime getLossDate() { return lossDate; }
    public void setLossDate(LocalDateTime lossDate) { this.lossDate = lossDate; }
}

