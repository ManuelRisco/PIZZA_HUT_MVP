package com.example.dtos;

import java.math.BigDecimal;
import java.util.List;

public class ProductionEstimateResponseDTO {
    private int totalUnits;
    private BigDecimal totalProductionCost;
    private BigDecimal projectedRevenue;
    private BigDecimal projectedGrossProfit;
    private BigDecimal projectedMarginPercent;
    private BigDecimal totalMissingPurchaseCost;
    private boolean canProduceWithCurrentStock;
    private List<BatchLineEstimateDTO> lines;
    private List<MaterialRequirementDTO> materials;

    public static class BatchLineEstimateDTO {
        private String itemType;
        private String itemName;
        private int quantity;
        private BigDecimal unitDirectCost;
        private BigDecimal totalDirectCost;
        private BigDecimal unitSalePrice;
        private BigDecimal projectedRevenue;
        private BigDecimal projectedGrossProfit;
        private BigDecimal profitMargin;

        public BatchLineEstimateDTO() {}

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public BigDecimal getUnitDirectCost() { return unitDirectCost; }
        public void setUnitDirectCost(BigDecimal unitDirectCost) { this.unitDirectCost = unitDirectCost; }

        public BigDecimal getTotalDirectCost() { return totalDirectCost; }
        public void setTotalDirectCost(BigDecimal totalDirectCost) { this.totalDirectCost = totalDirectCost; }

        public BigDecimal getUnitSalePrice() { return unitSalePrice; }
        public void setUnitSalePrice(BigDecimal unitSalePrice) { this.unitSalePrice = unitSalePrice; }

        public BigDecimal getProjectedRevenue() { return projectedRevenue; }
        public void setProjectedRevenue(BigDecimal projectedRevenue) { this.projectedRevenue = projectedRevenue; }

        public BigDecimal getProjectedGrossProfit() { return projectedGrossProfit; }
        public void setProjectedGrossProfit(BigDecimal projectedGrossProfit) { this.projectedGrossProfit = projectedGrossProfit; }

        public BigDecimal getProfitMargin() { return profitMargin; }
        public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }
    }

    public static class MaterialRequirementDTO {
        private String itemType; // "INGREDIENT" o "EXTRA"
        private Integer itemId;
        private String name;
        private String unit;
        private BigDecimal unitCost;
        private BigDecimal requiredQuantity;
        private BigDecimal availableStock;
        private BigDecimal missingQuantity;
        private BigDecimal totalDirectCost;
        private BigDecimal missingPurchaseCost;
        private boolean sufficient;

        public MaterialRequirementDTO() {}

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public Integer getItemId() { return itemId; }
        public void setItemId(Integer itemId) { this.itemId = itemId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

        public BigDecimal getRequiredQuantity() { return requiredQuantity; }
        public void setRequiredQuantity(BigDecimal requiredQuantity) { this.requiredQuantity = requiredQuantity; }

        public BigDecimal getAvailableStock() { return availableStock; }
        public void setAvailableStock(BigDecimal availableStock) { this.availableStock = availableStock; }

        public BigDecimal getMissingQuantity() { return missingQuantity; }
        public void setMissingQuantity(BigDecimal missingQuantity) { this.missingQuantity = missingQuantity; }

        public BigDecimal getTotalDirectCost() { return totalDirectCost; }
        public void setTotalDirectCost(BigDecimal totalDirectCost) { this.totalDirectCost = totalDirectCost; }

        public BigDecimal getMissingPurchaseCost() { return missingPurchaseCost; }
        public void setMissingPurchaseCost(BigDecimal missingPurchaseCost) { this.missingPurchaseCost = missingPurchaseCost; }

        public boolean isSufficient() { return sufficient; }
        public void setSufficient(boolean sufficient) { this.sufficient = sufficient; }
    }

    public ProductionEstimateResponseDTO() {}

    public int getTotalUnits() { return totalUnits; }
    public void setTotalUnits(int totalUnits) { this.totalUnits = totalUnits; }

    public BigDecimal getTotalProductionCost() { return totalProductionCost; }
    public void setTotalProductionCost(BigDecimal totalProductionCost) { this.totalProductionCost = totalProductionCost; }

    public BigDecimal getProjectedRevenue() { return projectedRevenue; }
    public void setProjectedRevenue(BigDecimal projectedRevenue) { this.projectedRevenue = projectedRevenue; }

    public BigDecimal getProjectedGrossProfit() { return projectedGrossProfit; }
    public void setProjectedGrossProfit(BigDecimal projectedGrossProfit) { this.projectedGrossProfit = projectedGrossProfit; }

    public BigDecimal getProjectedMarginPercent() { return projectedMarginPercent; }
    public void setProjectedMarginPercent(BigDecimal projectedMarginPercent) { this.projectedMarginPercent = projectedMarginPercent; }

    public BigDecimal getTotalMissingPurchaseCost() { return totalMissingPurchaseCost; }
    public void setTotalMissingPurchaseCost(BigDecimal totalMissingPurchaseCost) { this.totalMissingPurchaseCost = totalMissingPurchaseCost; }

    public boolean isCanProduceWithCurrentStock() { return canProduceWithCurrentStock; }
    public void setCanProduceWithCurrentStock(boolean canProduceWithCurrentStock) { this.canProduceWithCurrentStock = canProduceWithCurrentStock; }

    public List<BatchLineEstimateDTO> getLines() { return lines; }
    public void setLines(List<BatchLineEstimateDTO> lines) { this.lines = lines; }

    public List<MaterialRequirementDTO> getMaterials() { return materials; }
    public void setMaterials(List<MaterialRequirementDTO> materials) { this.materials = materials; }
}

