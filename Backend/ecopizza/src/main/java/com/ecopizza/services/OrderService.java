package com.ecopizza.services;

import com.ecopizza.models.Order;
import com.ecopizza.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<Order> listarOrders() {
        return orderRepository.findTop100ByOrderByCreatedAtDesc();
    }

    public Optional<Order> obtenerPorId(Integer id) {
        return orderRepository.findById(id);
    }

    public List<Order> obtenerPorUserId(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> obtenerPorStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    public Order crearOrder(Order order) {
        return orderRepository.save(order);
    }

    public Order actualizarOrder(Integer id, Order orderActualizada) {
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order no encontrado");
        }

        Order order = orderOpt.get();

        if (orderActualizada.getUserId() != null) order.setUserId(orderActualizada.getUserId());
        if (orderActualizada.getAddressId() != null) order.setAddressId(orderActualizada.getAddressId());
        if (orderActualizada.getStatus() != null) order.setStatus(orderActualizada.getStatus());
        if (orderActualizada.getDeliveryType() != null) order.setDeliveryType(orderActualizada.getDeliveryType());
        if (orderActualizada.getPaymentMethodId() != null) order.setPaymentMethodId(orderActualizada.getPaymentMethodId());
        if (orderActualizada.getPromotionId() != null) order.setPromotionId(orderActualizada.getPromotionId());
        if (orderActualizada.getSubtotal() != null) order.setSubtotal(orderActualizada.getSubtotal());
        if (orderActualizada.getDeliveryFee() != null) order.setDeliveryFee(orderActualizada.getDeliveryFee());
        if (orderActualizada.getDiscount() != null) order.setDiscount(orderActualizada.getDiscount());
        if (orderActualizada.getTotal() != null) order.setTotal(orderActualizada.getTotal());
        if (orderActualizada.getNotes() != null) order.setNotes(orderActualizada.getNotes());
        if (orderActualizada.getPromoCode() != null) order.setPromoCode(orderActualizada.getPromoCode());
        if (orderActualizada.getStockSettled() != null) order.setStockSettled(orderActualizada.getStockSettled());
        if (orderActualizada.getEstimatedDelivery() != null) order.setEstimatedDelivery(orderActualizada.getEstimatedDelivery());

        if (orderActualizada.getWantsBoleta() != null) order.setWantsBoleta(orderActualizada.getWantsBoleta());
        if (orderActualizada.getBoletaTipoComprobante() != null) order.setBoletaTipoComprobante(orderActualizada.getBoletaTipoComprobante());
        if (orderActualizada.getBoletaSerie() != null) order.setBoletaSerie(orderActualizada.getBoletaSerie());
        if (orderActualizada.getBoletaNumero() != null) order.setBoletaNumero(orderActualizada.getBoletaNumero());
        if (orderActualizada.getBoletaClienteDni() != null) order.setBoletaClienteDni(orderActualizada.getBoletaClienteDni());
        if (orderActualizada.getBoletaClienteNombre() != null) order.setBoletaClienteNombre(orderActualizada.getBoletaClienteNombre());
        if (orderActualizada.getBoletaClienteDireccion() != null) order.setBoletaClienteDireccion(orderActualizada.getBoletaClienteDireccion());
        if (orderActualizada.getBoletaPdfUrl() != null) order.setBoletaPdfUrl(orderActualizada.getBoletaPdfUrl());
        if (orderActualizada.getBoletaXmlUrl() != null) order.setBoletaXmlUrl(orderActualizada.getBoletaXmlUrl());
        if (orderActualizada.getBoletaCdrUrl() != null) order.setBoletaCdrUrl(orderActualizada.getBoletaCdrUrl());
        if (orderActualizada.getBoletaEnlace() != null) order.setBoletaEnlace(orderActualizada.getBoletaEnlace());
        if (orderActualizada.getBoletaHash() != null) order.setBoletaHash(orderActualizada.getBoletaHash());
        if (orderActualizada.getBoletaQr() != null) order.setBoletaQr(orderActualizada.getBoletaQr());
        if (orderActualizada.getBoletaEstado() != null) order.setBoletaEstado(orderActualizada.getBoletaEstado());
        if (orderActualizada.getBoletaError() != null) order.setBoletaError(orderActualizada.getBoletaError());
        if (orderActualizada.getBoletaTotalGravada() != null) order.setBoletaTotalGravada(orderActualizada.getBoletaTotalGravada());
        if (orderActualizada.getBoletaTotalIgv() != null) order.setBoletaTotalIgv(orderActualizada.getBoletaTotalIgv());
        if (orderActualizada.getBoletaEmitidaAt() != null) order.setBoletaEmitidaAt(orderActualizada.getBoletaEmitidaAt());

        if (orderActualizada.getNcSerie() != null) order.setNcSerie(orderActualizada.getNcSerie());
        if (orderActualizada.getNcNumero() != null) order.setNcNumero(orderActualizada.getNcNumero());
        if (orderActualizada.getNcTipoNota() != null) order.setNcTipoNota(orderActualizada.getNcTipoNota());
        if (orderActualizada.getNcMotivo() != null) order.setNcMotivo(orderActualizada.getNcMotivo());
        if (orderActualizada.getNcPdfUrl() != null) order.setNcPdfUrl(orderActualizada.getNcPdfUrl());
        if (orderActualizada.getNcXmlUrl() != null) order.setNcXmlUrl(orderActualizada.getNcXmlUrl());
        if (orderActualizada.getNcCdrUrl() != null) order.setNcCdrUrl(orderActualizada.getNcCdrUrl());
        if (orderActualizada.getNcEnlace() != null) order.setNcEnlace(orderActualizada.getNcEnlace());
        if (orderActualizada.getNcHash() != null) order.setNcHash(orderActualizada.getNcHash());
        if (orderActualizada.getNcQr() != null) order.setNcQr(orderActualizada.getNcQr());
        if (orderActualizada.getNcEstado() != null) order.setNcEstado(orderActualizada.getNcEstado());
        if (orderActualizada.getNcError() != null) order.setNcError(orderActualizada.getNcError());
        if (orderActualizada.getNcEmitidaAt() != null) order.setNcEmitidaAt(orderActualizada.getNcEmitidaAt());

        return orderRepository.save(order);
    }

    public void eliminarOrder(Integer id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order no encontrado");
        }
        orderRepository.deleteById(id);
    }
}
