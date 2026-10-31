package com.example.integration;

import com.example.dtos.CheckoutRequestDTO;
import com.example.dtos.OrderDTO;
import com.example.models.Pizza;
import com.example.models.Size;
import com.example.models.Usuario;
import com.example.repositories.PizzaRepository;
import com.example.repositories.SizeRepository;
import com.example.repositories.UsuarioRepository;
import com.example.services.CheckoutService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("Test de Integración de Concurrencia: Prevención de Oversell y Bloqueo Pesimista en Checkout")
public class CheckoutConcurrencyIntegrationTest {

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private PizzaRepository pizzaRepository;

    @Autowired
    private SizeRepository sizeRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("Cuando 2 clientes piden la última pizza a la vez, solo uno tiene éxito y el otro recibe error de stock insuficiente")
    void testDosPersonasPidiendoUltimaPizzaSimultaneamente() throws Exception {
        // 1. Obtener o crear usuarios de prueba
        Usuario user1 = usuarioRepository.findByEmail("cliente_test1@pizzahut.com")
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setName("Cliente Uno");
                    u.setEmail("cliente_test1@pizzahut.com");
                    u.setPasswordHash("$2a$10$N.ZOmG4byf24u5U5u5u5u.abcdefghijklmnopqrstuvw");
                    u.setRole(Usuario.Role.CUSTOMER);
                    return usuarioRepository.save(u);
                });

        Usuario user2 = usuarioRepository.findByEmail("cliente_test2@pizzahut.com")
                .orElseGet(() -> {
                    Usuario u = new Usuario();
                    u.setName("Cliente Dos");
                    u.setEmail("cliente_test2@pizzahut.com");
                    u.setPasswordHash("$2a$10$N.ZOmG4byf24u5U5u5u5u.abcdefghijklmnopqrstuvw");
                    u.setRole(Usuario.Role.CUSTOMER);
                    return usuarioRepository.save(u);
                });

        // 2. Obtener una pizza activa existente o crear una
        Pizza pizza = pizzaRepository.findByDeletedAtIsNull().stream().findFirst()
                .orElseGet(() -> {
                    Pizza p = new Pizza();
                    p.setName("Pizza Concurrencia");
                    p.setPrice(new BigDecimal("35.00"));
                    p.setIsAvailable(true);
                    return pizzaRepository.save(p);
                });

        // 3. Crear un tamaño exclusivo para esta prueba con stockQuantity EXACTAMENTE =
        // 1 masa
        Size sizeTest = new Size();
        sizeTest.setName("Tamano Concurrencia " + System.currentTimeMillis());
        sizeTest.setExtraCost(BigDecimal.ZERO);
        sizeTest.setRecipeMultiplier(BigDecimal.ONE);
        sizeTest.setStockQuantity(1); // EXACTAMENTE 1 en stock
        sizeTest.setMinStock(0);
        sizeTest.setDisplayOrder(99);
        sizeTest = sizeRepository.save(sizeTest);

        Integer sizeId = sizeTest.getId();

        // 4. Preparar DTOs para ambos usuarios que compiten por la misma y única pizza
        CheckoutRequestDTO req1 = buildCheckoutRequest(user1.getId().intValue(), pizza.getId(), sizeId);
        CheckoutRequestDTO req2 = buildCheckoutRequest(user2.getId().intValue(), pizza.getId(), sizeId);

        // 5. Ejecutar concurrentemente usando dos hilos y CountDownLatch para disparar
        // al mismo instante
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger exitos = new AtomicInteger(0);
        AtomicInteger fallosStock = new AtomicInteger(0);

        Callable<Void> task1 = () -> {
            latch.await();
            try {
                checkoutService.checkout(req1);
                exitos.incrementAndGet();
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("Stock insuficiente")) {
                    fallosStock.incrementAndGet();
                } else {
                    System.err.println("Task1 IllegalStateException: " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.err.println("Task1 Exception: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        };

        Callable<Void> task2 = () -> {
            latch.await();
            try {
                checkoutService.checkout(req2);
                exitos.incrementAndGet();
            } catch (IllegalStateException e) {
                if (e.getMessage() != null && e.getMessage().contains("Stock insuficiente")) {
                    fallosStock.incrementAndGet();
                } else {
                    System.err.println("Task2 IllegalStateException: " + e.getMessage());
                    e.printStackTrace();
                }
            } catch (Exception e) {
                System.err.println("Task2 Exception: " + e.getMessage());
                e.printStackTrace();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        // Disparo simultáneo
        latch.countDown();

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        executor.shutdown();

        // 6. Verificaciones estrictas
        // EXACTAMENTE uno de los dos debe haber completado el pedido
        assertEquals(1, exitos.get(), "Exactamente uno de los dos pedidos debe tener éxito");

        // EXACTAMENTE uno de los dos debe haber sido rechazado con mensaje de falta de
        // stock
        assertEquals(1, fallosStock.get(), "El segundo pedido debe ser rechazado por stock insuficiente");

        // El stock final en base de datos debe ser exactamente 0 (NUNCA negativo ni
        // oversell)
        Size sizeFinal = sizeRepository.findById(sizeId).orElseThrow();
        assertEquals(0, sizeFinal.getStockQuantity(), "El stock final de masa debe ser exactamente 0");
    }

    private CheckoutRequestDTO buildCheckoutRequest(Integer userId, Integer pizzaId, Integer sizeId) {
        CheckoutRequestDTO req = new CheckoutRequestDTO();

        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setUserId(userId);
        orderDTO.setDeliveryType("PICKUP");
        orderDTO.setPaymentMethodId(1);
        orderDTO.setSubtotal(new BigDecimal("35.00"));
        orderDTO.setTotal(new BigDecimal("35.00"));
        orderDTO.setStatus("PENDING");
        req.setOrder(orderDTO);

        CheckoutRequestDTO.CheckoutItemDTO item = new CheckoutRequestDTO.CheckoutItemDTO();
        item.setItemType("PIZZA");
        item.setPizzaId(pizzaId);
        item.setSizeId(sizeId);
        item.setQuantity(1);
        item.setUnitPrice(new BigDecimal("35.00"));
        item.setLineTotal(new BigDecimal("35.00"));
        req.setItems(List.of(item));

        CheckoutRequestDTO.PaymentDTO payment = new CheckoutRequestDTO.PaymentDTO();
        payment.setPaymentMethodId(1);
        payment.setAmount(new BigDecimal("35.00"));
        payment.setStatus("PAID");
        req.setPayment(payment);

        return req;
    }
}
