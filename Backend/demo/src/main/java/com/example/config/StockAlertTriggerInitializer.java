package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * Inicializa automáticamente en SQL Server las tablas y triggers T-SQL
 * encargados de detectar cada ENTRADA y SALIDA de inventario para determinar
 * faltantes de stock y encolar alertas por correo electrónico.
 */
@Component
@Order(20)
public class StockAlertTriggerInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(StockAlertTriggerInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public StockAlertTriggerInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        if (!isSqlServer()) {
            log.info("Motor distinto a SQL Server detectado; se omite inicialización de triggers T-SQL de stock.");
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("sql/stock_alert_triggers.sql");
            String fullScript;
            try (InputStream is = resource.getInputStream()) {
                fullScript = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            String[] batches = fullScript.split("(?im)^\\s*GO\\s*$");
            int executed = 0;
            for (String rawBatch : batches) {
                String batch = rawBatch.trim();
                if (!batch.isEmpty()) {
                    jdbcTemplate.execute(batch);
                    executed++;
                }
            }
            log.info("Triggers T-SQL de detección de entradas/salidas y alertas de stock verificados ({} bloques ejecutados).", executed);
        } catch (Exception e) {
            log.error("Error al inicializar triggers T-SQL de alertas de stock: {}", e.getMessage(), e);
        }
    }

    private boolean isSqlServer() {
        try (Connection conn = dataSource.getConnection()) {
            String productName = conn.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("microsoft sql server");
        } catch (Exception e) {
            log.warn("No se pudo verificar el motor de BD para triggers de stock: {}", e.getMessage());
            return false;
        }
    }
}

