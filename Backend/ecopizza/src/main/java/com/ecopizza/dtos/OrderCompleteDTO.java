package com.ecopizza.dtos;

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
    private String userDni;
    private String userRuc;

    // Dirección
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

    // Promoción
    private String promoCode;

    // Adicionales
    private String notes;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime createdAt;

    // Boleta de Venta Electrónica (SUNAT / Nubefact)
    private Boolean wantsBoleta;
    private Integer boletaTipoComprobante;
    private String boletaSerie;
    private Integer boletaNumero;
    private String boletaComprobante;
    private String boletaClienteDni;
    private String boletaClienteNombre;
    private String boletaClienteDireccion;
    private String boletaPdfUrl;
    private String boletaXmlUrl;
    private String boletaCdrUrl;
    private String boletaEnlace;
    private String boletaHash;
    private String boletaQr;
    private String boletaEstado;
    private String boletaError;
    private BigDecimal boletaTotalGravada;
    private BigDecimal boletaTotalIgv;
    private LocalDateTime boletaEmitidaAt;

    // Nota de Crédito Electrónica (SUNAT / Nubefact)
    private String ncSerie;
    private Integer ncNumero;
    private String ncComprobante;
    private Integer ncTipoNota;
    private String ncMotivo;
    private String ncPdfUrl;
    private String ncXmlUrl;
    private String ncCdrUrl;
    private String ncEnlace;
    private String ncHash;
    private String ncQr;
    private String ncEstado;
    private String ncError;
    private LocalDateTime ncEmitidaAt;

    // Items del pedido
    private List<OrderItemCompleteDTO> items;

    public OrderCompleteDTO() {}

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

    public String getUserDni() { return userDni; }
    public void setUserDni(String userDni) { this.userDni = userDni; }

    public String getUserRuc() { return userRuc; }
    public void setUserRuc(String userRuc) { this.userRuc = userRuc; }

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

    public Boolean getWantsBoleta() { return wantsBoleta; }
    public void setWantsBoleta(Boolean wantsBoleta) { this.wantsBoleta = wantsBoleta; }

    public Integer getBoletaTipoComprobante() { return boletaTipoComprobante; }
    public void setBoletaTipoComprobante(Integer boletaTipoComprobante) { this.boletaTipoComprobante = boletaTipoComprobante; }

    public String getBoletaSerie() { return boletaSerie; }
    public void setBoletaSerie(String boletaSerie) { this.boletaSerie = boletaSerie; }

    public Integer getBoletaNumero() { return boletaNumero; }
    public void setBoletaNumero(Integer boletaNumero) { this.boletaNumero = boletaNumero; }

    public String getBoletaComprobante() { return boletaComprobante; }
    public void setBoletaComprobante(String boletaComprobante) { this.boletaComprobante = boletaComprobante; }

    public String getBoletaClienteDni() { return boletaClienteDni; }
    public void setBoletaClienteDni(String boletaClienteDni) { this.boletaClienteDni = boletaClienteDni; }

    public String getBoletaClienteNombre() { return boletaClienteNombre; }
    public void setBoletaClienteNombre(String boletaClienteNombre) { this.boletaClienteNombre = boletaClienteNombre; }

    public String getBoletaClienteDireccion() { return boletaClienteDireccion; }
    public void setBoletaClienteDireccion(String boletaClienteDireccion) { this.boletaClienteDireccion = boletaClienteDireccion; }

    public String getBoletaPdfUrl() { return boletaPdfUrl; }
    public void setBoletaPdfUrl(String boletaPdfUrl) { this.boletaPdfUrl = boletaPdfUrl; }

    public String getBoletaXmlUrl() { return boletaXmlUrl; }
    public void setBoletaXmlUrl(String boletaXmlUrl) { this.boletaXmlUrl = boletaXmlUrl; }

    public String getBoletaCdrUrl() { return boletaCdrUrl; }
    public void setBoletaCdrUrl(String boletaCdrUrl) { this.boletaCdrUrl = boletaCdrUrl; }

    public String getBoletaEnlace() { return boletaEnlace; }
    public void setBoletaEnlace(String boletaEnlace) { this.boletaEnlace = boletaEnlace; }

    public String getBoletaHash() { return boletaHash; }
    public void setBoletaHash(String boletaHash) { this.boletaHash = boletaHash; }

    public String getBoletaQr() { return boletaQr; }
    public void setBoletaQr(String boletaQr) { this.boletaQr = boletaQr; }

    public String getBoletaEstado() { return boletaEstado; }
    public void setBoletaEstado(String boletaEstado) { this.boletaEstado = boletaEstado; }

    public String getBoletaError() { return boletaError; }
    public void setBoletaError(String boletaError) { this.boletaError = boletaError; }

    public BigDecimal getBoletaTotalGravada() { return boletaTotalGravada; }
    public void setBoletaTotalGravada(BigDecimal boletaTotalGravada) { this.boletaTotalGravada = boletaTotalGravada; }

    public BigDecimal getBoletaTotalIgv() { return boletaTotalIgv; }
    public void setBoletaTotalIgv(BigDecimal boletaTotalIgv) { this.boletaTotalIgv = boletaTotalIgv; }

    public LocalDateTime getBoletaEmitidaAt() { return boletaEmitidaAt; }
    public void setBoletaEmitidaAt(LocalDateTime boletaEmitidaAt) { this.boletaEmitidaAt = boletaEmitidaAt; }

    public String getNcSerie() { return ncSerie; }
    public void setNcSerie(String ncSerie) { this.ncSerie = ncSerie; }

    public Integer getNcNumero() { return ncNumero; }
    public void setNcNumero(Integer ncNumero) { this.ncNumero = ncNumero; }

    public String getNcComprobante() { return ncComprobante; }
    public void setNcComprobante(String ncComprobante) { this.ncComprobante = ncComprobante; }

    public Integer getNcTipoNota() { return ncTipoNota; }
    public void setNcTipoNota(Integer ncTipoNota) { this.ncTipoNota = ncTipoNota; }

    public String getNcMotivo() { return ncMotivo; }
    public void setNcMotivo(String ncMotivo) { this.ncMotivo = ncMotivo; }

    public String getNcPdfUrl() { return ncPdfUrl; }
    public void setNcPdfUrl(String ncPdfUrl) { this.ncPdfUrl = ncPdfUrl; }

    public String getNcXmlUrl() { return ncXmlUrl; }
    public void setNcXmlUrl(String ncXmlUrl) { this.ncXmlUrl = ncXmlUrl; }

    public String getNcCdrUrl() { return ncCdrUrl; }
    public void setNcCdrUrl(String ncCdrUrl) { this.ncCdrUrl = ncCdrUrl; }

    public String getNcEnlace() { return ncEnlace; }
    public void setNcEnlace(String ncEnlace) { this.ncEnlace = ncEnlace; }

    public String getNcHash() { return ncHash; }
    public void setNcHash(String ncHash) { this.ncHash = ncHash; }

    public String getNcQr() { return ncQr; }
    public void setNcQr(String ncQr) { this.ncQr = ncQr; }

    public String getNcEstado() { return ncEstado; }
    public void setNcEstado(String ncEstado) { this.ncEstado = ncEstado; }

    public String getNcError() { return ncError; }
    public void setNcError(String ncError) { this.ncError = ncError; }

    public LocalDateTime getNcEmitidaAt() { return ncEmitidaAt; }
    public void setNcEmitidaAt(LocalDateTime ncEmitidaAt) { this.ncEmitidaAt = ncEmitidaAt; }

    public List<OrderItemCompleteDTO> getItems() { return items; }
    public void setItems(List<OrderItemCompleteDTO> items) { this.items = items; }

    public static class OrderItemCompleteDTO {
        private Integer id;
        private Integer pizzaId;
        private Integer extraId;
        private String itemType;
        private String pizzaName;
        private String extraName;
        private Integer sizeId;
        private String sizeName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
        private List<String> extras;

        public OrderItemCompleteDTO() {}

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
