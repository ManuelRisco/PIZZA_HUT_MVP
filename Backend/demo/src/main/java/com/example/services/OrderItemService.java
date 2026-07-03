package com.example.services;

import com.example.models.OrderItem;
import com.example.repositories.OrderItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    public OrderItemService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

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
