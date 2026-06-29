-- =============================================
-- INSERTS PARA EXTRAS Y PROMOCIONES
-- Base de datos: pizzahut_bd
-- =============================================

USE pizzahut_bd;

-- =============================================
-- TABLA: extras
-- =============================================

-- BEBIDAS (12 productos)
INSERT INTO extras (name, description, price, category, isAvailable, displayOrder, createdAt, updatedAt) VALUES
('Coca Cola 500ml', 'Bebida gaseosa refrescante de 500ml', 3.50, 'BEBIDA', true, 1, NOW(), NOW()),
('Inca Kola 500ml', 'La bebida del Perú, 500ml', 3.50, 'BEBIDA', true, 2, NOW(), NOW()),
('Sprite 500ml', 'Bebida gaseosa sabor lima-limón, 500ml', 3.50, 'BEBIDA', true, 3, NOW(), NOW()),
('Fanta 500ml', 'Bebida gaseosa sabor naranja, 500ml', 3.50, 'BEBIDA', true, 4, NOW(), NOW()),
('Agua San Luis 625ml', 'Agua mineral sin gas', 2.50, 'BEBIDA', true, 5, NOW(), NOW()),
('Agua San Luis con Gas 625ml', 'Agua mineral con gas', 2.50, 'BEBIDA', true, 6, NOW(), NOW()),
('Coca Cola 1.5L', 'Bebida gaseosa refrescante de 1.5 litros', 7.00, 'BEBIDA', true, 7, NOW(), NOW()),
('Inca Kola 1.5L', 'La bebida del Perú, 1.5 litros', 7.00, 'BEBIDA', true, 8, NOW(), NOW()),
('Jugo de Naranja Natural', 'Jugo de naranja recién exprimido', 8.00, 'BEBIDA', true, 9, NOW(), NOW()),
('Limonada Frozen', 'Limonada helada con hielo frappe', 6.50, 'BEBIDA', true, 10, NOW(), NOW()),
('Chicha Morada', 'Bebida tradicional peruana', 5.00, 'BEBIDA', true, 11, NOW(), NOW()),
('Té Helado', 'Té helado con limón', 4.50, 'BEBIDA', true, 12, NOW(), NOW());

-- POSTRES (9 productos)
INSERT INTO extras (name, description, price, category, isAvailable, displayOrder, createdAt, updatedAt) VALUES
('Cheesecake de Fresa', 'Delicioso cheesecake con topping de fresas', 12.00, 'POSTRE', true, 1, NOW(), NOW()),
('Brownie con Helado', 'Brownie de chocolate caliente con helado de vainilla', 10.00, 'POSTRE', true, 2, NOW(), NOW()),
('Tiramisu', 'Postre italiano clásico con café y mascarpone', 13.00, 'POSTRE', true, 3, NOW(), NOW()),
('Helado de Chocolate', 'Dos bolas de helado de chocolate', 6.00, 'POSTRE', true, 4, NOW(), NOW()),
('Helado de Vainilla', 'Dos bolas de helado de vainilla', 6.00, 'POSTRE', true, 5, NOW(), NOW()),
('Helado de Fresa', 'Dos bolas de helado de fresa', 6.00, 'POSTRE', true, 6, NOW(), NOW()),
('Pie de Limón', 'Tarta de limón con merengue', 11.00, 'POSTRE', true, 7, NOW(), NOW()),
('Volcán de Chocolate', 'Bizcocho con centro de chocolate fundido', 14.00, 'POSTRE', true, 8, NOW(), NOW()),
('Tres Leches', 'Torta tradicional bañada en tres tipos de leche', 9.00, 'POSTRE', true, 9, NOW(), NOW());

-- ENTRADAS (11 productos)
INSERT INTO extras (name, description, price, category, isAvailable, displayOrder, createdAt, updatedAt) VALUES
('Palitos de Ajo (6 unid)', 'Masa de pizza con ajo y queso parmesano', 8.00, 'ENTRADA', true, 1, NOW(), NOW()),
('Alitas BBQ (8 unid)', 'Alitas de pollo bañadas en salsa BBQ', 18.00, 'ENTRADA', true, 2, NOW(), NOW()),
('Alitas Picantes (8 unid)', 'Alitas de pollo con salsa picante', 18.00, 'ENTRADA', true, 3, NOW(), NOW()),
('Dedos de Queso (6 unid)', 'Bastones de queso mozzarella empanizados', 12.00, 'ENTRADA', true, 4, NOW(), NOW()),
('Nachos con Queso', 'Nachos crujientes con salsa de queso cheddar', 15.00, 'ENTRADA', true, 5, NOW(), NOW()),
('Aros de Cebolla (8 unid)', 'Aros de cebolla empanizados y fritos', 10.00, 'ENTRADA', true, 6, NOW(), NOW()),
('Pan con Ajo', 'Pan francés con mantequilla de ajo', 5.00, 'ENTRADA', true, 7, NOW(), NOW()),
('Ensalada César', 'Lechuga romana, crutones, parmesano y aderezo césar', 14.00, 'ENTRADA', true, 8, NOW(), NOW()),
('Ensalada Caprese', 'Tomate, mozzarella fresca, albahaca y aceite de oliva', 16.00, 'ENTRADA', true, 9, NOW(), NOW()),
('Tequeños (6 unid)', 'Masa frita rellena de queso', 10.00, 'ENTRADA', true, 10, NOW(), NOW()),
('Jalapeños Rellenos (6 unid)', 'Jalapeños rellenos de queso crema', 13.00, 'ENTRADA', true, 11, NOW(), NOW());

