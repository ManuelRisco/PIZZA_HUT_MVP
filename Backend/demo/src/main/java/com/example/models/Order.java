package com.example.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer userId;
    private Integer addressId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type")
    private DeliveryType deliveryType;
    private Integer paymentMethodId;
    private Integer promotionId;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discount;
    private BigDecimal total;
    private String notes;
    @Column(name = "promo_code")
    private String promoCode;
    @Column(name = "stockSettled", length = 20)
    private String stockSettled = "DEDUCTED";
    private LocalDateTime estimatedDelivery;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==== Campos de Boleta de Venta Electrónica (SUNAT / Nubefact) ====
    @Column(name = "wants_boleta")
    private Boolean wantsBoleta = false;

    @Column(name = "boleta_tipo_comprobante")
    private Integer boletaTipoComprobante;

    @Column(name = "boleta_serie", length = 10)
    private String boletaSerie;

    @Column(name = "boleta_numero")
    private Integer boletaNumero;

    @Column(name = "boleta_cliente_dni", length = 15)
    private String boletaClienteDni;

    @Column(name = "boleta_cliente_nombre", length = 200)
    private String boletaClienteNombre;

    @Column(name = "boleta_cliente_direccion", length = 255)
    private String boletaClienteDireccion;

    @Column(name = "boleta_pdf_url", length = 500)
    private String boletaPdfUrl;

    @Column(name = "boleta_xml_url", length = 500)
    private String boletaXmlUrl;

    @Column(name = "boleta_cdr_url", length = 500)
    private String boletaCdrUrl;

    @Column(name = "boleta_enlace", length = 500)
    private String boletaEnlace;

    @Column(name = "boleta_hash", length = 255)
    private String boletaHash;

    @Column(name = "boleta_qr", length = 500)
    private String boletaQr;

    @Column(name = "boleta_estado", length = 40)
    private String boletaEstado;

    @Column(name = "boleta_error", length = 500)
    private String boletaError;

    @Column(name = "boleta_total_gravada")
    private BigDecimal boletaTotalGravada;

    @Column(name = "boleta_total_igv")
    private BigDecimal boletaTotalIgv;

    @Column(name = "boleta_emitida_at")
    private LocalDateTime boletaEmitidaAt;

    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PREPARING,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }

    public enum DeliveryType {
        PICKUP,
        DELIVERY
    }

    public Order() {}

    public Order(Integer id, Integer userId, Integer addressId, OrderStatus status,
            DeliveryType deliveryType, Integer paymentMethodId, BigDecimal subtotal,
            BigDecimal deliveryFee, BigDecimal discount, BigDecimal total, String notes,
            String promoCode, LocalDateTime estimatedDelivery, LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.addressId = addressId;
        this.status = status;
        this.deliveryType = deliveryType;
        this.paymentMethodId = paymentMethodId;
        this.subtotal = subtotal;
        this.deliveryFee = deliveryFee;
        this.discount = discount;
        this.total = total;
        this.notes = notes;
        this.promoCode = promoCode;
        this.estimatedDelivery = estimatedDelivery;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public Integer getAddressId() { return addressId; }
    public void setAddressId(Integer addressId) { this.addressId = addressId; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public DeliveryType getDeliveryType() { return deliveryType; }
    public void setDeliveryType(DeliveryType deliveryType) { this.deliveryType = deliveryType; }

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

    public String getStockSettled() { return stockSettled; }
    public void setStockSettled(String stockSettled) { this.stockSettled = stockSettled; }

    public LocalDateTime getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(LocalDateTime estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getWantsBoleta() { return wantsBoleta; }
    public void setWantsBoleta(Boolean wantsBoleta) { this.wantsBoleta = wantsBoleta; }

    public Integer getBoletaTipoComprobante() { return boletaTipoComprobante; }
    public void setBoletaTipoComprobante(Integer boletaTipoComprobante) { this.boletaTipoComprobante = boletaTipoComprobante; }

    public String getBoletaSerie() { return boletaSerie; }
    public void setBoletaSerie(String boletaSerie) { this.boletaSerie = boletaSerie; }

    public Integer getBoletaNumero() { return boletaNumero; }
    public void setBoletaNumero(Integer boletaNumero) { this.boletaNumero = boletaNumero; }

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

    @PrePersist
    protected void onCreate() {
        if (stockSettled == null) stockSettled = "DEDUCTED";
        if (wantsBoleta == null) wantsBoleta = false;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        if (wantsBoleta == null) wantsBoleta = false;
        updatedAt = LocalDateTime.now();
    }
}
