package com.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Inicializa las columnas de DNI (RENIEC), Boleta de Venta Electrónica (SUNAT / Nubefact),
 * caché local de consultas RENIEC y limpia direcciones duplicadas en SQL Server.
 */
@Component
@Order(10)
public class SunatAndAddressDedupInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SunatAndAddressDedupInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public SunatAndAddressDedupInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        if (!isSqlServer()) {
            return;
        }
        try {
            ensureUserDniColumns();
            ensureOrderBoletaColumns();
            ensureReniecCacheTable();
            deduplicateCustomerAddresses();
        } catch (Exception e) {
            log.error("Error durante la inicializacion de columnas SUNAT/RENIEC y deduplicacion de direcciones: {}", e.getMessage(), e);
        }
    }

    private void ensureUserDniColumns() {
        jdbcTemplate.execute("""
            IF COL_LENGTH('dbo.users', 'dni') IS NULL
            BEGIN
                ALTER TABLE dbo.users ADD dni VARCHAR(15) NULL;
            END
            """);
        jdbcTemplate.execute("""
            IF COL_LENGTH('dbo.users', 'age_verified') IS NULL
            BEGIN
                ALTER TABLE dbo.users ADD age_verified BIT NOT NULL CONSTRAINT DF_users_age_verified DEFAULT 0;
            END
            """);
    }

    private void ensureOrderBoletaColumns() {
        String[] ddlStatements = new String[]{
            "IF COL_LENGTH('dbo.orders', 'wants_boleta') IS NULL ALTER TABLE dbo.orders ADD wants_boleta BIT NOT NULL CONSTRAINT DF_orders_wants_boleta DEFAULT 0;",
            "IF COL_LENGTH('dbo.orders', 'boleta_tipo_comprobante') IS NULL ALTER TABLE dbo.orders ADD boleta_tipo_comprobante INT NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_serie') IS NULL ALTER TABLE dbo.orders ADD boleta_serie VARCHAR(10) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_numero') IS NULL ALTER TABLE dbo.orders ADD boleta_numero INT NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_cliente_dni') IS NULL ALTER TABLE dbo.orders ADD boleta_cliente_dni VARCHAR(15) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_cliente_nombre') IS NULL ALTER TABLE dbo.orders ADD boleta_cliente_nombre VARCHAR(200) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_cliente_direccion') IS NULL ALTER TABLE dbo.orders ADD boleta_cliente_direccion VARCHAR(255) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_pdf_url') IS NULL ALTER TABLE dbo.orders ADD boleta_pdf_url VARCHAR(500) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_xml_url') IS NULL ALTER TABLE dbo.orders ADD boleta_xml_url VARCHAR(500) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_cdr_url') IS NULL ALTER TABLE dbo.orders ADD boleta_cdr_url VARCHAR(500) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_enlace') IS NULL ALTER TABLE dbo.orders ADD boleta_enlace VARCHAR(500) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_hash') IS NULL ALTER TABLE dbo.orders ADD boleta_hash VARCHAR(255) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_qr') IS NULL ALTER TABLE dbo.orders ADD boleta_qr VARCHAR(500) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_estado') IS NULL ALTER TABLE dbo.orders ADD boleta_estado VARCHAR(40) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_error') IS NULL ALTER TABLE dbo.orders ADD boleta_error VARCHAR(500) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_total_gravada') IS NULL ALTER TABLE dbo.orders ADD boleta_total_gravada DECIMAL(10,2) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_total_igv') IS NULL ALTER TABLE dbo.orders ADD boleta_total_igv DECIMAL(10,2) NULL;",
            "IF COL_LENGTH('dbo.orders', 'boleta_emitida_at') IS NULL ALTER TABLE dbo.orders ADD boleta_emitida_at DATETIME2 NULL;"
        };
        for (String sql : ddlStatements) {
            jdbcTemplate.execute(sql);
        }
    }

    private void ensureReniecCacheTable() {
        jdbcTemplate.execute("""
            IF OBJECT_ID('dbo.reniec_dni_cache', 'U') IS NULL
            BEGIN
                CREATE TABLE dbo.reniec_dni_cache (
                    dni VARCHAR(15) NOT NULL PRIMARY KEY,
                    nombres VARCHAR(120) NOT NULL,
                    apellido_paterno VARCHAR(120) NOT NULL,
                    apellido_materno VARCHAR(120) NOT NULL,
                    nombre_completo VARCHAR(255) NOT NULL,
                    updated_at DATETIME2 NOT NULL DEFAULT SYSDATETIME()
                );
            END
            """);
        jdbcTemplate.execute("""
            IF NOT EXISTS (SELECT 1 FROM dbo.reniec_dni_cache WHERE dni = '76949746')
            BEGIN
                INSERT INTO dbo.reniec_dni_cache (dni, nombres, apellido_paterno, apellido_materno, nombre_completo)
                VALUES ('76949746', 'MANUEL FABRIZZIO', 'RISCO', 'GIL', 'MANUEL FABRIZZIO RISCO GIL');
            END
            """);
    }

    private void deduplicateCustomerAddresses() {
        // 1. Re-apuntar pedidos que usen direcciones duplicadas hacia el ID canónico (MIN(id)) del mismo usuario y dirección
        int updatedOrders = jdbcTemplate.update("""
            ;WITH AddressGroups AS (
                SELECT
                    id,
                    userId,
                    MIN(id) OVER (
                        PARTITION BY
                            userId,
                            LOWER(LTRIM(RTRIM(ISNULL(line1, '')))),
                            LOWER(LTRIM(RTRIM(ISNULL(city, '')))),
                            LOWER(LTRIM(RTRIM(ISNULL(district, ''))))
                    ) AS canonical_id
                FROM dbo.addresses
                WHERE deletedAt IS NULL
            )
            UPDATE o
            SET o.addressId = ag.canonical_id
            FROM dbo.orders o
            INNER JOIN AddressGroups ag ON o.addressId = ag.id
            WHERE ag.id <> ag.canonical_id;
            """);

        // 2. Eliminar las filas duplicadas redundantes que ya no están referenciadas en orders
        int removedDuplicates = jdbcTemplate.update("""
            ;WITH AddressGroups AS (
                SELECT
                    id,
                    MIN(id) OVER (
                        PARTITION BY
                            userId,
                            LOWER(LTRIM(RTRIM(ISNULL(line1, '')))),
                            LOWER(LTRIM(RTRIM(ISNULL(city, '')))),
                            LOWER(LTRIM(RTRIM(ISNULL(district, ''))))
                    ) AS canonical_id
                FROM dbo.addresses
                WHERE deletedAt IS NULL
            )
            DELETE a
            FROM dbo.addresses a
            INNER JOIN AddressGroups ag ON a.id = ag.id
            WHERE ag.id <> ag.canonical_id
              AND NOT EXISTS (SELECT 1 FROM dbo.orders o WHERE o.addressId = a.id);
            """);

        if (updatedOrders > 0 || removedDuplicates > 0) {
            log.info("Deduplicación de direcciones completada: {} pedidos reasignados a dirección canónica, {} direcciones duplicadas eliminadas.",
                    updatedOrders, removedDuplicates);
        }
    }

    private boolean isSqlServer() {
        try (Connection conn = dataSource.getConnection()) {
            String productName = conn.getMetaData().getDatabaseProductName();
            return productName != null && productName.toLowerCase().contains("microsoft sql server");
        } catch (Exception e) {
            return false;
        }
    }
}
