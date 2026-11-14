/* ==========================================================================
   SISTEMA DE CONTROL DE FLUJO DE STOCK Y ALERTAS AUTOMÁTICAS (T-SQL)
   Detecta cada ENTRADA y SALIDA en inventario (ingredients, extras, sizes
   y kardex stock_movements) para determinar falta de stock y disparar alertas.
   Regla de no-repetición: una vez generada/enviada la alerta de un ítem, NO se
   vuelve a generar otra alerta para ese ítem hasta que su stock se restablezca
   superando estrictamente el stock mínimo (stockQuantity > minStock).
   ========================================================================== */

IF OBJECT_ID(N'[dbo].[stock_trigger_events]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[stock_trigger_events] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [itemType] NVARCHAR(30) NOT NULL,
        [itemId] INT NOT NULL,
        [itemName] NVARCHAR(150) NOT NULL,
        [unit] NVARCHAR(20) NOT NULL,
        [direction] NVARCHAR(20) NOT NULL,
        [movementType] NVARCHAR(30) NULL,
        [previousStock] DECIMAL(12,2) NOT NULL,
        [deltaQuantity] DECIMAL(12,2) NOT NULL,
        [newStock] DECIMAL(12,2) NOT NULL,
        [minStock] DECIMAL(12,2) NOT NULL,
        [stockStatus] NVARCHAR(20) NOT NULL,
        [referenceType] NVARCHAR(30) NULL,
        [referenceId] INT NULL,
        [reason] NVARCHAR(255) NULL,
        [createdAt] DATETIME2 NOT NULL DEFAULT SYSDATETIME()
    );

    CREATE INDEX [IX_stock_trigger_events_item_created]
        ON [dbo].[stock_trigger_events] ([itemType], [itemId], [createdAt] DESC);
END;
GO

IF OBJECT_ID(N'[dbo].[stock_alerts]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[stock_alerts] (
        [id] INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [itemType] NVARCHAR(30) NOT NULL,
        [itemId] INT NOT NULL,
        [itemName] NVARCHAR(150) NOT NULL,
        [unit] NVARCHAR(20) NOT NULL,
        [movementDirection] NVARCHAR(20) NOT NULL,
        [movementType] NVARCHAR(30) NULL,
        [previousStock] DECIMAL(12,2) NOT NULL,
        [movementQuantity] DECIMAL(12,2) NOT NULL,
        [newStock] DECIMAL(12,2) NOT NULL,
        [minStock] DECIMAL(12,2) NOT NULL,
        [deficitAmount] DECIMAL(12,2) NOT NULL,
        [alertLevel] NVARCHAR(20) NOT NULL,
        [referenceType] NVARCHAR(30) NULL,
        [referenceId] INT NULL,
        [reason] NVARCHAR(255) NULL,
        [isResolved] BIT NOT NULL DEFAULT 0,
        [emailSent] BIT NOT NULL DEFAULT 0,
        [emailRecipients] NVARCHAR(500) NULL,
        [emailError] NVARCHAR(500) NULL,
        [createdAt] DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        [sentAt] DATETIME2 NULL,
        [resolvedAt] DATETIME2 NULL,
        [resolvedByStock] DECIMAL(12,2) NULL
    );

    CREATE INDEX [IX_stock_alerts_pending_email]
        ON [dbo].[stock_alerts] ([emailSent], [isResolved], [createdAt] ASC);

    CREATE INDEX [IX_stock_alerts_active_item]
        ON [dbo].[stock_alerts] ([itemType], [itemId], [isResolved]);
END;
GO

IF OBJECT_ID(N'[dbo].[password_reset_tokens]', N'U') IS NULL
BEGIN
    CREATE TABLE [dbo].[password_reset_tokens] (
        [id] BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        [userId] INT NOT NULL,
        [email] NVARCHAR(255) NOT NULL,
        [codeHash] NVARCHAR(255) NOT NULL,
        [expiresAt] DATETIME2 NOT NULL,
        [attempts] INT NOT NULL DEFAULT 0,
        [used] BIT NOT NULL DEFAULT 0,
        [createdAt] DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        [usedAt] DATETIME2 NULL,
        CONSTRAINT [FK_password_reset_tokens_users] FOREIGN KEY ([userId]) REFERENCES [dbo].[users]([id]) ON DELETE CASCADE
    );

    CREATE INDEX [IX_password_reset_tokens_email_active]
        ON [dbo].[password_reset_tokens] ([email], [used], [expiresAt] DESC);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_session_logs_token' AND object_id = OBJECT_ID(N'[dbo].[session_logs]'))
BEGIN
    CREATE NONCLUSTERED INDEX [idx_session_logs_token] ON [dbo].[session_logs]([session_token]) INCLUDE ([is_active]);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_sm_size' AND object_id = OBJECT_ID(N'[dbo].[stock_movements]'))
BEGIN
    CREATE NONCLUSTERED INDEX [idx_sm_size] ON [dbo].[stock_movements]([sizeId]);
