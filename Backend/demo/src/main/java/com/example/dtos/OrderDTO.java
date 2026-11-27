package com.example.dtos;

import com.example.models.Order;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class OrderDTO {
    private Integer id;
    private Integer userId;
    private Integer addressId;
    private String status;
    private String deliveryType;

    @NotNull(message = "El ID del método de pago es obligatorio")
    private Integer paymentMethodId;

    private Integer promotionId;

    @NotNull(message = "El subtotal es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El subtotal no puede ser negativo")
    private BigDecimal subtotal;

    @NotNull(message = "La tarifa de entrega es obligatoria")
    @DecimalMin(value = "0.0", inclusive = true, message = "La tarifa de entrega no puede ser negativa")
    private BigDecimal deliveryFee;

    private BigDecimal discount;

    @NotNull(message = "El total es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "El total no puede ser negativo")
    private BigDecimal total;

    private String notes;
    private String promoCode;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime createdAt;

    private Boolean emailSent;
    private String emailSentTo;
    private Boolean adminEmailSent;

    // ==== Boleta de Venta Electrónica (SUNAT / Nubefact) ====
    private Boolean wantsBoleta;
    private Integer boletaTipoComprobante;
    private String boletaSerie;
    private Integer boletaNumero;
    private String boletaComprobante; // Ej. BBB1-000003
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

    // ==== Nota de Crédito Electrónica (SUNAT / Nubefact) ====
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

    public OrderDTO() {}

    public OrderDTO(Order order) {
        this.id = order.getId();
        this.userId = order.getUserId();
        this.addressId = order.getAddressId();
        this.status = order.getStatus() != null ? order.getStatus().name() : null;
        this.deliveryType = order.getDeliveryType() != null ? order.getDeliveryType().name() : null;
        this.paymentMethodId = order.getPaymentMethodId();
        this.promotionId = order.getPromotionId();
        this.subtotal = order.getSubtotal();
        this.deliveryFee = order.getDeliveryFee();
        this.discount = order.getDiscount();
        this.total = order.getTotal();
        this.notes = order.getNotes();
        this.promoCode = order.getPromoCode();
        this.estimatedDelivery = order.getEstimatedDelivery();
        this.createdAt = order.getCreatedAt();
        this.wantsBoleta = Boolean.TRUE.equals(order.getWantsBoleta());
        this.boletaTipoComprobante = order.getBoletaTipoComprobante();
        this.boletaSerie = order.getBoletaSerie();
        this.boletaNumero = order.getBoletaNumero();
        if (order.getBoletaSerie() != null && order.getBoletaNumero() != null) {
            this.boletaComprobante = order.getBoletaSerie() + "-" + String.format("%06d", order.getBoletaNumero());
        }
        this.boletaClienteDni = order.getBoletaClienteDni();
        this.boletaClienteNombre = order.getBoletaClienteNombre();
        this.boletaClienteDireccion = order.getBoletaClienteDireccion();
        this.boletaPdfUrl = order.getBoletaPdfUrl();
        this.boletaXmlUrl = order.getBoletaXmlUrl();
        this.boletaCdrUrl = order.getBoletaCdrUrl();
        this.boletaEnlace = order.getBoletaEnlace();
        this.boletaHash = order.getBoletaHash();
        this.boletaQr = order.getBoletaQr();
        this.boletaEstado = order.getBoletaEstado();
        this.boletaError = order.getBoletaError();
        this.boletaTotalGravada = order.getBoletaTotalGravada();
        this.boletaTotalIgv = order.getBoletaTotalIgv();
        this.boletaEmitidaAt = order.getBoletaEmitidaAt();
        this.ncSerie = order.getNcSerie();
        this.ncNumero = order.getNcNumero();
        if (order.getNcSerie() != null && order.getNcNumero() != null) {
            this.ncComprobante = order.getNcSerie() + "-" + String.format("%06d", order.getNcNumero());
        }
        this.ncTipoNota = order.getNcTipoNota();
        this.ncMotivo = order.getNcMotivo();
        this.ncPdfUrl = order.getNcPdfUrl();
        this.ncXmlUrl = order.getNcXmlUrl();
        this.ncCdrUrl = order.getNcCdrUrl();
        this.ncEnlace = order.getNcEnlace();
        this.ncHash = order.getNcHash();
        this.ncQr = order.getNcQr();
        this.ncEstado = order.getNcEstado();
        this.ncError = order.getNcError();
        this.ncEmitidaAt = order.getNcEmitidaAt();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public Integer getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(Integer paymentMethodId) { this.paymentMethodId = paymentMethodId; }

    public Integer getPromotionId() { return promotionId; }
    public void setPromotionId(Integer promotionId) { this.promotionId = promotionId; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(BigDecimal deliveryFee) { this.deliveryFee = deliveryFee; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Boolean getEmailSent() { return emailSent; }
    public void setEmailSent(Boolean emailSent) { this.emailSent = emailSent; }

    public String getEmailSentTo() { return emailSentTo; }
    public void setEmailSentTo(String emailSentTo) { this.emailSentTo = emailSentTo; }

    public Boolean getAdminEmailSent() { return adminEmailSent; }
    public void setAdminEmailSent(Boolean adminEmailSent) { this.adminEmailSent = adminEmailSent; }

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
}
