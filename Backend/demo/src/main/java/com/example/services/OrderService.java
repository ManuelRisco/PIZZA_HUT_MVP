package com.example.services;

import com.example.models.Order;
import com.example.repositories.OrderRepository;
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
        return orderRepository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<Order> obtenerPorId(Integer id) {
        return orderRepository.findById(id);
    }

    public List<Order> obtenerPorUserId(Integer userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> obtenerPorStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @SuppressWarnings("null")
    public Order crearOrder(Order order) {
        return orderRepository.save(order);
    }

    public Order actualizarOrder(Integer id, Order orderActualizada) {
        @SuppressWarnings("null")
        Optional<Order> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            throw new IllegalArgumentException("Order no encontrado");
        }

        Order order = orderOpt.get();

        // Actualizar solo los campos que vienen en orderActualizada
        if (orderActualizada.getUserId() != null) {
            order.setUserId(orderActualizada.getUserId());
        }
        if (orderActualizada.getAddressId() != null) {
            order.setAddressId(orderActualizada.getAddressId());
        }
        if (orderActualizada.getStatus() != null) {
            order.setStatus(orderActualizada.getStatus());
        }
        if (orderActualizada.getDeliveryType() != null) {
            order.setDeliveryType(orderActualizada.getDeliveryType());
        }
        if (orderActualizada.getPaymentMethodId() != null) {
            order.setPaymentMethodId(orderActualizada.getPaymentMethodId());
        }
        if (orderActualizada.getPromotionId() != null) {
            order.setPromotionId(orderActualizada.getPromotionId());
        }
        if (orderActualizada.getSubtotal() != null) {
            order.setSubtotal(orderActualizada.getSubtotal());
        }
        if (orderActualizada.getDeliveryFee() != null) {
            order.setDeliveryFee(orderActualizada.getDeliveryFee());
        }
        if (orderActualizada.getDiscount() != null) {
            order.setDiscount(orderActualizada.getDiscount());
        }
        if (orderActualizada.getTotal() != null) {
            order.setTotal(orderActualizada.getTotal());
        }
        if (orderActualizada.getNotes() != null) {
            order.setNotes(orderActualizada.getNotes());
        }
        if (orderActualizada.getPromoCode() != null) {
            order.setPromoCode(orderActualizada.getPromoCode());
        }
        if (orderActualizada.getEstimatedDelivery() != null) {
            order.setEstimatedDelivery(orderActualizada.getEstimatedDelivery());
        }

        order.setUpdatedAt(java.time.LocalDateTime.now());
        return orderRepository.save(order);
    }

    @SuppressWarnings("null")
    public void eliminarOrder(Integer id) {
        if (!orderRepository.existsById(id)) {
            throw new IllegalArgumentException("Order no encontrado");
        }
        orderRepository.deleteById(id);
    }
}
