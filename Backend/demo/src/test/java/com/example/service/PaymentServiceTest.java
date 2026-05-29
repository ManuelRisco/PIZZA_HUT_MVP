package com.example.service;

import com.example.domain.model.Payment;
import com.example.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("UC3: Pruebas de Procesamiento de Pagos - TDD")
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    private Payment paymentTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        paymentTest = new Payment();
        paymentTest.setId(1);
        paymentTest.setOrderId(100);
        paymentTest.setAmount(new BigDecimal("55.00"));
        paymentTest.setStatus(Payment.PaymentStatus.PAID);
        paymentTest.setPaymentMethodId(5);
        paymentTest.setTransactionId("TXN_12345");
        paymentTest.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("UC3.1: Crear pago exitosamente")
    void testCrearPagoExitoso() {
        when(paymentRepository.save(paymentTest)).thenReturn(paymentTest);
        Payment pagoCreado = paymentService.crearPayment(paymentTest);
        assertNotNull(pagoCreado);
        assertEquals(Payment.PaymentStatus.PAID, pagoCreado.getStatus());
        assertEquals(new BigDecimal("55.00"), pagoCreado.getAmount());
        verify(paymentRepository, times(1)).save(paymentTest);
    }

    @Test
    @DisplayName("UC3.2: Obtener pago por ID")
    void testObtenerPagoPorId() {
        when(paymentRepository.findById(1)).thenReturn(Optional.of(paymentTest));
        Optional<Payment> pagoEncontrado = paymentService.obtenerPorId(1);
        assertTrue(pagoEncontrado.isPresent());
        assertEquals(100, pagoEncontrado.get().getOrderId());
        assertEquals("TXN_12345", pagoEncontrado.get().getTransactionId());
        verify(paymentRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("UC3.3: Obtener pago por Order ID")
    void testObtenerPagoPorOrderId() {
        when(paymentRepository.findByOrderId(100)).thenReturn(Optional.of(paymentTest));
        Optional<Payment> pagoEncontrado = paymentService.obtenerPorOrderId(100);
        assertTrue(pagoEncontrado.isPresent());
        assertEquals(new BigDecimal("55.00"), pagoEncontrado.get().getAmount());
        verify(paymentRepository, times(1)).findByOrderId(100);
    }

    @Test
    @DisplayName("UC3.4: Actualizar pago existente")
    void testActualizarPagoExistente() {
        when(paymentRepository.existsById(1)).thenReturn(true);
        when(paymentRepository.save(paymentTest)).thenReturn(paymentTest);
        Payment pagoActualizado = paymentService.actualizarPayment(1, paymentTest);
        assertNotNull(pagoActualizado);
        assertEquals(1, pagoActualizado.getId());
        verify(paymentRepository, times(1)).existsById(1);
        verify(paymentRepository, times(1)).save(paymentTest);
    }

    @Test
    @DisplayName("UC3.5: Error al actualizar pago no existente")
    void testActualizarPagoNoExistente() {
        when(paymentRepository.existsById(999)).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
            () -> paymentService.actualizarPayment(999, paymentTest));
        verify(paymentRepository, times(1)).existsById(999);
    }

    @Test
    @DisplayName("UC3.6: Eliminar pago existente")
    void testEliminarPagoExistente() {
        when(paymentRepository.existsById(1)).thenReturn(true);
        doNothing().when(paymentRepository).deleteById(1);
        paymentService.eliminarPayment(1);
        verify(paymentRepository, times(1)).existsById(1);
        verify(paymentRepository, times(1)).deleteById(1);
    }

    @Test
    @DisplayName("UC3.7: Error al eliminar pago no existente")
    void testEliminarPagoNoExistente() {
        when(paymentRepository.existsById(999)).thenReturn(false);
        assertThrows(IllegalArgumentException.class,
            () -> paymentService.eliminarPayment(999));
        verify(paymentRepository, times(1)).existsById(999);
    }

    @Test
    @DisplayName("UC3.8: Listar todos los pagos")
    void testListarTodosLosPagos() {
        List<Payment> pagos = new ArrayList<>();
        pagos.add(paymentTest);
        when(paymentRepository.findAll()).thenReturn(pagos);
        List<Payment> pagosEncontrados = paymentService.listarPayments();
        assertEquals(1, pagosEncontrados.size());
        verify(paymentRepository, times(1)).findAll();
    }
}
