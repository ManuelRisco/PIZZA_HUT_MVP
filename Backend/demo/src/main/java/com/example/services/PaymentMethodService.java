package com.example.services;

import com.example.models.PaymentMethod;
import com.example.repositories.PaymentMethodRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final com.example.repositories.PaymentRepository paymentRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository,
            com.example.repositories.PaymentRepository paymentRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.paymentRepository = paymentRepository;
    }

    @Cacheable("paymentMethods")
    public List<PaymentMethod> obtenerTodos() {
        return paymentMethodRepository.findAll();
    }

    public Optional<PaymentMethod> obtenerPorId(Integer id) {
        return paymentMethodRepository.findById(id);
    }

    @Cacheable("paymentMethodsActivos")
    public List<PaymentMethod> obtenerActivos() {
        return paymentMethodRepository.findByActive(true);
    }

    @CacheEvict(value = {"paymentMethods", "paymentMethodsActivos"}, allEntries = true)
    public PaymentMethod crearMetodoPago(PaymentMethod paymentMethod) {
        if (paymentMethod.getName() == null || paymentMethod.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar un nombre válido.");
        }
        if (paymentMethodRepository.existsByName(paymentMethod.getName())) {
            throw new IllegalArgumentException("El método de pago ya existe.");
        }
        // Validar que el displayOrder no esté en uso
        if (paymentMethodRepository.existsByDisplayOrder(paymentMethod.getDisplayOrder())) {
            throw new IllegalArgumentException(
                    "El orden de visualización " + paymentMethod.getDisplayOrder() + " ya está en uso");
        }
        return paymentMethodRepository.save(paymentMethod);
    }

    @CacheEvict(value = {"paymentMethods", "paymentMethodsActivos"}, allEntries = true)
    public PaymentMethod actualizarMetodoPago(Integer id, PaymentMethod paymentMethodActualizado) {
        if (paymentMethodActualizado.getName() == null || paymentMethodActualizado.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Debe ingresar un nombre válido.");
        }
        Optional<PaymentMethod> paymentMethodOpt = paymentMethodRepository.findById(id);
        if (paymentMethodOpt.isPresent()) {
            PaymentMethod paymentMethod = paymentMethodOpt.get();

            if (!paymentMethod.getName().equals(paymentMethodActualizado.getName())
                    && paymentMethodRepository.existsByName(paymentMethodActualizado.getName())) {
                throw new IllegalArgumentException("El método de pago ya existe.");
            }

            // Validar que el displayOrder no esté en uso por otro método
            if (paymentMethodRepository.existsByDisplayOrderAndIdNot(paymentMethodActualizado.getDisplayOrder(), id)) {
                throw new IllegalArgumentException("El orden de visualización "
                        + paymentMethodActualizado.getDisplayOrder() + " ya está en uso por otro método de pago");
            }

            paymentMethod.setName(paymentMethodActualizado.getName());
            paymentMethod.setDescription(paymentMethodActualizado.getDescription());
            paymentMethod.setActive(paymentMethodActualizado.isActive());
            paymentMethod.setDisplayOrder(paymentMethodActualizado.getDisplayOrder());
            return paymentMethodRepository.save(paymentMethod);
        }
        return null;
    }

    public boolean isInUse(Integer id) {
        return paymentRepository.existsByPaymentMethodId(id);
    }

    public java.util.Set<Integer> getPaymentMethodIdsInUse() {
        return paymentRepository.findPaymentMethodIdsInUse();
    }

    @CacheEvict(value = {"paymentMethods", "paymentMethodsActivos"}, allEntries = true)
    public boolean eliminarMetodoPago(Integer id) {
        Optional<PaymentMethod> paymentMethodOpt = paymentMethodRepository.findById(id);
        if (paymentMethodOpt.isPresent()) {
            if (isInUse(id)) {
                throw new IllegalArgumentException("El método de pago está en uso y no puede ser eliminado");
            }
            paymentMethodRepository.delete(paymentMethodOpt.get());
            return true;
        }
        return false;
    }

    public boolean existsByName(String name) {
        return paymentMethodRepository.existsByName(name);
    }

    @CacheEvict(value = {"paymentMethods", "paymentMethodsActivos"}, allEntries = true)
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
