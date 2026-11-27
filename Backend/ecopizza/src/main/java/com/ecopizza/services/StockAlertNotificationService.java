package com.ecopizza.services;

import com.ecopizza.dtos.StockAlertDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Consolida durante una ventana de 1 minuto (60s) todas las alertas de falta de stock
 * generadas por los triggers T-SQL y el catalogo en vivo, enviandolas en un unico correo conjunto.
 * Una vez enviado el correo de un producto, no vuelve a enviarse otro correo para ese
 * producto hasta que su stock sea restablecido superando estrictamente su minStock.
 */
@Service
public class StockAlertNotificationService {

    private static final Logger log = LoggerFactory.getLogger(StockAlertNotificationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final EmailService emailService;
    private final ReentrantLock dispatchLock = new ReentrantLock();

    private volatile Boolean sqlServerSupported;
    private volatile long backoffUntilEpochMs = 0L;

    @Value("${app.stock-alerts.poll-enabled:true}")
    private boolean pollEnabled;

    @Value("${app.stock-alerts.batch-window-seconds:60}")
    private int batchWindowSeconds;

    public StockAlertNotificationService(JdbcTemplate jdbcTemplate,
                                         DataSource dataSource,
                                         EmailService emailService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelayString = "${app.stock-alerts.poll-ms:5000}", initialDelay = 6000)
    public void pollPendingStockAlerts() {
        if (!pollEnabled) {
            return;
        }
        processPendingAlerts(false);
    }

    @Async("mailTaskExecutor")
    public void synchronizeAlertsAsync() {
        if (!isStockAlertsTableAvailable()) {
            return;
        }
        try {
            synchronizeAlertsWithLiveCatalog();
        } catch (Exception ex) {
            log.debug("Sincronizaci\u00f3n de alertas omitida: {}", ex.getMessage());
        }
    }

    @Async("mailTaskExecutor")
    public void dispatchPendingAlertsAsync() {
        processPendingAlerts(false);
    }

    public int processPendingAlerts() {
        return processPendingAlerts(false);
    }

    public int processPendingAlerts(boolean forceImmediate) {
        if (!forceImmediate && System.currentTimeMillis() < backoffUntilEpochMs) {
            return 0;
        }
        if (!isStockAlertsTableAvailable()) {
            return 0;
        }
        if (!dispatchLock.tryLock()) {
            return 0;
        }

        try {
            synchronizeAlertsWithLiveCatalog();

            if (!emailService.isMailOperational()) {
                return 0;
            }

            if (!forceImmediate && batchWindowSeconds > 0) {
                Integer oldestAgeSec = jdbcTemplate.queryForObject(
                        "SELECT DATEDIFF(SECOND, MIN(createdAt), SYSDATETIME()) FROM dbo.stock_alerts WHERE emailSent = 0 AND isResolved = 0",
                        Integer.class);
                if (oldestAgeSec == null || oldestAgeSec < batchWindowSeconds) {
                    return 0;
                }
            }

            String selectSql = """
                SELECT TOP 200
                    id, itemType, itemId, itemName, unit,
                    movementDirection, movementType,
                    previousStock, movementQuantity, newStock, minStock, deficitAmount,
                    alertLevel, referenceType, referenceId, reason,
                    isResolved, emailSent, emailRecipients, emailError,
                    createdAt, sentAt, resolvedAt, resolvedByStock
                FROM dbo.stock_alerts
                WHERE emailSent = 0 AND isResolved = 0
                ORDER BY CASE WHEN alertLevel = 'OUT_OF_STOCK' THEN 0 ELSE 1 END, createdAt ASC, id ASC
                """;

            List<StockAlertDTO> rawPending = jdbcTemplate.query(selectSql, (rs, rowNum) -> mapStockAlert(rs));
            if (rawPending.isEmpty()) {
                return 0;
            }

            Map<String, StockAlertDTO> uniqueByItem = new LinkedHashMap<>();
            List<Integer> allIdsToMark = new ArrayList<>();
            for (StockAlertDTO dto : rawPending) {
                allIdsToMark.add(dto.getId());
                String key = (dto.getItemType() != null ? dto.getItemType().toUpperCase() : "") + ":" + dto.getItemId();
                uniqueByItem.putIfAbsent(key, dto);
            }

            List<StockAlertDTO> consolidatedAlerts = new ArrayList<>(uniqueByItem.values());
            String placeholders = allIdsToMark.stream().map(id -> "?").collect(Collectors.joining(","));

            try {
                List<String> recipients = emailService.sendStockShortageAlertEmail(consolidatedAlerts);
                if (!recipients.isEmpty()) {
                    backoffUntilEpochMs = 0L;
                    String recipientsCsv = String.join(", ", recipients);
                    String updateSql = "UPDATE dbo.stock_alerts SET emailSent = 1, sentAt = SYSDATETIME(), emailRecipients = ?, emailError = NULL WHERE id IN (" + placeholders + ")";
                    Object[] params = new Object[allIdsToMark.size() + 1];
                    params[0] = recipientsCsv;
                    for (int i = 0; i < allIdsToMark.size(); i++) {
                        params[i + 1] = allIdsToMark.get(i);
                    }
                    jdbcTemplate.update(updateSql, params);
                    return consolidatedAlerts.size();
                }
            } catch (Exception mailEx) {
                backoffUntilEpochMs = System.currentTimeMillis() + 60_000L;
                log.warn("No se pudo despachar correo consolidado de alerta de stock (reintento en 60s): {}", mailEx.getMessage());
                String errMsg = mailEx.getMessage() != null
                        ? mailEx.getMessage().substring(0, Math.min(480, mailEx.getMessage().length()))
                        : "Error SMTP desconocido";
                String errSql = "UPDATE dbo.stock_alerts SET emailError = ? WHERE id IN (" + placeholders + ")";
                Object[] params = new Object[allIdsToMark.size() + 1];
                params[0] = errMsg;
                for (int i = 0; i < allIdsToMark.size(); i++) {
                    params[i + 1] = allIdsToMark.get(i);
                }
                jdbcTemplate.update(errSql, params);
            }
            return 0;
        } catch (Exception ex) {
            log.debug("Consulta de stock_alerts omitida o con aviso: {}", ex.getMessage());
            return 0;
        } finally {
            dispatchLock.unlock();
        }
    }

    /**
     * Sincroniza las alertas activas (isResolved = 0) con el stock en vivo de ingredients, extras y sizes:
     * 1-3. Si algun producto ya supero su minStock (> minStock y > 0) o fue eliminado, marca isResolved = 1.
     * 4. Mantiene actualizados newStock, minStock, deficitAmount y alertLevel para el lote consolidado.
     * 5. Incorpora al lote cualquier producto activo en o debajo de minStock que aun no tenga episodio activo.
     */
    private void synchronizeAlertsWithLiveCatalog() {
        String syncSql = """
            -- 1. Resolver alertas de ingredientes cuyo stock actual ya supero el minimo o fueron eliminados
            UPDATE sa
            SET sa.isResolved = 1,
                sa.resolvedAt = SYSDATETIME(),
                sa.resolvedByStock = CAST(ing.stockQuantity AS DECIMAL(12,2)),
                sa.emailError = CASE
                    WHEN sa.emailSent = 0 THEN N'Stock repuesto sobre el m\u00ednimo antes del env\u00edo del lote'
                    ELSE sa.emailError
                END
            FROM dbo.stock_alerts sa
            LEFT JOIN dbo.ingredients ing ON sa.itemId = ing.id
            WHERE sa.isResolved = 0
              AND sa.itemType = N'INGREDIENT'
              AND (
                  ing.id IS NULL
                  OR ing.deletedAt IS NOT NULL
                  OR (CAST(ing.stockQuantity AS DECIMAL(12,2)) > ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00)
                      AND CAST(ing.stockQuantity AS DECIMAL(12,2)) > 0)
              );

            -- 2. Resolver alertas de extras cuyo stock actual ya supero el minimo o fueron eliminados
            UPDATE sa
            SET sa.isResolved = 1,
                sa.resolvedAt = SYSDATETIME(),
                sa.resolvedByStock = CAST(ext.stockQuantity AS DECIMAL(12,2)),
                sa.emailError = CASE
                    WHEN sa.emailSent = 0 THEN N'Stock repuesto sobre el m\u00ednimo antes del env\u00edo del lote'
                    ELSE sa.emailError
                END
            FROM dbo.stock_alerts sa
            LEFT JOIN dbo.extras ext ON sa.itemId = ext.id
            WHERE sa.isResolved = 0
              AND sa.itemType = N'EXTRA'
              AND (
                  ext.id IS NULL
                  OR ext.deletedAt IS NOT NULL
                  OR (ext.stockQuantity > ISNULL(ext.minStock, 5) AND ext.stockQuantity > 0)
              );

            -- 3. Resolver alertas de masas (sizes) cuyo stock actual ya supero el minimo o fueron eliminadas
            UPDATE sa
            SET sa.isResolved = 1,
                sa.resolvedAt = SYSDATETIME(),
                sa.resolvedByStock = CAST(sz.stockQuantity AS DECIMAL(12,2)),
                sa.emailError = CASE
                    WHEN sa.emailSent = 0 THEN N'Stock repuesto sobre el m\u00ednimo antes del env\u00edo del lote'
                    ELSE sa.emailError
                END
            FROM dbo.stock_alerts sa
            LEFT JOIN dbo.sizes sz ON sa.itemId = sz.id
            WHERE sa.isResolved = 0
              AND sa.itemType = N'SIZE'
              AND (
                  sz.id IS NULL
                  OR sz.deletedAt IS NOT NULL
                  OR (sz.stockQuantity > ISNULL(sz.minStock, 10) AND sz.stockQuantity > 0)
              );

            -- 4. Refrescar cantidades exactas en alertas activas pendientes (emailSent = 0)
            UPDATE sa
            SET sa.newStock = CAST(ing.stockQuantity AS DECIMAL(12,2)),
                sa.minStock = ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00),
                sa.deficitAmount = CASE
                    WHEN ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00) > CAST(ing.stockQuantity AS DECIMAL(12,2))
                        THEN ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00) - CAST(ing.stockQuantity AS DECIMAL(12,2))
                    ELSE 0.00
                END,
                sa.alertLevel = CASE WHEN CAST(ing.stockQuantity AS DECIMAL(12,2)) <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END
            FROM dbo.stock_alerts sa
            INNER JOIN dbo.ingredients ing ON sa.itemId = ing.id
            WHERE sa.isResolved = 0 AND sa.emailSent = 0 AND sa.itemType = N'INGREDIENT';

            UPDATE sa
            SET sa.newStock = CAST(ext.stockQuantity AS DECIMAL(12,2)),
                sa.minStock = CAST(ISNULL(ext.minStock, 5) AS DECIMAL(12,2)),
                sa.deficitAmount = CASE
                    WHEN ISNULL(ext.minStock, 5) > ext.stockQuantity
                        THEN CAST(ISNULL(ext.minStock, 5) - ext.stockQuantity AS DECIMAL(12,2))
                    ELSE 0.00
                END,
                sa.alertLevel = CASE WHEN ext.stockQuantity <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END
            FROM dbo.stock_alerts sa
            INNER JOIN dbo.extras ext ON sa.itemId = ext.id
            WHERE sa.isResolved = 0 AND sa.emailSent = 0 AND sa.itemType = N'EXTRA';

            -- 5. Incorporar cualquier insumo o extra actualmente <= minStock que aun no tenga episodio activo (isResolved = 0)
            INSERT INTO dbo.stock_alerts (
                itemType, itemId, itemName, unit,
                movementDirection, movementType,
                previousStock, movementQuantity, newStock, minStock, deficitAmount,
                alertLevel, reason
            )
            SELECT
                N'INGREDIENT',
                ing.id,
                ing.name,
                ISNULL(ing.unit, N'g'),
                N'SALIDA',
                N'CATALOG_SYNC',
                CAST(ing.stockQuantity AS DECIMAL(12,2)),
                0.00,
                CAST(ing.stockQuantity AS DECIMAL(12,2)),
                ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00),
                CASE
                    WHEN ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00) > CAST(ing.stockQuantity AS DECIMAL(12,2))
                        THEN ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00) - CAST(ing.stockQuantity AS DECIMAL(12,2))
                    ELSE 0.00
                END,
                CASE WHEN CAST(ing.stockQuantity AS DECIMAL(12,2)) <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END,
                N'Stock actual en o debajo del m\u00ednimo configurado'
            FROM dbo.ingredients ing
            WHERE ing.deletedAt IS NULL
              AND (CAST(ing.stockQuantity AS DECIMAL(12,2)) <= 0
                   OR CAST(ing.stockQuantity AS DECIMAL(12,2)) <= ISNULL(CAST(ing.minStock AS DECIMAL(12,2)), 0.00))
              AND NOT EXISTS (
                  SELECT 1 FROM dbo.stock_alerts sa
                  WHERE sa.itemType = N'INGREDIENT' AND sa.itemId = ing.id AND sa.isResolved = 0
              );

            INSERT INTO dbo.stock_alerts (
                itemType, itemId, itemName, unit,
                movementDirection, movementType,
                previousStock, movementQuantity, newStock, minStock, deficitAmount,
                alertLevel, reason
            )
            SELECT
                N'EXTRA',
                ext.id,
                ext.name,
                N'und',
                N'SALIDA',
                N'CATALOG_SYNC',
                CAST(ext.stockQuantity AS DECIMAL(12,2)),
                0.00,
                CAST(ext.stockQuantity AS DECIMAL(12,2)),
                CAST(ISNULL(ext.minStock, 5) AS DECIMAL(12,2)),
                CASE
                    WHEN ISNULL(ext.minStock, 5) > ext.stockQuantity
                        THEN CAST(ISNULL(ext.minStock, 5) - ext.stockQuantity AS DECIMAL(12,2))
                    ELSE 0.00
                END,
                CASE WHEN ext.stockQuantity <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END,
                N'Stock actual en o debajo del m\u00ednimo configurado'
            FROM dbo.extras ext
            WHERE ext.deletedAt IS NULL
              AND (ext.stockQuantity <= 0 OR ext.stockQuantity <= ISNULL(ext.minStock, 5))
              AND NOT EXISTS (
                  SELECT 1 FROM dbo.stock_alerts sa
                  WHERE sa.itemType = N'EXTRA' AND sa.itemId = ext.id AND sa.isResolved = 0
              );
            """;
        jdbcTemplate.execute(syncSql);
    }

    public Map<String, Object> getBatchNotificationStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("mailOperational", emailService.isMailOperational());
        status.put("batchWindowSeconds", batchWindowSeconds);

        if (!isStockAlertsTableAvailable()) {
            status.put("pendingBatchCount", 0);
            status.put("secondsRemaining", 0);
            status.put("notifiedActiveCount", 0);
            status.put("lastSentAt", null);
            status.put("lastRecipients", null);
            status.put("activeAlerts", List.of());
            return status;
        }

        try {
            synchronizeAlertsWithLiveCatalog();

            String activeSql = """
                SELECT
                    id, itemType, itemId, itemName, unit,
                    movementDirection, movementType,
                    previousStock, movementQuantity, newStock, minStock, deficitAmount,
                    alertLevel, referenceType, referenceId, reason,
                    isResolved, emailSent, emailRecipients, emailError,
                    createdAt, sentAt, resolvedAt, resolvedByStock
                FROM dbo.stock_alerts
                WHERE isResolved = 0
                ORDER BY emailSent ASC, createdAt ASC
                """;
            List<StockAlertDTO> activeAlerts = jdbcTemplate.query(activeSql, (rs, rowNum) -> mapStockAlert(rs));

            long pendingCount = activeAlerts.stream().filter(a -> !a.isEmailSent()).count();
            long notifiedActiveCount = activeAlerts.stream().filter(StockAlertDTO::isEmailSent).count();

            int secondsRemaining = 0;
            if (pendingCount > 0 && batchWindowSeconds > 0) {
                Integer oldestAgeSec = jdbcTemplate.queryForObject(
                        "SELECT DATEDIFF(SECOND, MIN(createdAt), SYSDATETIME()) FROM dbo.stock_alerts WHERE emailSent = 0 AND isResolved = 0",
                        Integer.class);
                int age = oldestAgeSec != null ? Math.max(0, oldestAgeSec) : 0;
                secondsRemaining = Math.max(0, batchWindowSeconds - age);
            }

            Map<String, Object> lastSentInfo = jdbcTemplate.query(
                    "SELECT TOP 1 sentAt, emailRecipients FROM dbo.stock_alerts WHERE emailSent = 1 AND sentAt IS NOT NULL ORDER BY sentAt DESC",
                    rs -> {
                        Map<String, Object> info = new LinkedHashMap<>();
                        if (rs.next()) {
                            Timestamp ts = rs.getTimestamp("sentAt");
                            info.put("lastSentAt", ts != null ? ts.toLocalDateTime() : null);
                            info.put("lastRecipients", rs.getString("emailRecipients"));
                        }
                        return info;
                    });

            status.put("pendingBatchCount", pendingCount);
            status.put("secondsRemaining", secondsRemaining);
            status.put("notifiedActiveCount", notifiedActiveCount);
            status.put("lastSentAt", lastSentInfo != null ? lastSentInfo.get("lastSentAt") : null);
            status.put("lastRecipients", lastSentInfo != null ? lastSentInfo.get("lastRecipients") : null);
            status.put("activeAlerts", activeAlerts);
        } catch (Exception ex) {
            log.debug("No se pudo obtener estado de lote de alertas: {}", ex.getMessage());
            status.put("pendingBatchCount", 0);
            status.put("secondsRemaining", 0);
            status.put("notifiedActiveCount", 0);
            status.put("activeAlerts", List.of());
        }
        return status;
    }

