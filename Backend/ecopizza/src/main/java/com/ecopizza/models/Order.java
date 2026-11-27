package com.ecopizza.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    // ==== Campos de Nota de Crédito Electrónica (SUNAT / Nubefact - Anulación de Boleta) ====
    @Column(name = "nc_serie", length = 10)
    private String ncSerie;

    @Column(name = "nc_numero")
    private Integer ncNumero;

    @Column(name = "nc_tipo_nota")
    private Integer ncTipoNota;

    @Column(name = "nc_motivo", length = 255)
    private String ncMotivo;

    @Column(name = "nc_pdf_url", length = 500)
    private String ncPdfUrl;

    @Column(name = "nc_xml_url", length = 500)
    private String ncXmlUrl;

    @Column(name = "nc_cdr_url", length = 500)
    private String ncCdrUrl;

    @Column(name = "nc_enlace", length = 500)
    private String ncEnlace;

    @Column(name = "nc_hash", length = 255)
    private String ncHash;

    @Column(name = "nc_qr", length = 500)
    private String ncQr;

    @Column(name = "nc_estado", length = 40)
    private String ncEstado;

    @Column(name = "nc_error", length = 500)
    private String ncError;

    @Column(name = "nc_emitida_at")
    private LocalDateTime ncEmitidaAt;

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