END;
GO

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name = N'FK_sm_size')
BEGIN
    ALTER TABLE [dbo].[stock_movements] ADD CONSTRAINT [FK_sm_size] FOREIGN KEY ([sizeId]) REFERENCES [dbo].[sizes]([id]);
END;
GO

IF COL_LENGTH('dbo.users', 'google_linked') IS NULL
BEGIN
    ALTER TABLE [dbo].[users] ADD [google_linked] BIT NOT NULL CONSTRAINT [DF_users_google_linked] DEFAULT 0;
END;
GO

IF COL_LENGTH('dbo.users', 'auth_provider') IS NULL
BEGIN
    ALTER TABLE [dbo].[users] ADD [auth_provider] NVARCHAR(30) NOT NULL CONSTRAINT [DF_users_auth_provider] DEFAULT N'LOCAL';
END;
GO

IF COL_LENGTH('dbo.users', 'password_set') IS NULL
BEGIN
    ALTER TABLE [dbo].[users] ADD [password_set] BIT NOT NULL CONSTRAINT [DF_users_password_set] DEFAULT 1;
END;
GO

SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

-- Si existía índice filtrado previo, reemplazar por índice estándar compatible con cualquier cliente SQL
IF EXISTS (
    SELECT 1 FROM sys.indexes
    WHERE name = N'UX_stock_alerts_one_active_per_item'
      AND object_id = OBJECT_ID(N'[dbo].[stock_alerts]')
)
BEGIN
    DROP INDEX [UX_stock_alerts_one_active_per_item] ON [dbo].[stock_alerts];
END;
GO

-- Limpiar posibles alertas activas duplicadas previas conservando la ya enviada por correo si existe
;WITH DuplicateActiveAlerts AS (
    SELECT
        [id],
        ROW_NUMBER() OVER (
            PARTITION BY [itemType], [itemId]
            ORDER BY [emailSent] DESC, [createdAt] ASC, [id] ASC
        ) AS rn
    FROM [dbo].[stock_alerts]
    WHERE [isResolved] = 0
)
UPDATE sa
SET sa.[isResolved] = 1,
    sa.[resolvedAt] = SYSDATETIME(),
    sa.[emailError] = COALESCE(sa.[emailError], N'Alerta duplicada consolidada')
FROM [dbo].[stock_alerts] sa
INNER JOIN DuplicateActiveAlerts d ON sa.[id] = d.[id]
WHERE d.rn > 1;
GO

