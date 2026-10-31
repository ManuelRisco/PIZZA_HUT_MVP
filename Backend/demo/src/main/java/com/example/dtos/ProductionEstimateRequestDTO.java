package com.example.dtos;

import java.util.List;

public class ProductionEstimateRequestDTO {
    private List<BatchItemRequest> items;

    public static class BatchItemRequest {
        private String itemType; // "PIZZA" o "EXTRA"
        private Integer pizzaId;
        private Integer sizeId;
        private Integer extraId;
        private List<Integer> extraIngredientIds;
        private Integer quantity;

        public BatchItemRequest() {}

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

        public Integer getSizeId() { return sizeId; }
        public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

        public Integer getExtraId() { return extraId; }
        public void setExtraId(Integer extraId) { this.extraId = extraId; }

        public List<Integer> getExtraIngredientIds() { return extraIngredientIds; }
        public void setExtraIngredientIds(List<Integer> extraIngredientIds) { this.extraIngredientIds = extraIngredientIds; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public ProductionEstimateRequestDTO() {}

    public List<BatchItemRequest> getItems() { return items; }
    public void setItems(List<BatchItemRequest> items) { this.items = items; }
}