    public List<StockAlertDTO> getRecentAlerts(Integer limit) {
        if (!isStockAlertsTableAvailable()) {
            return List.of();
        }
        int safeLimit = (limit != null && limit > 0) ? Math.min(limit, 200) : 50;
        String sql = """
            SELECT TOP (%d)
                id, itemType, itemId, itemName, unit,
                movementDirection, movementType,
                previousStock, movementQuantity, newStock, minStock, deficitAmount,
                alertLevel, referenceType, referenceId, reason,
                isResolved, emailSent, emailRecipients, emailError,
                createdAt, sentAt, resolvedAt, resolvedByStock
            FROM dbo.stock_alerts
            ORDER BY createdAt DESC
            """.formatted(safeLimit);
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapStockAlert(rs));
    }

    public List<Map<String, Object>> getRecentTriggerEvents(Integer limit) {
        if (!isStockAlertsTableAvailable()) {
            return List.of();
        }
        int safeLimit = (limit != null && limit > 0) ? Math.min(limit, 200) : 50;
        String sql = """
            SELECT TOP (%d)
                id, itemType, itemId, itemName, unit, direction, movementType,
                previousStock, deltaQuantity, newStock, minStock, stockStatus,
                referenceType, referenceId, reason, createdAt
            FROM dbo.stock_trigger_events
            ORDER BY createdAt DESC
            """.formatted(safeLimit);
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("itemType", rs.getString("itemType"));
            row.put("itemId", rs.getInt("itemId"));
            row.put("itemName", rs.getString("itemName"));
            row.put("unit", rs.getString("unit"));
            row.put("direction", rs.getString("direction"));
            row.put("movementType", rs.getString("movementType"));
            row.put("previousStock", rs.getBigDecimal("previousStock"));
            row.put("deltaQuantity", rs.getBigDecimal("deltaQuantity"));
            row.put("newStock", rs.getBigDecimal("newStock"));
            row.put("minStock", rs.getBigDecimal("minStock"));
            row.put("stockStatus", rs.getString("stockStatus"));
            row.put("referenceType", rs.getString("referenceType"));
            row.put("referenceId", rs.getObject("referenceId"));
            row.put("reason", rs.getString("reason"));
            Timestamp created = rs.getTimestamp("createdAt");
            row.put("createdAt", created != null ? created.toLocalDateTime() : null);
            return row;
        });
    }

    private boolean isStockAlertsTableAvailable() {
        Boolean cached = this.sqlServerSupported;
        if (cached != null) {
            return cached;
        }
        try (Connection conn = dataSource.getConnection()) {
            String product = conn.getMetaData().getDatabaseProductName();
            boolean isMssql = product != null && product.toLowerCase().contains("microsoft sql server");
            if (!isMssql) {
                this.sqlServerSupported = false;
                return false;
            }
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT CASE WHEN OBJECT_ID(N'dbo.stock_alerts', N'U') IS NOT NULL THEN 1 ELSE 0 END",
                    Integer.class);
            boolean ready = exists != null && exists == 1;
            if (ready) {
                this.sqlServerSupported = true;
            }
            return ready;
        } catch (Exception ex) {
            return false;
        }
    }

    private StockAlertDTO mapStockAlert(ResultSet rs) throws SQLException {
        StockAlertDTO dto = new StockAlertDTO();
        dto.setId(rs.getInt("id"));
        dto.setItemType(rs.getString("itemType"));
        dto.setItemId(rs.getInt("itemId"));
        dto.setItemName(rs.getString("itemName"));
        dto.setUnit(rs.getString("unit"));
        dto.setMovementDirection(rs.getString("movementDirection"));
        dto.setMovementType(rs.getString("movementType"));
        dto.setPreviousStock(rs.getBigDecimal("previousStock"));
        dto.setMovementQuantity(rs.getBigDecimal("movementQuantity"));
        dto.setNewStock(rs.getBigDecimal("newStock"));
        dto.setMinStock(rs.getBigDecimal("minStock"));
        dto.setDeficitAmount(rs.getBigDecimal("deficitAmount"));
        dto.setAlertLevel(rs.getString("alertLevel"));
        dto.setReferenceType(rs.getString("referenceType"));
        dto.setReferenceId(rs.getObject("referenceId") != null ? rs.getInt("referenceId") : null);
        dto.setReason(rs.getString("reason"));
        dto.setResolved(rs.getBoolean("isResolved"));
        dto.setEmailSent(rs.getBoolean("emailSent"));
        dto.setEmailRecipients(rs.getString("emailRecipients"));
        dto.setEmailError(rs.getString("emailError"));
        Timestamp created = rs.getTimestamp("createdAt");
        dto.setCreatedAt(created != null ? created.toLocalDateTime() : null);
        Timestamp sent = rs.getTimestamp("sentAt");
        dto.setSentAt(sent != null ? sent.toLocalDateTime() : null);
        Timestamp resolved = rs.getTimestamp("resolvedAt");
        dto.setResolvedAt(resolved != null ? resolved.toLocalDateTime() : null);
        dto.setResolvedByStock(rs.getBigDecimal("resolvedByStock"));
        return dto;
    }
}