-- ==========================================================================
-- TRIGGER 1: Detección en Kardex (stock_movements) de cada ENTRADA y SALIDA
-- ==========================================================================
CREATE OR ALTER TRIGGER [dbo].[trg_stock_movements_stock_alert]
ON [dbo].[stock_movements]
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH MovimientosPorItem AS (
        SELECT
            UPPER(i.[itemType]) AS itemType,
            COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) AS itemId,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(ing.[name], CONCAT(N'Ingrediente #', i.[ingredientId]))
                WHEN UPPER(i.[itemType]) = N'EXTRA' THEN COALESCE(ext.[name], CONCAT(N'Extra #', i.[extraId]))
                WHEN UPPER(i.[itemType]) = N'SIZE' THEN CONCAT(N'Masa ', COALESCE(sz.[name], CONCAT(N'#', i.[sizeId])))
                ELSE N'Ítem Inventario'
            END AS itemName,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(ing.[unit], N'g')
                ELSE N'und'
            END AS unit,
            CASE
                WHEN CAST(i.[newStock] AS DECIMAL(12,2)) > CAST(i.[previousStock] AS DECIMAL(12,2)) THEN N'ENTRADA'
                WHEN CAST(i.[newStock] AS DECIMAL(12,2)) < CAST(i.[previousStock] AS DECIMAL(12,2)) THEN N'SALIDA'
                ELSE N'AJUSTE'
            END AS direction,
            i.[movementType],
            CAST(i.[previousStock] AS DECIMAL(12,2)) AS previousStock,
            CAST(i.[newStock] AS DECIMAL(12,2)) - CAST(i.[previousStock] AS DECIMAL(12,2)) AS deltaQuantity,
            CAST(i.[newStock] AS DECIMAL(12,2)) AS newStock,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(CAST(ing.[minStock] AS DECIMAL(12,2)), 0.00)
                WHEN UPPER(i.[itemType]) = N'EXTRA' THEN COALESCE(CAST(ext.[minStock] AS DECIMAL(12,2)), 5.00)
                WHEN UPPER(i.[itemType]) = N'SIZE' THEN COALESCE(CAST(sz.[minStock] AS DECIMAL(12,2)), 10.00)
                ELSE 0.00
            END AS minStock,
            i.[referenceType],
            i.[referenceId],
            i.[reason],
            ROW_NUMBER() OVER (
                PARTITION BY UPPER(i.[itemType]), COALESCE(i.[ingredientId], i.[extraId], i.[sizeId])
                ORDER BY i.[id] DESC
            ) AS rn
        FROM inserted i
        LEFT JOIN [dbo].[ingredients] ing ON UPPER(i.[itemType]) = N'INGREDIENT' AND i.[ingredientId] = ing.[id]
        LEFT JOIN [dbo].[extras] ext ON UPPER(i.[itemType]) = N'EXTRA' AND i.[extraId] = ext.[id]
        LEFT JOIN [dbo].[sizes] sz ON UPPER(i.[itemType]) = N'SIZE' AND i.[sizeId] = sz.[id]
        WHERE COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) IS NOT NULL
    )
    INSERT INTO [dbo].[stock_trigger_events] (
        [itemType], [itemId], [itemName], [unit], [direction], [movementType],
        [previousStock], [deltaQuantity], [newStock], [minStock], [stockStatus],
        [referenceType], [referenceId], [reason]
    )
    SELECT
        m.itemType,
        m.itemId,
        m.itemName,
        m.unit,
        m.direction,
        m.movementType,
        m.previousStock,
        m.deltaQuantity,
        m.newStock,
        m.minStock,
        CASE
            WHEN m.newStock <= 0 THEN N'OUT_OF_STOCK'
            WHEN m.newStock <= m.minStock THEN N'LOW_STOCK'
            ELSE N'OK'
        END,
        m.referenceType,
        m.referenceId,
        m.reason
    FROM MovimientosPorItem m;

    -- 1. Resolver alerta activa ÚNICAMENTE cuando el stock supera estrictamente el mínimo (newStock > minStock)
    ;WITH UltimoMovimiento AS (
        SELECT
            UPPER(i.[itemType]) AS itemType,
            COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) AS itemId,
            CAST(i.[newStock] AS DECIMAL(12,2)) AS newStock,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(CAST(ing.[minStock] AS DECIMAL(12,2)), 0.00)
                WHEN UPPER(i.[itemType]) = N'EXTRA' THEN COALESCE(CAST(ext.[minStock] AS DECIMAL(12,2)), 5.00)
                WHEN UPPER(i.[itemType]) = N'SIZE' THEN COALESCE(CAST(sz.[minStock] AS DECIMAL(12,2)), 10.00)
                ELSE 0.00
            END AS minStock,
            ROW_NUMBER() OVER (
                PARTITION BY UPPER(i.[itemType]), COALESCE(i.[ingredientId], i.[extraId], i.[sizeId])
                ORDER BY i.[id] DESC
            ) AS rn
        FROM inserted i
        LEFT JOIN [dbo].[ingredients] ing ON UPPER(i.[itemType]) = N'INGREDIENT' AND i.[ingredientId] = ing.[id]
        LEFT JOIN [dbo].[extras] ext ON UPPER(i.[itemType]) = N'EXTRA' AND i.[extraId] = ext.[id]
        LEFT JOIN [dbo].[sizes] sz ON UPPER(i.[itemType]) = N'SIZE' AND i.[sizeId] = sz.[id]
        WHERE COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) IS NOT NULL
    )
    UPDATE sa
    SET sa.[isResolved] = 1,
        sa.[resolvedAt] = SYSDATETIME(),
        sa.[resolvedByStock] = um.newStock,
        sa.[emailError] = CASE
            WHEN sa.[emailSent] = 0 THEN N'Stock repuesto sobre el mínimo antes del envío del lote'
            ELSE sa.[emailError]
        END
    FROM [dbo].[stock_alerts] sa
    INNER JOIN UltimoMovimiento um ON sa.[itemType] = um.itemType AND sa.[itemId] = um.itemId AND um.rn = 1
    WHERE sa.[isResolved] = 0
      AND um.newStock > um.minStock
      AND um.newStock > 0;

    -- 2. Actualizar métricas de la alerta activa existente (SIN reiniciar emailSent = 1 ni duplicar correo)
    ;WITH UltimoMovimientoBajoMinimo AS (
        SELECT
            UPPER(i.[itemType]) AS itemType,
            COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) AS itemId,
            i.[movementType],
            CAST(i.[newStock] AS DECIMAL(12,2)) - CAST(i.[previousStock] AS DECIMAL(12,2)) AS deltaQuantity,
            CAST(i.[newStock] AS DECIMAL(12,2)) AS newStock,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(CAST(ing.[minStock] AS DECIMAL(12,2)), 0.00)
                WHEN UPPER(i.[itemType]) = N'EXTRA' THEN COALESCE(CAST(ext.[minStock] AS DECIMAL(12,2)), 5.00)
                WHEN UPPER(i.[itemType]) = N'SIZE' THEN COALESCE(CAST(sz.[minStock] AS DECIMAL(12,2)), 10.00)
                ELSE 0.00
            END AS minStock,
            i.[referenceType],
            i.[referenceId],
            i.[reason],
            ROW_NUMBER() OVER (
                PARTITION BY UPPER(i.[itemType]), COALESCE(i.[ingredientId], i.[extraId], i.[sizeId])
                ORDER BY i.[id] DESC
            ) AS rn
        FROM inserted i
        LEFT JOIN [dbo].[ingredients] ing ON UPPER(i.[itemType]) = N'INGREDIENT' AND i.[ingredientId] = ing.[id]
        LEFT JOIN [dbo].[extras] ext ON UPPER(i.[itemType]) = N'EXTRA' AND i.[extraId] = ext.[id]
        LEFT JOIN [dbo].[sizes] sz ON UPPER(i.[itemType]) = N'SIZE' AND i.[sizeId] = sz.[id]
        WHERE COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) IS NOT NULL
    )
    UPDATE sa
    SET sa.[movementType] = COALESCE(sf.[movementType], sa.[movementType]),
        sa.[newStock] = sf.newStock,
        sa.[minStock] = sf.minStock,
        sa.[movementQuantity] = sf.deltaQuantity,
        sa.[deficitAmount] = CASE WHEN sf.minStock > sf.newStock THEN sf.minStock - sf.newStock ELSE 0.00 END,
        sa.[alertLevel] = CASE WHEN sf.newStock <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END,
        sa.[referenceType] = COALESCE(sf.[referenceType], sa.[referenceType]),
        sa.[referenceId] = COALESCE(sf.[referenceId], sa.[referenceId]),
        sa.[reason] = COALESCE(sf.[reason], sa.[reason])
    FROM [dbo].[stock_alerts] sa
    INNER JOIN UltimoMovimientoBajoMinimo sf ON sa.[itemType] = sf.itemType AND sa.[itemId] = sf.itemId AND sf.rn = 1
    WHERE sa.[isResolved] = 0
      AND (sf.newStock <= 0 OR sf.newStock <= sf.minStock);

    -- 3. Insertar nueva alerta SOLO si el ítem NO tiene ya un episodio activo (isResolved = 0)
    ;WITH SalidasConFalta AS (
        SELECT
            UPPER(i.[itemType]) AS itemType,
            COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) AS itemId,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(ing.[name], CONCAT(N'Ingrediente #', i.[ingredientId]))
                WHEN UPPER(i.[itemType]) = N'EXTRA' THEN COALESCE(ext.[name], CONCAT(N'Extra #', i.[extraId]))
                WHEN UPPER(i.[itemType]) = N'SIZE' THEN CONCAT(N'Masa ', COALESCE(sz.[name], CONCAT(N'#', i.[sizeId])))
                ELSE N'Ítem Inventario'
            END AS itemName,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(ing.[unit], N'g')
                ELSE N'und'
            END AS unit,
            i.[movementType],
            CAST(i.[previousStock] AS DECIMAL(12,2)) AS previousStock,
            CAST(i.[newStock] AS DECIMAL(12,2)) - CAST(i.[previousStock] AS DECIMAL(12,2)) AS deltaQuantity,
            CAST(i.[newStock] AS DECIMAL(12,2)) AS newStock,
            CASE
                WHEN UPPER(i.[itemType]) = N'INGREDIENT' THEN COALESCE(CAST(ing.[minStock] AS DECIMAL(12,2)), 0.00)
                WHEN UPPER(i.[itemType]) = N'EXTRA' THEN COALESCE(CAST(ext.[minStock] AS DECIMAL(12,2)), 5.00)
                WHEN UPPER(i.[itemType]) = N'SIZE' THEN COALESCE(CAST(sz.[minStock] AS DECIMAL(12,2)), 10.00)
                ELSE 0.00
            END AS minStock,
            i.[referenceType],
            i.[referenceId],
            i.[reason],
            ROW_NUMBER() OVER (
                PARTITION BY UPPER(i.[itemType]), COALESCE(i.[ingredientId], i.[extraId], i.[sizeId])
                ORDER BY i.[id] DESC
            ) AS rn
        FROM inserted i
        LEFT JOIN [dbo].[ingredients] ing ON UPPER(i.[itemType]) = N'INGREDIENT' AND i.[ingredientId] = ing.[id]
        LEFT JOIN [dbo].[extras] ext ON UPPER(i.[itemType]) = N'EXTRA' AND i.[extraId] = ext.[id]
        LEFT JOIN [dbo].[sizes] sz ON UPPER(i.[itemType]) = N'SIZE' AND i.[sizeId] = sz.[id]
        WHERE COALESCE(i.[ingredientId], i.[extraId], i.[sizeId]) IS NOT NULL
          AND CAST(i.[newStock] AS DECIMAL(12,2)) < CAST(i.[previousStock] AS DECIMAL(12,2))
    )
    INSERT INTO [dbo].[stock_alerts] (
        [itemType], [itemId], [itemName], [unit],
        [movementDirection], [movementType],
        [previousStock], [movementQuantity], [newStock], [minStock], [deficitAmount],
        [alertLevel], [referenceType], [referenceId], [reason]
    )
    SELECT
        sf.itemType,
        sf.itemId,
        sf.itemName,
        sf.unit,
        N'SALIDA',
        sf.movementType,
        sf.previousStock,
        sf.deltaQuantity,
        sf.newStock,
        sf.minStock,
        CASE WHEN sf.minStock > sf.newStock THEN sf.minStock - sf.newStock ELSE 0.00 END,
        CASE WHEN sf.newStock <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END,
        sf.referenceType,
        sf.referenceId,
        sf.reason
    FROM SalidasConFalta sf
    WHERE sf.rn = 1
      AND (sf.newStock <= 0 OR sf.newStock <= sf.minStock)
      AND NOT EXISTS (
          SELECT 1 FROM [dbo].[stock_alerts] sa
          WHERE sa.[itemType] = sf.itemType
            AND sa.[itemId] = sf.itemId
            AND sa.[isResolved] = 0
      );
