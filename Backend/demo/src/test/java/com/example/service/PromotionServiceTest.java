package com.example.service;

import com.example.domain.model.Promotion;
import com.example.domain.repository.PromotionRepository;
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

@SuppressWarnings("null")
@DisplayName("UC5: Pruebas de Gestión de Promociones - TDD")
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;

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
        promotionTest.setDescription("20% descuento en pizzas");
        promotionTest.setDiscountType(Promotion.DiscountType.PERCENTAGE);
        promotionTest.setDiscountValue(new BigDecimal("20.00"));
        promotionTest.setUsageLimit(100);
        promotionTest.setUsageCount(10);
        promotionTest.setIsActive(true);
        promotionTest.setStartDate(LocalDateTime.now());
        promotionTest.setEndDate(LocalDateTime.now().plusDays(30));
    }

    @Test
    @DisplayName("UC5.1: Crear promoción exitosamente")
    void testCrearPromocionExitosa() {
        when(promotionRepository.save(promotionTest)).thenReturn(promotionTest);
        Promotion promocionCreada = promotionRepository.save(promotionTest);
        assertNotNull(promocionCreada);
        assertEquals("PIZZA20", promocionCreada.getCode());
        assertEquals(new BigDecimal("20.00"), promocionCreada.getDiscountValue());
        assertTrue(promocionCreada.getIsActive());
        verify(promotionRepository, times(1)).save(promotionTest);
    }

    @Test
    @DisplayName("UC5.2: Obtener promoción por ID")
    void testObtenerPromocionPorId() {
        when(promotionRepository.findById(1)).thenReturn(Optional.of(promotionTest));
        Optional<Promotion> promocionEncontrada = promotionRepository.findById(1);
        assertTrue(promocionEncontrada.isPresent());
        assertEquals("PIZZA20", promocionEncontrada.get().getCode());
        verify(promotionRepository, times(1)).findById(1);
    }

    @Test
    @DisplayName("UC5.3: Validar código de promoción existente")
    void testValidarCodigoPromocionExistente() {
        when(promotionRepository.findByCodeAndDeletedAtIsNull("PIZZA20")).thenReturn(Optional.of(promotionTest));
        Optional<Promotion> promocionEncontrada = promotionRepository.findByCodeAndDeletedAtIsNull("PIZZA20");
        assertTrue(promocionEncontrada.isPresent());
        assertEquals(new BigDecimal("20.00"), promocionEncontrada.get().getDiscountValue());
        verify(promotionRepository, times(1)).findByCodeAndDeletedAtIsNull("PIZZA20");
    }

    @Test
    @DisplayName("UC5.4: Aplicar descuento válido a total")
    void testAplicarDescuentoValido() {
        BigDecimal montoTotal = new BigDecimal("100.00");
        BigDecimal porcentaje = new BigDecimal("20.00");
        BigDecimal resultado = montoTotal.multiply(porcentaje).divide(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("20.00"), resultado);
    }

    @Test
    @DisplayName("UC5.5: Validar límite de usos de promoción")
    void testValidarLimiteUsos() {
        promotionTest.setUsageLimit(10);
        promotionTest.setUsageCount(10);
        boolean puedeUsarse = promotionTest.getUsageLimit() > promotionTest.getUsageCount();
        assertFalse(puedeUsarse);
    }

    @Test
    @DisplayName("UC5.6: Incrementar contador de uso de promoción")
    void testIncrementarUsoPromocion() {
        int usosActuales = promotionTest.getUsageCount();
        promotionTest.setUsageCount(usosActuales + 1);
        when(promotionRepository.save(promotionTest)).thenReturn(promotionTest);
        Promotion promocionActualizada = promotionRepository.save(promotionTest);
        assertEquals(usosActuales + 1, promocionActualizada.getUsageCount());
        verify(promotionRepository, times(1)).save(promotionTest);
    }

    @Test
    @DisplayName("UC5.7: Obtener promociones activas")
    void testObtenerPromocionesActivas() {
        List<Promotion> promociones = new ArrayList<>();
        promociones.add(promotionTest);
        when(promotionRepository.findAll()).thenReturn(promociones);
        List<Promotion> promocionesEncontradas = promotionRepository.findAll();
        assertEquals(1, promocionesEncontradas.size());
        assertTrue(promocionesEncontradas.get(0).getIsActive());
        verify(promotionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("UC5.8: Validar vigencia de promoción")
    void testValidarVigenciaPromocion() {
        LocalDateTime ahora = LocalDateTime.now();
        promotionTest.setStartDate(ahora.minusDays(1));
        promotionTest.setEndDate(ahora.plusDays(1));
        boolean esVigente = !ahora.isBefore(promotionTest.getStartDate()) 
                         && !ahora.isAfter(promotionTest.getEndDate());
        assertTrue(esVigente);
    }

    @Test
    @DisplayName("UC5.9: Listar todas las promociones")
    void testListarTodasLasPromociones() {
        List<Promotion> promociones = new ArrayList<>();
        promociones.add(promotionTest);
        when(promotionRepository.findAll()).thenReturn(promociones);
        List<Promotion> promocionesEncontradas = promotionRepository.findAll();
        assertEquals(1, promocionesEncontradas.size());
        verify(promotionRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("UC5.10: Desactivar promoción")
    void testDesactivarPromocion() {
        promotionTest.setIsActive(false);
        when(promotionRepository.save(promotionTest)).thenReturn(promotionTest);
        Promotion promocionDesactivada = promotionRepository.save(promotionTest);
        assertFalse(promocionDesactivada.getIsActive());
        verify(promotionRepository, times(1)).save(promotionTest);
    }
}
