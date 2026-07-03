package com.example.services;

import com.example.models.OrderItemExtra;
import com.example.repositories.OrderItemExtraRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@SuppressWarnings("null")
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

    public java.util.Optional<OrderItemExtra> obtenerPorId(Integer id) {
        return orderItemExtraRepository.findById(id);
    }

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
