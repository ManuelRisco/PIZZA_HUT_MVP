-- ===========================================
-- CREACIÓN DE BASE DE DATOS PIZZAHUT - VERSIÓN MEJORADA
-- ===========================================
CREATE DATABASE IF NOT EXISTS pizzahut_bd;
USE pizzahut_bd;

-- ===========================================
-- TABLAS BASE
-- ===========================================
-- USERS (Mejorada con tokens JWT)
CREATE TABLE users (
    id INT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    name VARCHAR(100),
    phone VARCHAR(20),
    -- Campos para JWT
    refresh_token VARCHAR(512),
    refresh_token_expiry DATETIME(6),
    token_version INT NOT NULL DEFAULT 1,
    last_login DATETIME(6),
    login_attempts INT NOT NULL DEFAULT 0,
    locked_until DATETIME(6),
    -- Campos de auditoría
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_email (email),
    INDEX idx_refresh_token (refresh_token(255))
);
select * from users;

-- CATEGORIES
CREATE TABLE categories (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    imageUrl VARCHAR(500),	
    displayOrder INT NOT NULL DEFAULT 0,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deletedAt DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_display_order (displayOrder)
);

-- SIZES
CREATE TABLE sizes (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    extraCost DECIMAL(10,2) NOT NULL DEFAULT 0,
    description VARCHAR(255),
    displayOrder INT NOT NULL DEFAULT 0,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deletedAt DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_display_order (displayOrder)
);

-- INGREDIENTS
CREATE TABLE ingredients (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    extraCost DECIMAL(10,2) NOT NULL DEFAULT 0,
    isAvailable BOOLEAN NOT NULL DEFAULT TRUE,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deletedAt DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_available (isAvailable)
);

-- PAYMENT METHODS
CREATE TABLE payment_methods (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    isActive BOOLEAN NOT NULL DEFAULT TRUE,
    displayOrder INT NOT NULL UNIQUE DEFAULT 0,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_active (isActive)
);

-- EXTRAS (Nueva tabla para productos adicionales)
CREATE TABLE extras (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    category ENUM('BEBIDA', 'POSTRE', 'ENTRADA', 'COMPLEMENTO') NOT NULL,
    isAvailable BOOLEAN NOT NULL DEFAULT TRUE,
    displayOrder INT NOT NULL DEFAULT 0,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deletedAt DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_category (category),
    INDEX idx_available (isAvailable)
);
-- ===========================================
-- TABLAS DEPENDIENTES
-- ===========================================

-- ADDRESSES
CREATE TABLE addresses (
    id INT NOT NULL AUTO_INCREMENT,
    userId INT NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100),
    reference VARCHAR(255),
    isDefault BOOLEAN NOT NULL DEFAULT FALSE,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deletedAt DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_user (userId),
    FOREIGN KEY (userId) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

select * from addresses;