END;
GO

-- ==========================================================================
-- TRIGGER 2: Control directo en tabla [dbo].[ingredients] (ENTRADAS / SALIDAS)
-- ==========================================================================
CREATE OR ALTER TRIGGER [dbo].[trg_ingredients_stock_flow]
ON [dbo].[ingredients]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT UPDATE([stockQuantity]) AND NOT UPDATE([minStock]) AND EXISTS (SELECT 1 FROM deleted)
        RETURN;

    INSERT INTO [dbo].[stock_trigger_events] (
        [itemType], [itemId], [itemName], [unit], [direction], [movementType],
        [previousStock], [deltaQuantity], [newStock], [minStock], [stockStatus], [reason]
    )
    SELECT
        N'INGREDIENT',
        i.[id],
        i.[name],
        ISNULL(i.[unit], N'g'),
        CASE
            WHEN d.[id] IS NULL OR CAST(i.[stockQuantity] AS DECIMAL(12,2)) > CAST(d.[stockQuantity] AS DECIMAL(12,2)) THEN N'ENTRADA'
            WHEN CAST(i.[stockQuantity] AS DECIMAL(12,2)) < CAST(d.[stockQuantity] AS DECIMAL(12,2)) THEN N'SALIDA'
            ELSE N'UMBRAL'
        END,
        N'DIRECT_UPDATE',
        ISNULL(CAST(d.[stockQuantity] AS DECIMAL(12,2)), 0.00),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)) - ISNULL(CAST(d.[stockQuantity] AS DECIMAL(12,2)), 0.00),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00),
        CASE
            WHEN CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= 0 THEN N'OUT_OF_STOCK'
            WHEN CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00) THEN N'LOW_STOCK'
            ELSE N'OK'
        END,
        N'Actualización directa en tabla ingredients'
    FROM inserted i
    LEFT JOIN deleted d ON i.[id] = d.[id]
    WHERE i.[deletedAt] IS NULL
      AND (
          d.[id] IS NULL
          OR CAST(i.[stockQuantity] AS DECIMAL(12,2)) <> CAST(d.[stockQuantity] AS DECIMAL(12,2))
          OR ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00) <> ISNULL(CAST(d.[minStock] AS DECIMAL(12,2)), 0.00)
      )
      AND NOT EXISTS (
          SELECT 1 FROM [dbo].[stock_trigger_events] ste
          WHERE ste.[itemType] = N'INGREDIENT'
            AND ste.[itemId] = i.[id]
            AND ste.[newStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2))
            AND ste.[createdAt] >= DATEADD(SECOND, -5, SYSDATETIME())
      );

    -- 1. Resolver alerta activa SOLO si el stock supera estrictamente el mínimo (stockQuantity > minStock)
    UPDATE sa
    SET sa.[isResolved] = 1,
        sa.[resolvedAt] = SYSDATETIME(),
        sa.[resolvedByStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        sa.[emailError] = CASE
            WHEN sa.[emailSent] = 0 THEN N'Stock repuesto sobre el mínimo antes del envío del lote'
            ELSE sa.[emailError]
        END
    FROM [dbo].[stock_alerts] sa
    INNER JOIN inserted i ON sa.[itemType] = N'INGREDIENT' AND sa.[itemId] = i.[id]
    WHERE sa.[isResolved] = 0
      AND CAST(i.[stockQuantity] AS DECIMAL(12,2)) > ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00)
      AND CAST(i.[stockQuantity] AS DECIMAL(12,2)) > 0;

    -- 2. Actualizar stock actual en alerta activa (tanto pendiente como ya enviada, sin reenviar correo)
    UPDATE sa
    SET sa.[newStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        sa.[minStock] = ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00),
        sa.[deficitAmount] = CASE
            WHEN ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00) > CAST(i.[stockQuantity] AS DECIMAL(12,2))
                THEN ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00) - CAST(i.[stockQuantity] AS DECIMAL(12,2))
            ELSE 0.00
        END,
        sa.[alertLevel] = CASE WHEN CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END
    FROM [dbo].[stock_alerts] sa
    INNER JOIN inserted i ON sa.[itemType] = N'INGREDIENT' AND sa.[itemId] = i.[id]
    WHERE sa.[isResolved] = 0
      AND i.[deletedAt] IS NULL
      AND (CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= 0 OR CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00));

    -- 3. Insertar nueva alerta SOLO si no existe ya un episodio activo (isResolved = 0)
    INSERT INTO [dbo].[stock_alerts] (
        [itemType], [itemId], [itemName], [unit],
        [movementDirection], [movementType],
        [previousStock], [movementQuantity], [newStock], [minStock], [deficitAmount],
        [alertLevel], [reason]
    )
    SELECT
        N'INGREDIENT',
        i.[id],
        i.[name],
        ISNULL(i.[unit], N'g'),
        CASE
            WHEN d.[id] IS NULL THEN N'ENTRADA'
            WHEN CAST(i.[stockQuantity] AS DECIMAL(12,2)) < CAST(d.[stockQuantity] AS DECIMAL(12,2)) THEN N'SALIDA'
            ELSE N'UMBRAL'
        END,
        N'DIRECT_UPDATE',
        ISNULL(CAST(d.[stockQuantity] AS DECIMAL(12,2)), 0.00),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)) - ISNULL(CAST(d.[stockQuantity] AS DECIMAL(12,2)), 0.00),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00),
        CASE
            WHEN ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00) > CAST(i.[stockQuantity] AS DECIMAL(12,2))
                THEN ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00) - CAST(i.[stockQuantity] AS DECIMAL(12,2))
            ELSE 0.00
        END,
        CASE WHEN CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END,
        N'Falta de stock detectada automáticamente en insumo'
    FROM inserted i
    LEFT JOIN deleted d ON i.[id] = d.[id]
    WHERE i.[deletedAt] IS NULL
      AND (
          d.[id] IS NULL
          OR CAST(i.[stockQuantity] AS DECIMAL(12,2)) < CAST(d.[stockQuantity] AS DECIMAL(12,2))
          OR (ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00) > ISNULL(CAST(d.[minStock] AS DECIMAL(12,2)), 0.00)
              AND CAST(d.[stockQuantity] AS DECIMAL(12,2)) > ISNULL(CAST(d.[minStock] AS DECIMAL(12,2)), 0.00))
      )
      AND (CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= 0 OR CAST(i.[stockQuantity] AS DECIMAL(12,2)) <= ISNULL(CAST(i.[minStock] AS DECIMAL(12,2)), 0.00))
      AND NOT EXISTS (
          SELECT 1 FROM [dbo].[stock_alerts] sa
          WHERE sa.[itemType] = N'INGREDIENT'
            AND sa.[itemId] = i.[id]
            AND sa.[isResolved] = 0
      );
