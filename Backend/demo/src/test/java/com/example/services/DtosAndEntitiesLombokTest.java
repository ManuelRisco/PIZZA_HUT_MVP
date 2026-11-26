package com.example.services;

import com.example.dtos.DeliveryFeeConfigDTO;
import com.example.dtos.MercadoPagoConfigDTO;
import com.example.dtos.PasswordResetRequestResultDTO;
import com.example.models.AuditLog;
import com.example.models.FavoriteId;
import com.example.models.Ingredient;
import com.example.models.Pizza;
import com.example.models.PizzaIngredient;
import com.example.models.PizzaIngredientId;
import com.example.models.Size;
import com.example.repositories.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Verificación de DTOs consolidados y entidades JPA con Lombok")
class DtosAndEntitiesLombokTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PaymentMethodService paymentMethodService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("DeliveryFeeConfigDTO omite message en GET y lo incluye en PUT con withMessage")
    void deliveryFeeConfigDtoJsonCompatibility() throws Exception {
        DeliveryFeeConfigDTO getDto = new DeliveryFeeConfigDTO(new BigDecimal("7.50"), "PEN", null);
        JsonNode getJson = objectMapper.readTree(objectMapper.writeValueAsString(getDto));

        assertEquals(0, new BigDecimal("7.50").compareTo(getJson.get("deliveryFee").decimalValue()));
        assertEquals("PEN", getJson.get("currency").asText());
        assertTrue(getJson.has("updatedAt"));
        assertFalse(getJson.has("message"), "GET no debe incluir message cuando es null");

        DeliveryFeeConfigDTO putDto = getDto.withMessage("Costo de delivery actualizado correctamente");
        JsonNode putJson = objectMapper.readTree(objectMapper.writeValueAsString(putDto));
        assertEquals("Costo de delivery actualizado correctamente", putJson.get("message").asText());
    }

    @Test
    @DisplayName("StoreSettingsService usa DeliveryFeeConfigDTO al actualizar tarifa")
    void storeSettingsServiceReturnsDto() {
        when(jdbcTemplate.update(anyString(), eq("8.50"), eq("DELIVERY_FEE"))).thenReturn(1);
        StoreSettingsService service = new StoreSettingsService(jdbcTemplate);

        DeliveryFeeConfigDTO updated = service.updateDeliveryFee(new BigDecimal("8.5"));
        assertEquals(new BigDecimal("8.50"), updated.deliveryFee());
        assertEquals("PEN", updated.currency());
        assertNotNull(updated.updatedAt());
        assertNull(updated.message());
    }

    @Test
    @DisplayName("MercadoPagoService devuelve MercadoPagoConfigDTO tipado")
    void mercadoPagoServiceReturnsConfigDto() {
        MercadoPagoService service = new MercadoPagoService(usuarioRepository, paymentMethodService);
        ReflectionTestUtils.setField(service, "publicKey", "TEST-public-key-123");
        ReflectionTestUtils.setField(service, "accessToken", "TEST-access-token-456");
        ReflectionTestUtils.setField(service, "sandboxMode", true);

        MercadoPagoConfigDTO cfg = service.obtenerConfiguracionPublica();
        assertTrue(cfg.configured());
        assertEquals("TEST-public-key-123", cfg.publicKey());
        assertTrue(cfg.sandboxMode());
        assertEquals("PEN", cfg.currency());
        assertEquals("es-PE", cfg.locale());
        assertEquals(List.of("CARD", "YAPE"), cfg.supportedGateways());
    }

    @Test
    @DisplayName("PasswordResetRequestResultDTO excluye message del JSON manteniendo el dato interno")
    void passwordResetResultJsonOmitsInternalMessage() throws Exception {
        PasswordResetRequestResultDTO dto = new PasswordResetRequestResultDTO(true, "ma***l@gmail.com", 15, "Enviado");
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(dto));

        assertTrue(json.get("emailSent").asBoolean());
        assertEquals("ma***l@gmail.com", json.get("maskedEmail").asText());
        assertEquals(15, json.get("expirationMinutes").asInt());
        assertFalse(json.has("message"));
        assertEquals("Enviado", dto.message());
    }

    @Test
    @DisplayName("Entidades con Lombok preservan getters/setters, builders, equals/hashCode y propiedades JSON")
    void lombokEntitiesCompatibility() throws Exception {
        // AuditLog @Builder + @Builder.Default
        AuditLog audit = AuditLog.builder()
                .userId(10)
                .actionType(AuditLog.ActionType.CREATE)
                .description("Test")
                .build();
        assertEquals(10, audit.getUserId());
        assertEquals(AuditLog.Status.SUCCESS, audit.getStatus());
        assertNotNull(audit.getCreatedAt());

        // Embeddable IDs @EqualsAndHashCode
        assertEquals(new FavoriteId(1, 2), new FavoriteId(1, 2));
        assertEquals(new FavoriteId(1, 2).hashCode(), new FavoriteId(1, 2).hashCode());
        assertEquals(new PizzaIngredientId(3, 4), new PizzaIngredientId(3, 4));
        assertEquals(new PizzaIngredientId(3, 4).hashCode(), new PizzaIngredientId(3, 4).hashCode());

        // Ingredient primitive boolean getters
        Ingredient ing = new Ingredient();
        ing.setAvailable(false);
        ing.setAllowAsExtra(false);
        assertFalse(ing.isAvailable());
        assertFalse(ing.isAllowAsExtra());

        // Size null-safe getters
        Size size = new Size();
        size.setStockQuantity(null);
        size.setMinStock(null);
        assertEquals(0, size.getStockQuantity());
        assertEquals(0, size.getMinStock());

        // Pizza & PizzaIngredient JSON boolean property names (no duplicate "available"/"isAvailable")
        Pizza pizza = new Pizza();
        pizza.setName("Americana");
        pizza.setIsAvailable(true);
        pizza.setIsPopular(false);
        JsonNode pizzaJson = objectMapper.readTree(objectMapper.writeValueAsString(pizza));
        assertTrue(pizzaJson.has("isAvailable"));
        assertTrue(pizzaJson.has("isPopular"));
        assertFalse(pizzaJson.has("available"), "No debe duplicarse la propiedad JSON 'available'");
        assertFalse(pizzaJson.has("popular"), "No debe duplicarse la propiedad JSON 'popular'");

        PizzaIngredient pi = new PizzaIngredient();
        pi.setIsDefault(true);
        pi.setCreatedAt(null);
        JsonNode piJson = objectMapper.readTree(objectMapper.writeValueAsString(pi));
        assertTrue(piJson.has("isDefault"));
        assertFalse(piJson.has("default"), "No debe duplicarse la propiedad JSON 'default'");
    }
}
