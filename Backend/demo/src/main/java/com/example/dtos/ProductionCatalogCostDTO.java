package com.example.dtos;

import java.math.BigDecimal;
import java.util.List;

public class ProductionCatalogCostDTO {
    private List<PizzaSizeCostDTO> pizzaCosts;
    private List<ExtraCostDTO> extraCosts;

    public static class PizzaSizeCostDTO {
        private Integer pizzaId;
        private String pizzaName;
        private String categoryName;
        private String imageUrl;
        private boolean isAvailable;
        private Integer sizeId;
        private String sizeName;
        private BigDecimal recipeMultiplier;
        private BigDecimal salePrice;
        private BigDecimal directCost;
        private BigDecimal unitProfit;
        private BigDecimal profitMargin;
        private int maxProducibleUnits;
        private List<RecipeLineCostDTO> ingredients;

        public PizzaSizeCostDTO() {}

        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

        public String getPizzaName() { return pizzaName; }
        public void setPizzaName(String pizzaName) { this.pizzaName = pizzaName; }

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public boolean isAvailable() { return isAvailable; }
        public void setAvailable(boolean available) { isAvailable = available; }

        public Integer getSizeId() { return sizeId; }
        public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

        public String getSizeName() { return sizeName; }
        public void setSizeName(String sizeName) { this.sizeName = sizeName; }

        public BigDecimal getRecipeMultiplier() { return recipeMultiplier; }
        public void setRecipeMultiplier(BigDecimal recipeMultiplier) { this.recipeMultiplier = recipeMultiplier; }

        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

        public BigDecimal getDirectCost() { return directCost; }
        public void setDirectCost(BigDecimal directCost) { this.directCost = directCost; }

        public BigDecimal getUnitProfit() { return unitProfit; }
        public void setUnitProfit(BigDecimal unitProfit) { this.unitProfit = unitProfit; }

        public BigDecimal getProfitMargin() { return profitMargin; }
        public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }

        public int getMaxProducibleUnits() { return maxProducibleUnits; }
        public void setMaxProducibleUnits(int maxProducibleUnits) { this.maxProducibleUnits = maxProducibleUnits; }

        public List<RecipeLineCostDTO> getIngredients() { return ingredients; }
        public void setIngredients(List<RecipeLineCostDTO> ingredients) { this.ingredients = ingredients; }
    }

    public static class RecipeLineCostDTO {
        private Integer ingredientId;
        private String ingredientName;
        private String unit;
        private BigDecimal quantityPerUnit;
        private BigDecimal unitCost;
        private BigDecimal lineCost;

        public RecipeLineCostDTO() {}

        public Integer getIngredientId() { return ingredientId; }
        public void setIngredientId(Integer ingredientId) { this.ingredientId = ingredientId; }

        public String getIngredientName() { return ingredientName; }
        public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }

        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        public BigDecimal getQuantityPerUnit() { return quantityPerUnit; }
        public void setQuantityPerUnit(BigDecimal quantityPerUnit) { this.quantityPerUnit = quantityPerUnit; }

        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

        public BigDecimal getLineCost() { return lineCost; }
        public void setLineCost(BigDecimal lineCost) { this.lineCost = lineCost; }
    }

    public static class ExtraCostDTO {
        private Integer extraId;
        private String name;
        private String category;
        private String imageUrl;
        private boolean isAvailable;
        private BigDecimal salePrice;
        private BigDecimal directCost;
        private BigDecimal unitProfit;
        private BigDecimal profitMargin;
        private int availableStock;

        public ExtraCostDTO() {}

        public Integer getExtraId() { return extraId; }
        public void setExtraId(Integer extraId) { this.extraId = extraId; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public boolean isAvailable() { return isAvailable; }
        public void setAvailable(boolean available) { isAvailable = available; }

        public BigDecimal getSalePrice() { return salePrice; }
        public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }

        public BigDecimal getDirectCost() { return directCost; }
        public void setDirectCost(BigDecimal directCost) { this.directCost = directCost; }

        public BigDecimal getUnitProfit() { return unitProfit; }
        public void setUnitProfit(BigDecimal unitProfit) { this.unitProfit = unitProfit; }

        public BigDecimal getProfitMargin() { return profitMargin; }
        public void setProfitMargin(BigDecimal profitMargin) { this.profitMargin = profitMargin; }

        public int getAvailableStock() { return availableStock; }
        public void setAvailableStock(int availableStock) { this.availableStock = availableStock; }
    }

    public ProductionCatalogCostDTO() {}

    public List<PizzaSizeCostDTO> getPizzaCosts() { return pizzaCosts; }
    public void setPizzaCosts(List<PizzaSizeCostDTO> pizzaCosts) { this.pizzaCosts = pizzaCosts; }

    public List<ExtraCostDTO> getExtraCosts() { return extraCosts; }
    public void setExtraCosts(List<ExtraCostDTO> extraCosts) { this.extraCosts = extraCosts; }
}