-- PIZZAS
CREATE TABLE pizzas (
    id INT NOT NULL AUTO_INCREMENT,
    categoryId INT,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    imageUrl VARCHAR(500),
    price DECIMAL(10,2) NOT NULL,
    isAvailable BOOLEAN NOT NULL DEFAULT TRUE,
    isPopular BOOLEAN NOT NULL DEFAULT FALSE,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deletedAt DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_popular (isPopular),
    INDEX idx_available (isAvailable),
    INDEX idx_category (categoryId),
    FOREIGN KEY (categoryId) REFERENCES categories(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- PIZZA_INGREDIENTS
CREATE TABLE pizza_ingredients (
    pizzaId INT NOT NULL,
    ingredientId INT NOT NULL,
    isDefault BOOLEAN NOT NULL DEFAULT TRUE,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (pizzaId, ingredientId),
    FOREIGN KEY (pizzaId) REFERENCES pizzas(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (ingredientId) REFERENCES ingredients(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- PROMOCIONES (Nueva tabla)
CREATE TABLE promotions (
    id INT NOT NULL AUTO_INCREMENT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    discount_type ENUM('PERCENTAGE', 'FIXED_AMOUNT', 'BUNDLE') NOT NULL,
    discount_value DECIMAL(10,2),
    final_price DECIMAL(10,2),
    min_purchase DECIMAL(10,2),
    max_discount DECIMAL(10,2),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    start_date DATETIME(6) NOT NULL,
    end_date DATETIME(6) NOT NULL,
    usage_limit INT,
    usage_count INT NOT NULL DEFAULT 0,
    applicable_to ENUM('ALL', 'PIZZAS', 'EXTRAS', 'SPECIFIC') NOT NULL DEFAULT 'ALL',
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deletedAt DATETIME(6),
    PRIMARY KEY (id),
    INDEX idx_code (code),
    INDEX idx_active (is_active),
    INDEX idx_dates (start_date, end_date)
);
select * from promotions;

-- PROMOTION_PIZZAS (Pizzas incluidas en promoción)
CREATE TABLE promotion_pizzas (
    promotionId INT NOT NULL,
    pizzaId INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (promotionId, pizzaId),
    FOREIGN KEY (promotionId) REFERENCES promotions(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (pizzaId) REFERENCES pizzas(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- PROMOTION_EXTRAS (Extras incluidos en promoción)
CREATE TABLE promotion_extras (
    promotionId INT NOT NULL,
    extraId INT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (promotionId, extraId),
    FOREIGN KEY (promotionId) REFERENCES promotions(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (extraId) REFERENCES extras(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
-- ORDERS
CREATE TABLE orders (
    id INT NOT NULL AUTO_INCREMENT,
    userId INT NOT NULL,
    addressId INT,
    paymentMethodId INT,
    promotionId INT,
    status ENUM('PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    delivery_type ENUM('PICKUP','DELIVERY') NOT NULL DEFAULT 'DELIVERY',
    subtotal DECIMAL(10,2) NOT NULL,
    discount DECIMAL(10,2) NOT NULL DEFAULT 0.00, -- Kept this single definition
    deliveryFee DECIMAL(10,2) NOT NULL DEFAULT 0,
    total DECIMAL(10,2) NOT NULL,
    notes TEXT,
    promo_code VARCHAR(50),
    estimatedDelivery DATETIME(6),
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_user (userId),
    INDEX idx_created (createdAt),
    FOREIGN KEY (userId) REFERENCES users(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    FOREIGN KEY (addressId) REFERENCES addresses(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    FOREIGN KEY (paymentMethodId) REFERENCES payment_methods(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    FOREIGN KEY (promotionId) REFERENCES promotions(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- ITEMS DE PEDIDO
CREATE TABLE order_items (
    id INT NOT NULL AUTO_INCREMENT,
    orderId INT NOT NULL,
    pizzaId INT,
    extraId INT,
    sizeId INT,
    item_type ENUM('PIZZA', 'EXTRA') NOT NULL DEFAULT 'PIZZA',
    quantity INT NOT NULL DEFAULT 1,
    unitPrice DECIMAL(10,2) NOT NULL,
    sizeExtra DECIMAL(10,2) NOT NULL DEFAULT 0,
    lineTotal DECIMAL(10,2) NOT NULL,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_order (orderId),
    FOREIGN KEY (orderId) REFERENCES orders(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (pizzaId) REFERENCES pizzas(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    FOREIGN KEY (extraId) REFERENCES extras(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    FOREIGN KEY (sizeId) REFERENCES sizes(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- INGREDIENTES EXTRAS POR ITEM DE PEDIDO
CREATE TABLE order_item_extras (
    id INT NOT NULL AUTO_INCREMENT,
    orderItemId INT NOT NULL,
    ingredientId INT NOT NULL,
    ingredientName VARCHAR(100) NOT NULL,
    extraCost DECIMAL(10,2) NOT NULL DEFAULT 0,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_order_item (orderItemId),
    FOREIGN KEY (orderItemId) REFERENCES order_items(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (ingredientId) REFERENCES ingredients(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- PAYMENTS
CREATE TABLE payments (
    id INT NOT NULL AUTO_INCREMENT,
    orderId INT NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    paymentMethodId INT,
    status ENUM('PENDING', 'PAID', 'FAILED', 'REFUNDED') NOT NULL DEFAULT 'PENDING',
    transactionId VARCHAR(255),
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_status (status),
    INDEX idx_transaction (transactionId),
    FOREIGN KEY (orderId) REFERENCES orders(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (paymentMethodId) REFERENCES payment_methods(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- ORDER TRACKING (Actualizada con los nuevos estados)
CREATE TABLE order_tracking (
    id INT NOT NULL AUTO_INCREMENT,
    orderId INT NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED') NOT NULL,
    description VARCHAR(255),
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_order (orderId),
    FOREIGN KEY (orderId) REFERENCES orders(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- REVIEWS
CREATE TABLE reviews (
    id INT AUTO_INCREMENT PRIMARY KEY,
    userId INT NOT NULL,
    orderId INT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updatedAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_user (userId),
    INDEX idx_order (orderId),
    INDEX idx_rating (rating),
    CONSTRAINT fk_review_user FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_review_order FOREIGN KEY (orderId) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_order UNIQUE (userId, orderId)
);

-- FAVORITES
CREATE TABLE favorites (
    userId INT NOT NULL,
    pizzaId INT NOT NULL,
    createdAt DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (userId, pizzaId),
    FOREIGN KEY (userId) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    FOREIGN KEY (pizzaId) REFERENCES pizzas(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ===========================================
-- TABLA DE AUDITORÍA Y LOGS
-- ===========================================

-- AUDIT_LOGS (Nueva tabla para auditoría completa)
CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT,
    action_type ENUM('LOGIN', 'LOGOUT', 'CREATE', 'UPDATE', 'DELETE', 'VIEW', 'EXPORT', 'FAILED_LOGIN') NOT NULL,
    entity_type VARCHAR(50),
    entity_id INT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_method VARCHAR(10),
    request_url VARCHAR(500),
    old_values JSON,
    new_values JSON,
    description TEXT,
    status ENUM('SUCCESS', 'FAILED', 'WARNING') NOT NULL DEFAULT 'SUCCESS',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_user (user_id),
    INDEX idx_action (action_type),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_created (created_at),
    INDEX idx_status (status),
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

-- SESSION_LOGS (Nueva tabla específica para sesiones)
CREATE TABLE session_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,
    session_token VARCHAR(512),
    login_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    logout_time DATETIME(6),
    ip_address VARCHAR(45),
    user_agent TEXT,
    device_type VARCHAR(50),
    browser VARCHAR(50),
    location VARCHAR(100),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    logout_reason ENUM('MANUAL', 'TIMEOUT', 'TOKEN_EXPIRED', 'FORCED', 'SECURITY') DEFAULT 'MANUAL',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX idx_user (user_id),
    INDEX idx_active (is_active),
    INDEX idx_login_time (login_time),
    FOREIGN KEY (user_id) REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
-- ===========================================
-- TRIGGERS PARA AUDITORÍA AUTOMÁTICA
-- ===========================================

-- Trigger para registrar cambios en usuarios
DELIMITER //
CREATE TRIGGER trg_users_audit_update
AFTER UPDATE ON users
FOR EACH ROW
BEGIN
    IF OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL THEN
        INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description, old_values, new_values)
        VALUES (NEW.id, 'DELETE', 'USER', NEW.id, 'Usuario desactivado', 
                JSON_OBJECT('status', 'active'),
                JSON_OBJECT('status', 'deleted'));
    END IF;
END//

-- Trigger para registrar cambios en pedidos
CREATE TRIGGER trg_orders_audit_update
AFTER UPDATE ON orders
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description, old_values, new_values)
        VALUES (NEW.userId, 'UPDATE', 'ORDER', NEW.id, 
                CONCAT('Estado del pedido cambiado de ', OLD.status, ' a ', NEW.status),
                JSON_OBJECT('status', OLD.status),
                JSON_OBJECT('status', NEW.status));
                
        INSERT INTO order_tracking (orderId, status, description)
        VALUES (NEW.id, NEW.status, CONCAT('Estado cambiado a ', NEW.status));
    END IF;
END//

-- Trigger para registrar creación de pedidos
CREATE TRIGGER trg_orders_audit_insert
AFTER INSERT ON orders
FOR EACH ROW
BEGIN
    INSERT INTO audit_logs (user_id, action_type, entity_type, entity_id, description, new_values)
    VALUES (NEW.userId, 'CREATE', 'ORDER', NEW.id, 
            'Nuevo pedido creado',
            JSON_OBJECT('total', NEW.total, 'status', NEW.status));
            
    INSERT INTO order_tracking (orderId, status, description)
    VALUES (NEW.id, NEW.status, 'Pedido creado');
END//

DELIMITER ;

-- ===========================================
-- ÍNDICES ADICIONALES PARA OPTIMIZACIÓN
-- ===========================================

-- Índices compuestos para búsquedas frecuentes
CREATE INDEX idx_orders_user_status ON orders(userId, status);
CREATE INDEX idx_orders_created_status ON orders(createdAt, status);
CREATE INDEX idx_pizzas_category_available ON pizzas(categoryId, isAvailable);
CREATE INDEX idx_promotions_active_dates ON promotions(is_active, start_date, end_date);