END;
GO

-- ==========================================================================
-- TRIGGER 3: Control directo en tabla [dbo].[extras] (ENTRADAS / SALIDAS)
-- ==========================================================================
CREATE OR ALTER TRIGGER [dbo].[trg_extras_stock_flow]
ON [dbo].[extras]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT UPDATE([stockQuantity]) AND NOT UPDATE([minStock]) AND EXISTS (SELECT 1 FROM deleted)
        RETURN;

    INSERT INTO [dbo].[stock_trigger_events] (
        [itemType], [itemId], [itemName], [unit], [direction], [movementType],
        [previousStock], [deltaQuantity], [newStock], [minStock], [stockStatus], [reason]
    )
    SELECT
        N'EXTRA',
        i.[id],
        i.[name],
        N'und',
        CASE
            WHEN d.[id] IS NULL OR i.[stockQuantity] > d.[stockQuantity] THEN N'ENTRADA'
            WHEN i.[stockQuantity] < d.[stockQuantity] THEN N'SALIDA'
            ELSE N'UMBRAL'
        END,
        N'DIRECT_UPDATE',
        CAST(ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] - ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        CAST(ISNULL(i.[minStock], 5) AS DECIMAL(12,2)),
        CASE
            WHEN i.[stockQuantity] <= 0 THEN N'OUT_OF_STOCK'
            WHEN i.[stockQuantity] <= ISNULL(i.[minStock], 5) THEN N'LOW_STOCK'
            ELSE N'OK'
        END,
        N'Actualización directa en tabla extras'
    FROM inserted i
    LEFT JOIN deleted d ON i.[id] = d.[id]
    WHERE i.[deletedAt] IS NULL
      AND (
          d.[id] IS NULL
          OR i.[stockQuantity] <> d.[stockQuantity]
          OR ISNULL(i.[minStock], 5) <> ISNULL(d.[minStock], 5)
      )
      AND NOT EXISTS (
          SELECT 1 FROM [dbo].[stock_trigger_events] ste
          WHERE ste.[itemType] = N'EXTRA'
            AND ste.[itemId] = i.[id]
            AND ste.[newStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2))
            AND ste.[createdAt] >= DATEADD(SECOND, -5, SYSDATETIME())
      );

    -- 1. Resolver alerta activa SOLO si el stock supera estrictamente el mínimo (stockQuantity > minStock)
    UPDATE sa
    SET sa.[isResolved] = 1,
        sa.[resolvedAt] = SYSDATETIME(),
        sa.[resolvedByStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        sa.[emailError] = CASE
            WHEN sa.[emailSent] = 0 THEN N'Stock repuesto sobre el mínimo antes del envío del lote'
            ELSE sa.[emailError]
        END
    FROM [dbo].[stock_alerts] sa
    INNER JOIN inserted i ON sa.[itemType] = N'EXTRA' AND sa.[itemId] = i.[id]
    WHERE sa.[isResolved] = 0
      AND i.[stockQuantity] > ISNULL(i.[minStock], 5)
      AND i.[stockQuantity] > 0;

    -- 2. Actualizar stock actual en alerta activa (sin reiniciar emailSent = 1)
    UPDATE sa
    SET sa.[newStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        sa.[minStock] = CAST(ISNULL(i.[minStock], 5) AS DECIMAL(12,2)),
        sa.[deficitAmount] = CASE
            WHEN ISNULL(i.[minStock], 5) > i.[stockQuantity] THEN CAST(ISNULL(i.[minStock], 5) - i.[stockQuantity] AS DECIMAL(12,2))
            ELSE 0.00
        END,
        sa.[alertLevel] = CASE WHEN i.[stockQuantity] <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END
    FROM [dbo].[stock_alerts] sa
    INNER JOIN inserted i ON sa.[itemType] = N'EXTRA' AND sa.[itemId] = i.[id]
    WHERE sa.[isResolved] = 0
      AND i.[deletedAt] IS NULL
      AND (i.[stockQuantity] <= 0 OR i.[stockQuantity] <= ISNULL(i.[minStock], 5));

    -- 3. Insertar nueva alerta SOLO si no existe ya un episodio activo (isResolved = 0)
    INSERT INTO [dbo].[stock_alerts] (
        [itemType], [itemId], [itemName], [unit],
        [movementDirection], [movementType],
        [previousStock], [movementQuantity], [newStock], [minStock], [deficitAmount],
        [alertLevel], [reason]
    )
    SELECT
        N'EXTRA',
        i.[id],
        i.[name],
        N'und',
        CASE
            WHEN d.[id] IS NULL THEN N'ENTRADA'
            WHEN i.[stockQuantity] < d.[stockQuantity] THEN N'SALIDA'
            ELSE N'UMBRAL'
        END,
        N'DIRECT_UPDATE',
        CAST(ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] - ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        CAST(ISNULL(i.[minStock], 5) AS DECIMAL(12,2)),
        CASE
            WHEN ISNULL(i.[minStock], 5) > i.[stockQuantity] THEN CAST(ISNULL(i.[minStock], 5) - i.[stockQuantity] AS DECIMAL(12,2))
            ELSE 0.00
        END,
        CASE WHEN i.[stockQuantity] <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END,
        N'Falta de stock detectada automáticamente en producto extra'
    FROM inserted i
    LEFT JOIN deleted d ON i.[id] = d.[id]
    WHERE i.[deletedAt] IS NULL
      AND (
          d.[id] IS NULL
          OR i.[stockQuantity] < d.[stockQuantity]
          OR (ISNULL(i.[minStock], 5) > ISNULL(d.[minStock], 5) AND d.[stockQuantity] > ISNULL(d.[minStock], 5))
      )
      AND (i.[stockQuantity] <= 0 OR i.[stockQuantity] <= ISNULL(i.[minStock], 5))
      AND NOT EXISTS (
          SELECT 1 FROM [dbo].[stock_alerts] sa
          WHERE sa.[itemType] = N'EXTRA'
            AND sa.[itemId] = i.[id]
            AND sa.[isResolved] = 0
      );
END;
GO

-- ==========================================================================
-- TRIGGER 4: Control directo en tabla [dbo].[sizes] (ENTRADAS / SALIDAS)
-- ==========================================================================
CREATE OR ALTER TRIGGER [dbo].[trg_sizes_stock_flow]
ON [dbo].[sizes]
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT UPDATE([stockQuantity]) AND NOT UPDATE([minStock]) AND EXISTS (SELECT 1 FROM deleted)
        RETURN;

    INSERT INTO [dbo].[stock_trigger_events] (
        [itemType], [itemId], [itemName], [unit], [direction], [movementType],
        [previousStock], [deltaQuantity], [newStock], [minStock], [stockStatus], [reason]
    )
    SELECT
        N'SIZE',
        i.[id],
        CONCAT(N'Masa ', i.[name]),
        N'und',
        CASE
            WHEN d.[id] IS NULL OR i.[stockQuantity] > d.[stockQuantity] THEN N'ENTRADA'
            WHEN i.[stockQuantity] < d.[stockQuantity] THEN N'SALIDA'
            ELSE N'UMBRAL'
        END,
        N'DIRECT_UPDATE',
        CAST(ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] - ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        CAST(ISNULL(i.[minStock], 10) AS DECIMAL(12,2)),
        CASE
            WHEN i.[stockQuantity] <= 0 THEN N'OUT_OF_STOCK'
            WHEN i.[stockQuantity] <= ISNULL(i.[minStock], 10) THEN N'LOW_STOCK'
            ELSE N'OK'
        END,
        N'Actualización directa en tabla sizes'
    FROM inserted i
    LEFT JOIN deleted d ON i.[id] = d.[id]
    WHERE i.[deletedAt] IS NULL
      AND (
          d.[id] IS NULL
          OR i.[stockQuantity] <> d.[stockQuantity]
          OR ISNULL(i.[minStock], 10) <> ISNULL(d.[minStock], 10)
      )
      AND NOT EXISTS (
          SELECT 1 FROM [dbo].[stock_trigger_events] ste
          WHERE ste.[itemType] = N'SIZE'
            AND ste.[itemId] = i.[id]
            AND ste.[newStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2))
            AND ste.[createdAt] >= DATEADD(SECOND, -5, SYSDATETIME())
      );

    -- 1. Resolver alerta activa SOLO si el stock supera estrictamente el mínimo (stockQuantity > minStock)
    UPDATE sa
    SET sa.[isResolved] = 1,
        sa.[resolvedAt] = SYSDATETIME(),
        sa.[resolvedByStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        sa.[emailError] = CASE
            WHEN sa.[emailSent] = 0 THEN N'Stock repuesto sobre el mínimo antes del envío del lote'
            ELSE sa.[emailError]
        END
    FROM [dbo].[stock_alerts] sa
    INNER JOIN inserted i ON sa.[itemType] = N'SIZE' AND sa.[itemId] = i.[id]
    WHERE sa.[isResolved] = 0
      AND i.[stockQuantity] > ISNULL(i.[minStock], 10)
      AND i.[stockQuantity] > 0;

    -- 2. Actualizar stock actual en alerta activa (sin reiniciar emailSent = 1)
    UPDATE sa
    SET sa.[newStock] = CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        sa.[minStock] = CAST(ISNULL(i.[minStock], 10) AS DECIMAL(12,2)),
        sa.[deficitAmount] = CASE
            WHEN ISNULL(i.[minStock], 10) > i.[stockQuantity] THEN CAST(ISNULL(i.[minStock], 10) - i.[stockQuantity] AS DECIMAL(12,2))
            ELSE 0.00
        END,
        sa.[alertLevel] = CASE WHEN i.[stockQuantity] <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END
    FROM [dbo].[stock_alerts] sa
    INNER JOIN inserted i ON sa.[itemType] = N'SIZE' AND sa.[itemId] = i.[id]
    WHERE sa.[isResolved] = 0
      AND i.[deletedAt] IS NULL
      AND (i.[stockQuantity] <= 0 OR i.[stockQuantity] <= ISNULL(i.[minStock], 10));

    -- 3. Insertar nueva alerta SOLO si no existe ya un episodio activo (isResolved = 0)
    INSERT INTO [dbo].[stock_alerts] (
        [itemType], [itemId], [itemName], [unit],
        [movementDirection], [movementType],
        [previousStock], [movementQuantity], [newStock], [minStock], [deficitAmount],
        [alertLevel], [reason]
    )
    SELECT
        N'SIZE',
        i.[id],
        CONCAT(N'Masa ', i.[name]),
        N'und',
        CASE
            WHEN d.[id] IS NULL THEN N'ENTRADA'
            WHEN i.[stockQuantity] < d.[stockQuantity] THEN N'SALIDA'
            ELSE N'UMBRAL'
        END,
        N'DIRECT_UPDATE',
        CAST(ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] - ISNULL(d.[stockQuantity], 0) AS DECIMAL(12,2)),
        CAST(i.[stockQuantity] AS DECIMAL(12,2)),
        CAST(ISNULL(i.[minStock], 10) AS DECIMAL(12,2)),
        CASE
            WHEN ISNULL(i.[minStock], 10) > i.[stockQuantity] THEN CAST(ISNULL(i.[minStock], 10) - i.[stockQuantity] AS DECIMAL(12,2))
            ELSE 0.00
        END,
        CASE WHEN i.[stockQuantity] <= 0 THEN N'OUT_OF_STOCK' ELSE N'LOW_STOCK' END,
        N'Falta de stock detectada automáticamente en masa'
    FROM inserted i
    LEFT JOIN deleted d ON i.[id] = d.[id]
    WHERE i.[deletedAt] IS NULL
      AND (
          d.[id] IS NULL
          OR i.[stockQuantity] < d.[stockQuantity]
          OR (ISNULL(i.[minStock], 10) > ISNULL(d.[minStock], 10) AND d.[stockQuantity] > ISNULL(d.[minStock], 10))
      )
      AND (i.[stockQuantity] <= 0 OR i.[stockQuantity] <= ISNULL(i.[minStock], 10))
      AND NOT EXISTS (
          SELECT 1 FROM [dbo].[stock_alerts] sa
          WHERE sa.[itemType] = N'SIZE'
            AND sa.[itemId] = i.[id]
            AND sa.[isResolved] = 0
      );
END;
GO
