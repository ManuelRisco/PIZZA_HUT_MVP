package com.example.services;

import com.example.models.OrderItemExtra;
import com.example.repositories.OrderItemExtraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderItemExtraService {

    private final OrderItemExtraRepository orderItemExtraRepository;

    public OrderItemExtraService(OrderItemExtraRepository orderItemExtraRepository) {
        this.orderItemExtraRepository = orderItemExtraRepository;
    }

    public List<OrderItemExtra> listarTodos() {
        return orderItemExtraRepository.findAll();
    }

    public List<OrderItemExtra> obtenerPorOrderItemId(Integer orderItemId) {
        return orderItemExtraRepository.findByOrderItemId(orderItemId);
    }

    @SuppressWarnings("null")
    @Transactional
    public OrderItemExtra crearOrderItemExtra(OrderItemExtra orderItemExtra) {
        return orderItemExtraRepository.save(orderItemExtra);
    }

    @Transactional
    public void eliminarPorOrderItemId(Integer orderItemId) {
        if (orderItemId != null) {
            orderItemExtraRepository.deleteByOrderItemId(orderItemId);
        }
    }

    @Transactional
    public void eliminar(Integer id) {
        if (id != null) {
            orderItemExtraRepository.deleteById(id);
        }
    }
}
