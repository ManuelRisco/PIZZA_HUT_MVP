package com.ecopizza.services;

import com.ecopizza.dtos.DeliveryFeeConfigDTO;
import com.ecopizza.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StoreSettingsService {

    private static final Logger log = LoggerFactory.getLogger(StoreSettingsService.class);
    public static final String KEY_DELIVERY_FEE = "DELIVERY_FEE";
    public static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("5.00");
    private static final BigDecimal MAX_DELIVERY_FEE = new BigDecimal("500.00");

    private final JdbcTemplate jdbcTemplate;
    private volatile BigDecimal cachedDeliveryFee;
    private volatile LocalDateTime cachedUpdatedAt;

    public StoreSettingsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public BigDecimal getDeliveryFee() {
        BigDecimal cached = this.cachedDeliveryFee;
        if (cached != null) {
            return cached;
        }
        return loadDeliveryFeeFromDb().deliveryFee();
    }

    public DeliveryFeeConfigDTO getDeliveryFeeConfig() {
        BigDecimal cached = this.cachedDeliveryFee;
        if (cached != null) {
            return new DeliveryFeeConfigDTO(cached, "PEN", this.cachedUpdatedAt);
        }
        return loadDeliveryFeeFromDb();
    }

    public synchronized DeliveryFeeConfigDTO updateDeliveryFee(BigDecimal rawFee) {
        if (rawFee == null) {
            throw new BadRequestException("El costo de delivery es obligatorio.");
        }
        BigDecimal normalized = rawFee.setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("El costo de delivery no puede ser negativo.");
        }
        if (normalized.compareTo(MAX_DELIVERY_FEE) > 0) {
            throw new BadRequestException("El costo de delivery no puede superar S/ 500.00.");
        }

        int updated = jdbcTemplate.update(
                "UPDATE app_settings SET setting_value = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?",
                normalized.toPlainString(),
                KEY_DELIVERY_FEE
        );
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO app_settings (setting_key, setting_value, description, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                    KEY_DELIVERY_FEE,
                    normalized.toPlainString(),
                    "Tarifa estandar de envio a domicilio (PEN)"
            );
        }

        LocalDateTime now = LocalDateTime.now();
        this.cachedDeliveryFee = normalized;
        this.cachedUpdatedAt = now;
        log.info("Tarifa de delivery actualizada a S/ {}", normalized.toPlainString());
        return new DeliveryFeeConfigDTO(normalized, "PEN", now);
    }

    private DeliveryFeeConfigDTO loadDeliveryFeeFromDb() {
        try {
            List<DeliveryFeeConfigDTO> rows = jdbcTemplate.query(
                    "SELECT setting_value, updated_at FROM app_settings WHERE setting_key = ?",
                    (rs, rowNum) -> {
                        String rawVal = rs.getString("setting_value");
                        Timestamp ts = rs.getTimestamp("updated_at");
                        BigDecimal parsed = parseFeeOrDefault(rawVal);
                        LocalDateTime updatedAt = ts != null ? ts.toLocalDateTime() : null;
                        return new DeliveryFeeConfigDTO(parsed, "PEN", updatedAt);
                    },
                    KEY_DELIVERY_FEE
            );
            if (!rows.isEmpty()) {
                DeliveryFeeConfigDTO cfg = rows.get(0);
                this.cachedDeliveryFee = cfg.deliveryFee();
                this.cachedUpdatedAt = cfg.updatedAt();
                return cfg;
            }
        } catch (Exception e) {
            log.warn("No se pudo leer DELIVERY_FEE desde app_settings, usando valor por defecto S/ {}: {}",
                    DEFAULT_DELIVERY_FEE, e.getMessage());
        }
        this.cachedDeliveryFee = DEFAULT_DELIVERY_FEE;
        return new DeliveryFeeConfigDTO(DEFAULT_DELIVERY_FEE, "PEN", null);
    }

    private BigDecimal parseFeeOrDefault(String rawVal) {
        if (rawVal == null || rawVal.isBlank()) {
            return DEFAULT_DELIVERY_FEE;
        }
        try {
            BigDecimal val = new BigDecimal(rawVal.trim()).setScale(2, RoundingMode.HALF_UP);
            return val.compareTo(BigDecimal.ZERO) >= 0 ? val : DEFAULT_DELIVERY_FEE;
        } catch (NumberFormatException ex) {
            return DEFAULT_DELIVERY_FEE;
        }
    }
}
