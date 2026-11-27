package com.ecopizza.dtos;

import java.time.LocalDateTime;
import java.util.List;

public class OrderLossCreateDTO {
    private Integer orderId;
    private String category;
    private String reason;
    private LocalDateTime lossDate;
    private Boolean restoreUnselectedStock = true;
    private Boolean deductStockIfPending = true;
    private List<OrderLossItemDTO> items;

    public OrderLossCreateDTO() {}

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getLossDate() { return lossDate; }
    public void setLossDate(LocalDateTime lossDate) { this.lossDate = lossDate; }

    public Boolean getRestoreUnselectedStock() { return restoreUnselectedStock; }
    public void setRestoreUnselectedStock(Boolean restoreUnselectedStock) { this.restoreUnselectedStock = restoreUnselectedStock; }

    public Boolean getDeductStockIfPending() { return deductStockIfPending; }
    public void setDeductStockIfPending(Boolean deductStockIfPending) { this.deductStockIfPending = deductStockIfPending; }

    public List<OrderLossItemDTO> getItems() { return items; }
    public void setItems(List<OrderLossItemDTO> items) { this.items = items; }

    public static class OrderLossItemDTO {
        private Integer orderItemId;
        private Integer quantity;

        public OrderLossItemDTO() {}

        public OrderLossItemDTO(Integer orderItemId, Integer quantity) {
            this.orderItemId = orderItemId;
            this.quantity = quantity;
        }

        public Integer getOrderItemId() { return orderItemId; }
        public void setOrderItemId(Integer orderItemId) { this.orderItemId = orderItemId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
