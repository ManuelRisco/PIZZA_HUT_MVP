package com.example.dtos;

import java.math.BigDecimal;
import java.util.List;

public class InventorySummaryDTO {
    private BigDecimal totalInventoryValue;
    private BigDecimal ingredientsInventoryValue;
    private BigDecimal extrasInventoryValue;
    private int totalIngredients;
    private int totalExtras;
    private int lowStockCount;
    private int outOfStockCount;
    private List<AlertItemDTO> alerts;

    public static class AlertItemDTO {
        private String itemType; // "INGREDIENT" o "EXTRA"
        private Integer itemId;
        private String name;
        private String unit;
        private BigDecimal stockQuantity;
        private BigDecimal minStock;
        private BigDecimal unitCost;
        private String status; // "LOW_STOCK" o "OUT_OF_STOCK"

        public AlertItemDTO() {}

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public Integer getItemId() { return itemId; }
        public void setItemId(Integer itemId) { this.itemId = itemId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public BigDecimal getStockQuantity() { return stockQuantity; }
        public void setStockQuantity(BigDecimal stockQuantity) { this.stockQuantity = stockQuantity; }

        public BigDecimal getMinStock() { return minStock; }
        public void setMinStock(BigDecimal minStock) { this.minStock = minStock; }

        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public InventorySummaryDTO() {}

    public BigDecimal getTotalInventoryValue() { return totalInventoryValue; }
    public void setTotalInventoryValue(BigDecimal totalInventoryValue) { this.totalInventoryValue = totalInventoryValue; }

    public BigDecimal getIngredientsInventoryValue() { return ingredientsInventoryValue; }
    public void setIngredientsInventoryValue(BigDecimal ingredientsInventoryValue) { this.ingredientsInventoryValue = ingredientsInventoryValue; }

    public BigDecimal getExtrasInventoryValue() { return extrasInventoryValue; }
    public void setExtrasInventoryValue(BigDecimal extrasInventoryValue) { this.extrasInventoryValue = extrasInventoryValue; }

    public int getTotalIngredients() { return totalIngredients; }
    public void setTotalIngredients(int totalIngredients) { this.totalIngredients = totalIngredients; }

    public int getTotalExtras() { return totalExtras; }
    public void setTotalExtras(int totalExtras) { this.totalExtras = totalExtras; }

    public int getLowStockCount() { return lowStockCount; }
    public void setLowStockCount(int lowStockCount) { this.lowStockCount = lowStockCount; }

    public int getOutOfStockCount() { return outOfStockCount; }
    public void setOutOfStockCount(int outOfStockCount) { this.outOfStockCount = outOfStockCount; }

    public List<AlertItemDTO> getAlerts() { return alerts; }
    public void setAlerts(List<AlertItemDTO> alerts) { this.alerts = alerts; }
}

