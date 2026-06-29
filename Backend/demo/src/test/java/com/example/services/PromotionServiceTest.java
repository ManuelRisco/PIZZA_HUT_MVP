package com.example.services;

import com.example.models.Promotion;
import com.example.repositories.OrderRepository;
import com.example.repositories.PromotionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("CU06: Gestión de Promociones - Pruebas TDD")

class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PromotionService promotionService;

    private Promotion promotionTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        promotionTest = new Promotion();
        promotionTest.setId(1);
        promotionTest.setCode("PIZZA20");
        promotionTest.setName("20% Descuento en Pizzas");
        promotionTest.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        promotionTest.setDiscountValue(new BigDecimal("20.00"));
        promotionTest.setUsageLimit(100);
        promotionTest.setUsageCount(10);
        promotionTest.setIsActive(true);
        promotionTest.setStartDate(LocalDateTime.now().minusDays(1));
        promotionTest.setEndDate(LocalDateTime.now().plusDays(30));
    }

    // ==========================================
    // CU06: Ver Promociones
    // ==========================================

    @Test
    @DisplayName("CU06 - Flujo Principal: Listar promociones activas [RF11]")
    void testObtenerPromocionesActivas() {
        List<Promotion> promociones = new ArrayList<>();
        promociones.add(promotionTest);
        when(promotionRepository.findByDeletedAtIsNull()).thenReturn(promociones);
        
        List<Promotion> promocionesEncontradas = promotionService.listarPromociones();
        
        assertEquals(1, promocionesEncontradas.size());
        assertTrue(promocionesEncontradas.get(0).getIsActive());
        verify(promotionRepository, times(1)).findByDeletedAtIsNull();
    }

    @Test
    @DisplayName("CU06 - Flujo Principal: Aplicar descuento válido a total [RF11]")
    void testAplicarDescuentoValido() {
        when(promotionRepository.findByCodeAndDeletedAtIsNull("PIZZA20")).thenReturn(Optional.of(promotionTest));
        
        BigDecimal montoTotal = new BigDecimal("100.00");
        Map<String, Object> resultado = promotionService.validarPromocionParaUsuario("PIZZA20", montoTotal, 1);
        
        assertTrue((Boolean) resultado.get("valid"), "Se esperaba true pero fue: " + resultado.get("message"));
        assertEquals(0, new BigDecimal("20.00").compareTo((BigDecimal) resultado.get("discount")));
        assertEquals(0, new BigDecimal("80.00").compareTo((BigDecimal) resultado.get("finalTotal")));
    }

    @Test
    @DisplayName("CU06 - A2: Intentar aplicar promoción vencida o inactiva")
    void testAplicarPromocionVencida() {
        promotionTest.setIsActive(false); // Inactivar
        when(promotionRepository.findByCodeAndDeletedAtIsNull("PIZZA20")).thenReturn(Optional.of(promotionTest));
        
        BigDecimal montoTotal = new BigDecimal("100.00");
        Map<String, Object> resultado = promotionService.validarPromocionParaUsuario("PIZZA20", montoTotal, 1);
        
        assertFalse((Boolean) resultado.get("valid"));
        assertEquals("Esta promoción no está disponible actualmente", resultado.get("message"));
    }

    @Test
    @DisplayName("CU06 - A3: Filtro/Código de promoción sin resultados")
    void testAplicarPromocionNoExistente() {
        when(promotionRepository.findByCodeAndDeletedAtIsNull("INVENTADO")).thenReturn(Optional.empty());
        
        BigDecimal montoTotal = new BigDecimal("100.00");
        Map<String, Object> resultado = promotionService.validarPromocionParaUsuario("INVENTADO", montoTotal, 1);
        
        assertFalse((Boolean) resultado.get("valid"));
        assertEquals("Código de promoción no válido", resultado.get("message"));
    }

    @Test
    @DisplayName("CU06 - Adicional: Validar límite de usos superado")
    void testValidarLimiteUsosSuperado() {
        promotionTest.setUsageLimit(10);
        promotionTest.setUsageCount(10); // Límite alcanzado
        when(promotionRepository.findByCodeAndDeletedAtIsNull("PIZZA20")).thenReturn(Optional.of(promotionTest));
        
        Map<String, Object> resultado = promotionService.validarPromocionParaUsuario("PIZZA20", new BigDecimal("100.00"), 1);
        
        assertFalse((Boolean) resultado.get("valid"));
        assertEquals("Esta promoción no está disponible actualmente", resultado.get("message"));
    }
}
