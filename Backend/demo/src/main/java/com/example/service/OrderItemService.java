package com.example.service;

import com.example.domain.model.OrderItem;
import com.example.domain.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    public List<OrderItem> listarOrderItems() {
        return orderItemRepository.findAll();
    }

    public Optional<OrderItem> obtenerPorId(Integer id) {
        return orderItemRepository.findById(id);
    }

    public List<OrderItem> obtenerPorOrderId(Integer orderId) {
        return orderItemRepository.findByOrderId(orderId);
    }

    public OrderItem crearOrderItem(OrderItem orderItem) {
        return orderItemRepository.save(orderItem);
    }

    public OrderItem actualizarOrderItem(Integer id, OrderItem orderItem) {
        if (!orderItemRepository.existsById(id)) {
            throw new IllegalArgumentException("OrderItem no encontrado");
        }
        orderItem.setId(id);
        return orderItemRepository.save(orderItem);
    }

    public void eliminarOrderItem(Integer id) {
        if (!orderItemRepository.existsById(id)) {
            throw new IllegalArgumentException("OrderItem no encontrado");
        }
        orderItemRepository.deleteById(id);
    }
}
