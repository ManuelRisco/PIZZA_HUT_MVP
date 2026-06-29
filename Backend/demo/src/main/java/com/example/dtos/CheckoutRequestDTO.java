package com.example.dtos;

import java.math.BigDecimal;
import java.util.List;

public class CheckoutRequestDTO {
    
    private OrderDTO order;
    private AddressDTO address;
    private List<CheckoutItemDTO> items;
    private PaymentDTO payment;
    
    public static class AddressDTO {
        private String line1;
        private String city;
        private String district;
        private String reference;
        private Boolean isDefault;
        
        public String getLine1() { return line1; }
        public void setLine1(String line1) { this.line1 = line1; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }

    public static class CheckoutItemDTO {
        private String itemType; // "EXTRA" o "PIZZA"
        private Integer pizzaId;
        private Integer extraId;
        private Integer sizeId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private List<Integer> extraIngredientIds;

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        public Integer getExtraId() { return extraId; }
        public void setExtraId(Integer extraId) { this.extraId = extraId; }
        public Integer getSizeId() { return sizeId; }
        public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
        public List<Integer> getExtraIngredientIds() { return extraIngredientIds; }
        public void setExtraIngredientIds(List<Integer> extraIngredientIds) { this.extraIngredientIds = extraIngredientIds; }
    }
    
    public static class PaymentDTO {
        private Integer paymentMethodId;
        private BigDecimal amount;
        private String status;
        private String transactionId;

        public Integer getPaymentMethodId() { return paymentMethodId; }
        public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }

    public OrderDTO getOrder() { return order; }
    public void setOrder(OrderDTO order) { this.order = order; }
    public AddressDTO getAddress() { return address; }
    public void setAddress(AddressDTO address) { this.address = address; }
    public List<CheckoutItemDTO> getItems() { return items; }
    public void setItems(List<CheckoutItemDTO> items) { this.items = items; }
    public PaymentDTO getPayment() { return payment; }
    public void setPayment(PaymentDTO payment) { this.payment = payment; }
}
