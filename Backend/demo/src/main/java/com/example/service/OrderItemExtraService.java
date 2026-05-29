package com.example.service;

import com.example.domain.model.OrderItemExtra;
import com.example.infrastructure.persistence.entities.OrderItemExtraEntity;
import com.example.infrastructure.persistence.repository.OrderItemExtraRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderItemExtraService {

    @Autowired
    private OrderItemExtraRepository orderItemExtraRepository;

    public List<OrderItemExtra> listarTodos() {
        return orderItemExtraRepository.findAll()
            .stream()
            .map(OrderItemExtraEntity::toDomain)
            .collect(Collectors.toList());
    }

    public List<OrderItemExtra> obtenerPorOrderItemId(Integer orderItemId) {
        return orderItemExtraRepository.findByOrderItemId(orderItemId)
            .stream()
            .map(OrderItemExtraEntity::toDomain)
            .collect(Collectors.toList());
    }

    @Transactional
    public OrderItemExtra crearOrderItemExtra(OrderItemExtra orderItemExtra) {
        OrderItemExtraEntity entity = OrderItemExtraEntity.fromDomain(orderItemExtra);
        if (entity == null) {
            throw new IllegalArgumentException("No se pudo crear la entidad OrderItemExtra");
        }
        OrderItemExtraEntity savedEntity = orderItemExtraRepository.save(entity);
        return savedEntity.toDomain();
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