-- COMPLEMENTOS (7 productos)
INSERT INTO extras (name, description, price, category, isAvailable, displayOrder, createdAt, updatedAt) VALUES
('Salsa Extra BBQ', 'Porción adicional de salsa BBQ', 2.00, 'COMPLEMENTO', true, 1, NOW(), NOW()),
('Salsa Extra Ranch', 'Porción adicional de salsa ranch', 2.00, 'COMPLEMENTO', true, 2, NOW(), NOW()),
('Salsa Extra Picante', 'Porción adicional de salsa picante', 2.00, 'COMPLEMENTO', true, 3, NOW(), NOW()),
('Salsa Extra Ají', 'Porción adicional de salsa de ají casera', 2.00, 'COMPLEMENTO', true, 4, NOW(), NOW()),
('Queso Extra', 'Porción adicional de queso mozzarella', 4.00, 'COMPLEMENTO', true, 5, NOW(), NOW()),
('Borde Relleno de Queso', 'Upgrade para borde de pizza relleno de queso', 8.00, 'COMPLEMENTO', true, 6, NOW(), NOW()),
('Pepperoni Extra', 'Porción adicional de pepperoni', 5.00, 'COMPLEMENTO', true, 7, NOW(), NOW());


-- =============================================
-- TABLA: promotions
-- =============================================

-- PROMOCIONES TIPO PERCENTAGE (5)
INSERT INTO promotions (code, name, description, discount_type, discount_value, min_purchase, max_discount, is_active, start_date, end_date, usage_limit, usage_count, applicable_to, createdAt, updatedAt) VALUES
('PROMO10', '10% de Descuento', 'Descuento del 10% en tu compra mínima de S/50', 'PERCENTAGE', 10.00, 50.00, 20.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 0, 'ALL', NOW(), NOW()),
('PIZZA15', '15% en Pizzas', 'Descuento del 15% exclusivo en pizzas', 'PERCENTAGE', 15.00, 30.00, 30.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 500, 0, 'PIZZAS', NOW(), NOW()),
('EXTRA20', '20% en Extras', 'Descuento del 20% en todos los extras', 'PERCENTAGE', 20.00, 20.00, 25.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 0, 'EXTRAS', NOW(), NOW()),
('VIP25', '25% VIP', 'Promoción especial del 25% para clientes VIP', 'PERCENTAGE', 25.00, 100.00, 50.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 100, 0, 'ALL', NOW(), NOW()),
('STUDENT5', '5% Estudiantes', 'Descuento especial para estudiantes', 'PERCENTAGE', 5.00, 25.00, 10.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 0, 'ALL', NOW(), NOW());

-- PROMOCIONES TIPO FIXED_AMOUNT (4)
INSERT INTO promotions (code, name, description, discount_type, discount_value, min_purchase, is_active, start_date, end_date, usage_limit, usage_count, applicable_to, createdAt, updatedAt) VALUES
('AHORRA15', 'Ahorra S/15', 'Descuento fijo de S/15 en compras mayores a S/70', 'FIXED_AMOUNT', 15.00, 70.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 0, 'ALL', NOW(), NOW()),
('REGALO10', 'Regalo de S/10', 'S/10 de descuento en tu próxima compra', 'FIXED_AMOUNT', 10.00, 40.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1000, 0, 'ALL', NOW(), NOW()),
('DESC20', 'S/20 de Descuento', 'Descuento fijo de S/20 en compras superiores a S/100', 'FIXED_AMOUNT', 20.00, 100.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 0, 'ALL', NOW(), NOW()),
('PRIMERA5', 'Primera Compra S/5', 'S/5 de descuento en tu primera compra', 'FIXED_AMOUNT', 5.00, 30.00, true, '2026-01-01 00:00:00', '2026-12-31 23:59:59', NULL, 0, 'ALL', NOW(), NOW());
