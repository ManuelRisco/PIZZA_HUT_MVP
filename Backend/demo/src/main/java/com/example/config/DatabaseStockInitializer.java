package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Asegura la unificación y consistencia del stock de masa por tamaño en la base de datos.
 * Todas las pizzas (Pepperoni, Suprema, Hawaiana, etc.) comparten el mismo stock de masa
 * disponible para cada tamaño comercial (Personal, Mediana, Familiar, etc.).
 */
@Component
public class DatabaseStockInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStockInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public DatabaseStockInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            // 1. Modificar stock_movements y product_losses para usar VARCHAR(30) en vez de ENUM rígido
            try {
                jdbcTemplate.execute("ALTER TABLE `stock_movements` MODIFY COLUMN `itemType` VARCHAR(30) NOT NULL");
                log.info("Columna stock_movements.itemType convertida exitosamente a VARCHAR(30).");
            } catch (Exception e) {
                log.warn("Aviso al modificar stock_movements.itemType: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE `stock_movements` MODIFY COLUMN `movementType` VARCHAR(30) NOT NULL");
                log.info("Columna stock_movements.movementType convertida exitosamente a VARCHAR(30).");
            } catch (Exception e) {
                log.warn("Aviso al modificar stock_movements.movementType: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE `product_losses` MODIFY COLUMN `itemType` VARCHAR(30) NOT NULL");
                log.info("Columna product_losses.itemType convertida exitosamente a VARCHAR(30).");
            } catch (Exception e) {
                log.warn("Aviso al modificar product_losses.itemType: {}", e.getMessage());
            }

            try {
                jdbcTemplate.execute("ALTER TABLE `product_losses` MODIFY COLUMN `category` VARCHAR(30) NOT NULL");
                log.info("Columna product_losses.category convertida exitosamente a VARCHAR(30).");
            } catch (Exception e) {
                log.warn("Aviso al modificar product_losses.category: {}", e.getMessage());
            }

            // 2. Agregar columnas sizeId y orderId en tablas que lo requieran
            try {
                jdbcTemplate.execute("ALTER TABLE `stock_movements` ADD COLUMN `sizeId` INT NULL");
                log.info("Columna sizeId agregada a tabla stock_movements.");
            } catch (Exception ignored) {
                // Ya existe la columna
            }

            try {
                jdbcTemplate.execute("ALTER TABLE `product_losses` ADD COLUMN `sizeId` INT NULL");
                log.info("Columna sizeId agregada a tabla product_losses.");
            } catch (Exception ignored) {
                // Ya existe la columna
            }

            try {
                jdbcTemplate.execute("ALTER TABLE `product_losses` ADD COLUMN `orderId` INT NULL");
                log.info("Columna orderId agregada a tabla product_losses.");
            } catch (Exception ignored) {
                // Ya existe la columna
            }

            // 3. Agregar columnas stockQuantity y minStock en tabla sizes si no existen
            try {
                jdbcTemplate.execute("ALTER TABLE `sizes` ADD COLUMN `stockQuantity` INT NOT NULL DEFAULT 0");
                log.info("Columna stockQuantity agregada a tabla sizes.");
            } catch (Exception ignored) {
                // Ya existe la columna
            }

            try {
                jdbcTemplate.execute("ALTER TABLE `sizes` ADD COLUMN `minStock` INT NOT NULL DEFAULT 0");
                log.info("Columna minStock agregada a tabla sizes.");
            } catch (Exception ignored) {
                // Ya existe la columna
            }

            // 4. Agregar columnas de stock en tabla orders y order_items si no existen
            try {
                jdbcTemplate.execute("ALTER TABLE `orders` ADD COLUMN `stockSettled` VARCHAR(20) NOT NULL DEFAULT 'DEDUCTED'");
                log.info("Columna stockSettled agregada a tabla orders.");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("ALTER TABLE `order_items` ADD COLUMN `unitCost` DECIMAL(10,2) NOT NULL DEFAULT 0.00");
                log.info("Columna unitCost agregada a tabla order_items.");
            } catch (Exception ignored) {}

            try {
                jdbcTemplate.execute("ALTER TABLE `order_items` ADD COLUMN `lineCost` DECIMAL(10,2) NOT NULL DEFAULT 0.00");
                log.info("Columna lineCost agregada a tabla order_items.");
            } catch (Exception ignored) {}

            log.info("Verificación de estructura y columnas de stock completada.");
        } catch (Exception e) {
            log.warn("Aviso en DatabaseStockInitializer: {}", e.getMessage());
        }
    }
}
