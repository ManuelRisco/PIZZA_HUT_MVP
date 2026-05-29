package com.example.service;

import com.example.domain.model.OrderTracking;
import com.example.domain.repository.OrderTrackingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderTrackingService {

    @Autowired
    private OrderTrackingRepository orderTrackingRepository;

    public List<OrderTracking> listarOrderTrackings() {
        return orderTrackingRepository.findAll();
    }

    public Optional<OrderTracking> obtenerPorId(Integer id) {
        return orderTrackingRepository.findById(id);
    }

    public List<OrderTracking> obtenerPorOrderId(Integer orderId) {
        return orderTrackingRepository.findByOrderId(orderId);
    }

    public OrderTracking crearOrderTracking(OrderTracking orderTracking) {
        return orderTrackingRepository.save(orderTracking);
    }

    public void eliminarOrderTracking(Integer id) {
        if (!orderTrackingRepository.existsById(id)) {
            throw new IllegalArgumentException("OrderTracking no encontrado");
        }
        orderTrackingRepository.deleteById(id);
    }
}
