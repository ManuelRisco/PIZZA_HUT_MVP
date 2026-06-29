package com.example.services;

import com.example.models.OrderTracking;
import com.example.repositories.OrderTrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderTrackingService {

    private final OrderTrackingRepository orderTrackingRepository;

    public OrderTrackingService(OrderTrackingRepository orderTrackingRepository) {
        this.orderTrackingRepository = orderTrackingRepository;
    }

    public List<OrderTracking> listarOrderTrackings() {
        return orderTrackingRepository.findAll();
    }

    @SuppressWarnings("null")
    public Optional<OrderTracking> obtenerPorId(Integer id) {
        return orderTrackingRepository.findById(id);
    }

    public List<OrderTracking> obtenerPorOrderId(Integer orderId) {
        return orderTrackingRepository.findByOrderId(orderId);
    }

    @SuppressWarnings("null")
    public OrderTracking crearOrderTracking(OrderTracking orderTracking) {
        return orderTrackingRepository.save(orderTracking);
    }

    @SuppressWarnings("null")
    public void eliminarOrderTracking(Integer id) {
        if (!orderTrackingRepository.existsById(id)) {
            throw new IllegalArgumentException("OrderTracking no encontrado");
        }
        orderTrackingRepository.deleteById(id);
    }
}
