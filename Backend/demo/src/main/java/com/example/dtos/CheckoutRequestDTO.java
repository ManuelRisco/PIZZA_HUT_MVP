package com.example.dtos;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

public class CheckoutRequestDTO {
    
    @Valid
    @NotNull(message = "El pedido es obligatorio")
    private OrderDTO order;
    
    @Valid
    @NotNull(message = "La dirección es obligatoria")
    private AddressDTO address;
    
    @Valid
    @NotEmpty(message = "Debe haber al menos un artículo en el pedido")
    private List<CheckoutItemDTO> items;
    
    @Valid
    @NotNull(message = "Los datos de pago son obligatorios")
    private PaymentDTO payment;
    
    public static class AddressDTO {
        @NotBlank(message = "La línea 1 de la dirección es obligatoria")
        private String line1;
        
        @NotBlank(message = "La ciudad es obligatoria")
        private String city;
        
        @NotBlank(message = "El distrito es obligatorio")
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
        @NotBlank(message = "El tipo de artículo es obligatorio")
        private String itemType; // "EXTRA" o "PIZZA"
        
        private Integer pizzaId;
        private Integer extraId;
        private Integer sizeId;
        
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        private Integer quantity;
        
        @NotNull(message = "El precio unitario es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio unitario debe ser mayor a 0")
        private BigDecimal unitPrice;
        
        @NotNull(message = "El total de línea es obligatorio")
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
        @NotNull(message = "El método de pago es obligatorio")
        private Integer paymentMethodId;
        
        @NotNull(message = "El monto a pagar es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El monto debe ser mayor a 0")
        private BigDecimal amount;
        
        @NotBlank(message = "El estado del pago es obligatorio")
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
