package com.example.services;

import com.example.models.Payment;
import com.example.repositories.PaymentRepository;
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

@DisplayName("CU17: Ver lista de pagos - Pruebas TDD")
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

    // ==========================================
    // CU17: Ver lista de pagos
    // ==========================================

    @Test
    @DisplayName("CU17 - Flujo Principal: Listar todos los pagos [RF31]")
    void testListarTodosLosPagos() {
        List<Payment> pagos = new ArrayList<>();
        pagos.add(paymentTest);
        when(paymentRepository.findAll()).thenReturn(pagos);

        List<Payment> pagosEncontrados = paymentService.listarPayments();

        assertEquals(1, pagosEncontrados.size());
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU17 - Flujo Principal: Filtrar/Obtener pago por Order ID [RF32]")
    void testObtenerPagoPorOrderId() {
        when(paymentRepository.findByOrderId(100)).thenReturn(Optional.of(paymentTest));

        Optional<Payment> pagoEncontrado = paymentService.obtenerPorOrderId(100);

        assertTrue(pagoEncontrado.isPresent());
        assertEquals(new BigDecimal("55.00"), pagoEncontrado.get().getAmount());
        verify(paymentRepository, times(1)).findByOrderId(100);
    }

    @Test
    @DisplayName("CU17 - A1: Validar error/lista vacía cuando no hay pagos")
    void testListarPagosVacio() {
        when(paymentRepository.findAll()).thenReturn(new ArrayList<>());

        List<Payment> pagosEncontrados = paymentService.listarPayments();

        assertTrue(pagosEncontrados.isEmpty());
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("CU17 - A2: Filtros de búsqueda sin resultados (por Order ID)")
    void testObtenerPagoPorOrderIdNoExistente() {
        when(paymentRepository.findByOrderId(999)).thenReturn(Optional.empty());

        Optional<Payment> pagoEncontrado = paymentService.obtenerPorOrderId(999);

        assertFalse(pagoEncontrado.isPresent());
        verify(paymentRepository, times(1)).findByOrderId(999);
    }

    // ==========================================
    // Otros métodos de gestión
    // ==========================================

    @Test
    @DisplayName("Adicional: Crear pago exitosamente")
    void testCrearPagoExitoso() {
        when(paymentRepository.save(paymentTest)).thenReturn(paymentTest);

        Payment pagoCreado = paymentService.crearPayment(paymentTest);

        assertNotNull(pagoCreado);
        assertEquals(Payment.PaymentStatus.PAID, pagoCreado.getStatus());
        verify(paymentRepository, times(1)).save(paymentTest);
    }
}
