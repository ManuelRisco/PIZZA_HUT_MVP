package com.example.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderLossCandidateDTO {
    private Integer orderId;
    private String status;
    private String deliveryType;
    private String stockSettled;
    private String customerName;
    private String customerEmail;
    private BigDecimal total;
    private String notes;
    private LocalDateTime createdAt;
    private List<OrderLossCandidateItemDTO> items;

    public OrderLossCandidateDTO() {}

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getStockSettled() { return stockSettled; }
    public void setStockSettled(String stockSettled) { this.stockSettled = stockSettled; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderLossCandidateItemDTO> getItems() { return items; }
    public void setItems(List<OrderLossCandidateItemDTO> items) { this.items = items; }

    public static class OrderLossCandidateItemDTO {
        private Integer orderItemId;
        private String itemType;
        private Integer pizzaId;
        private Integer extraId;
        private Integer sizeId;
        private String itemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private BigDecimal unitCost;
        private BigDecimal lineCost;
        private List<String> extras;

        public OrderLossCandidateItemDTO() {}

        public Integer getOrderItemId() { return orderItemId; }
        public void setOrderItemId(Integer orderItemId) { this.orderItemId = orderItemId; }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }

        public Integer getExtraId() { return extraId; }
        public void setExtraId(Integer extraId) { this.extraId = extraId; }

        public Integer getSizeId() { return sizeId; }
        public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }

        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

        public BigDecimal getUnitCost() { return unitCost; }
        public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

        public BigDecimal getLineCost() { return lineCost; }
        public void setLineCost(BigDecimal lineCost) { this.lineCost = lineCost; }

        public List<String> getExtras() { return extras; }
        public void setExtras(List<String> extras) { this.extras = extras; }
    }
}
