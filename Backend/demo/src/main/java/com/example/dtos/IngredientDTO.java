package com.example.dtos;

import java.math.BigDecimal;

import com.example.models.Ingredient;
import com.fasterxml.jackson.annotation.JsonProperty;

public class IngredientDTO {
    private Integer id;
    private String name;
    private BigDecimal extraCost;

    @JsonProperty("isAvailable")
    private boolean isAvailable;

    private String unit;
    private BigDecimal stockQuantity;
    private BigDecimal minStock;
    private BigDecimal unitCost;
    private BigDecimal extraPortionQuantity;

    @JsonProperty("allowAsExtra")
    private Boolean allowAsExtra;

    private boolean lowStock;
    private boolean outOfStock;

    // Constructor vacío
    public IngredientDTO() {}

    // Constructor desde entidad
    public IngredientDTO(Ingredient ingredient) {
        this.id = ingredient.getId();
        this.name = ingredient.getName();
        this.extraCost = ingredient.getExtraCost();
        this.isAvailable = ingredient.isAvailable();
        this.unit = ingredient.getUnit() != null ? ingredient.getUnit() : "g";
        this.stockQuantity = ingredient.getStockQuantity() != null ? ingredient.getStockQuantity() : BigDecimal.ZERO;
        this.minStock = ingredient.getMinStock() != null ? ingredient.getMinStock() : BigDecimal.ZERO;
        this.unitCost = ingredient.getUnitCost() != null ? ingredient.getUnitCost() : BigDecimal.ZERO;
        this.extraPortionQuantity = ingredient.getExtraPortionQuantity() != null ? ingredient.getExtraPortionQuantity() : new BigDecimal("50.00");
        this.allowAsExtra = ingredient.isAllowAsExtra();
        this.outOfStock = this.stockQuantity.compareTo(BigDecimal.ZERO) <= 0;
        this.lowStock = !this.outOfStock && this.stockQuantity.compareTo(this.minStock) <= 0;
    }

    // Getters y setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getExtraCost() { return extraCost; }
    public void setExtraCost(BigDecimal extraCost) { this.extraCost = extraCost; }

    @JsonProperty("isAvailable")
    public boolean getIsAvailable() { return isAvailable; }
    @JsonProperty("isAvailable")
    public void setIsAvailable(boolean isAvailable) { this.isAvailable = isAvailable; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public BigDecimal getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(BigDecimal stockQuantity) { this.stockQuantity = stockQuantity; }

    public BigDecimal getMinStock() { return minStock; }
    public void setMinStock(BigDecimal minStock) { this.minStock = minStock; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getExtraPortionQuantity() { return extraPortionQuantity; }
    public void setExtraPortionQuantity(BigDecimal extraPortionQuantity) { this.extraPortionQuantity = extraPortionQuantity; }

    @JsonProperty("allowAsExtra")
    public Boolean getAllowAsExtra() { return allowAsExtra; }
    @JsonProperty("allowAsExtra")
    public void setAllowAsExtra(Boolean allowAsExtra) { this.allowAsExtra = allowAsExtra; }

    public boolean isLowStock() { return lowStock; }
    public void setLowStock(boolean lowStock) { this.lowStock = lowStock; }

    public boolean isOutOfStock() { return outOfStock; }
    public void setOutOfStock(boolean outOfStock) { this.outOfStock = outOfStock; }
}
