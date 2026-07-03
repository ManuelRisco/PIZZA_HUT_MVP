package com.example.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderCompleteDTO {
    // Datos del pedido
    private Integer id;
    private Integer userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    
    // Direcci\u00f3n
    private Integer addressId;
    private String addressLine1;
    private String addressCity;
    private String addressDistrict;
    private String addressReference;
    
    // Estado y tipo
    private String deliveryType;
    private String status;
    
    // Pago
    private Integer paymentMethodId;
    private String paymentMethodName;
    
    // Totales
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discount;
    private BigDecimal total;
    
    // Promoci\u00f3n
    private String promoCode;
    
    // Adicionales
    private String notes;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime createdAt;
    
    // Items del pedido
    private List<OrderItemCompleteDTO> items;
    
    // Constructor vac\u00edo
    public OrderCompleteDTO() {
        // Constructor vacío para serialización por frameworks como Jackson
    }
    
    // Getters y Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    
    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }
    
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
    
    public String getAddressCity() { return addressCity; }
    public void setAddressCity(String addressCity) { this.addressCity = addressCity; }
    
    public String getAddressDistrict() { return addressDistrict; }
    public void setAddressDistrict(String addressDistrict) { this.addressDistrict = addressDistrict; }
    
    public String getAddressReference() { return addressReference; }
    public void setAddressReference(String addressReference) { this.addressReference = addressReference; }
    
    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }
    
    public String getPaymentMethodName() { return paymentMethodName; }
    public void setPaymentMethodName(String paymentMethodName) { this.paymentMethodName = paymentMethodName; }
    
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }
    
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    
    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<OrderItemCompleteDTO> getItems() { return items; }
    public void setItems(List<OrderItemCompleteDTO> items) { this.items = items; }
    
    // DTO interno para items
    public static class OrderItemCompleteDTO {
        private Integer id;
        private Integer pizzaId;
        private Integer extraId;
        private String itemType; // "PIZZA" o "EXTRA"
        private String pizzaName;
        private String extraName;
        private Integer sizeId;
        private String sizeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private List<String> extras; // Ingredientes extras (solo para pizzas)
        
        public OrderItemCompleteDTO() {
            // Constructor vacío para serialización
        }
        
        public Integer getId() { return id; }
        public void setId(Integer id) { this.id = id; }
        
        public Integer getPizzaId() { return pizzaId; }
        public void setPizzaId(Integer pizzaId) { this.pizzaId = pizzaId; }
        
        public Integer getExtraId() { return extraId; }
        public void setExtraId(Integer extraId) { this.extraId = extraId; }
        
        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }
        
        public String getPizzaName() { return pizzaName; }
        public void setPizzaName(String pizzaName) { this.pizzaName = pizzaName; }
        
        public String getExtraName() { return extraName; }
        public void setExtraName(String extraName) { this.extraName = extraName; }
        
        public Integer getSizeId() { return sizeId; }
        public void setSizeId(Integer sizeId) { this.sizeId = sizeId; }
        
        public String getSizeName() { return sizeName; }
        public void setSizeName(String sizeName) { this.sizeName = sizeName; }
        
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
        
        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
        
        public List<String> getExtras() { return extras; }
        public void setExtras(List<String> extras) { this.extras = extras; }
    }
}
