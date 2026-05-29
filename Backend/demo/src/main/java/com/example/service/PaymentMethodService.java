package com.example.service;

import com.example.domain.model.PaymentMethod;
import com.example.domain.repository.PaymentMethodRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentMethodService {

    @Autowired
    private PaymentMethodRepository paymentMethodRepository;

    public List<PaymentMethod> obtenerTodos() {
        return paymentMethodRepository.findAll();
    }

    public Optional<PaymentMethod> obtenerPorId(Integer id) {
        return paymentMethodRepository.findById(id);
    }

    public List<PaymentMethod> obtenerActivos() {
        return paymentMethodRepository.findByActive(true);
    }

    public PaymentMethod crearMetodoPago(PaymentMethod paymentMethod) {
        // Validar que el displayOrder no esté en uso
        if (paymentMethodRepository.existsByDisplayOrder(paymentMethod.getDisplayOrder())) {
            throw new IllegalArgumentException("El orden de visualización " + paymentMethod.getDisplayOrder() + " ya está en uso");
        }
        return paymentMethodRepository.save(paymentMethod);
    }

    public PaymentMethod actualizarMetodoPago(Integer id, PaymentMethod paymentMethodActualizado) {
        Optional<PaymentMethod> paymentMethodOpt = paymentMethodRepository.findById(id);
        if (paymentMethodOpt.isPresent()) {
            // Validar que el displayOrder no esté en uso por otro método
            if (paymentMethodRepository.existsByDisplayOrderAndIdNot(paymentMethodActualizado.getDisplayOrder(), id)) {
                throw new IllegalArgumentException("El orden de visualización " + paymentMethodActualizado.getDisplayOrder() + " ya está en uso por otro método de pago");
            }
            
            PaymentMethod paymentMethod = paymentMethodOpt.get();
            paymentMethod.setName(paymentMethodActualizado.getName());
            paymentMethod.setDescription(paymentMethodActualizado.getDescription());
            paymentMethod.setActive(paymentMethodActualizado.isActive());
            paymentMethod.setDisplayOrder(paymentMethodActualizado.getDisplayOrder());
            return paymentMethodRepository.save(paymentMethod);
        }
        return null;
    }

    public boolean eliminarMetodoPago(Integer id) {
        if (paymentMethodRepository.existsById(id)) {
            paymentMethodRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return paymentMethodRepository.existsByName(name);
    }

    public PaymentMethod cambiarEstado(Integer id, boolean isActive) {
        Optional<PaymentMethod> paymentMethodOpt = paymentMethodRepository.findById(id);
        if (paymentMethodOpt.isPresent()) {
            PaymentMethod paymentMethod = paymentMethodOpt.get();
            paymentMethod.setActive(isActive);
            return paymentMethodRepository.save(paymentMethod);
        }
        return null;
    }
}
