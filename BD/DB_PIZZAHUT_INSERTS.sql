USE pizzahut_bd;

-- ===========================================
-- INSERTS COMPLETOS PARA LA BASE DE DATOS PIZZAHUT
-- ===========================================

-- ===========================================
-- CATEGORIES (Categorías de Pizzas)
-- ===========================================
INSERT INTO categories (name, description, imageUrl, displayOrder, createdAt, updatedAt) VALUES
('Clásicas', 'Pizzas tradicionales con los sabores de siempre', 'https://images.unsplash.com/photo-1513104890138-7c749659a591', 1, NOW(6), NOW(6)),
('Especiales', 'Pizzas con ingredientes premium y combinaciones únicas', 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002', 2, NOW(6), NOW(6)),
('Vegetarianas', 'Pizzas sin carne, perfectas para vegetarianos', 'https://images.unsplash.com/photo-1511689660979-10d2b1aada49', 3, NOW(6), NOW(6)),
('Gourmet', 'Pizzas de autor con ingredientes selectos', 'https://images.unsplash.com/photo-1534308983496-4fabb1a015ee', 4, NOW(6), NOW(6)),
('Carnes', 'Pizzas con gran variedad de carnes', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38', 5, NOW(6), NOW(6));

-- ===========================================
-- SIZES (Tamaños de Pizzas)
-- ===========================================
INSERT INTO sizes (name, extraCost, description, displayOrder, createdAt, updatedAt) VALUES
('Personal', 0.00, 'Pizza individual - 20cm (4 porciones)', 1, NOW(6), NOW(6)),
('Mediana', 3.00, 'Pizza mediana - 30cm (6 porciones)', 2, NOW(6), NOW(6)),
('Familiar', 5.00, 'Pizza familiar - 35cm (8 porciones)', 3, NOW(6), NOW(6)),
('Extra Grande', 8.00, 'Pizza extra grande - 40cm (10 porciones)', 4, NOW(6), NOW(6));

-- ===========================================
-- INGREDIENTS (Ingredientes)
-- ===========================================

-- Ingredientes básicos (sin costo extra)
INSERT INTO ingredients (name, extraCost, isAvailable, createdAt, updatedAt) VALUES
('Salsa de Tomate', 0.00, TRUE, NOW(6), NOW(6)),
('Mozzarella', 0.00, TRUE, NOW(6), NOW(6)),
('Orégano', 0.00, TRUE, NOW(6), NOW(6));

-- Ingredientes extras para decoradores (los más usados)
INSERT INTO ingredients (name, extraCost, isAvailable, createdAt, updatedAt) VALUES
('Queso Extra', 2.50, TRUE, NOW(6), NOW(6)),
('Champiñones', 2.00, TRUE, NOW(6), NOW(6)),
('Pepperoni', 3.00, TRUE, NOW(6), NOW(6));

-- Ingredientes adicionales
INSERT INTO ingredients (name, extraCost, isAvailable, createdAt, updatedAt) VALUES
('Jamón', 2.50, TRUE, NOW(6), NOW(6)),
('Aceitunas', 1.50, TRUE, NOW(6), NOW(6)),
('Pimientos', 1.50, TRUE, NOW(6), NOW(6)),
('Cebolla', 1.00, TRUE, NOW(6), NOW(6)),
('Tomate', 1.00, TRUE, NOW(6), NOW(6)),
('Albahaca', 1.00, TRUE, NOW(6), NOW(6)),
('Piña', 2.00, TRUE, NOW(6), NOW(6)),
('Tocino', 2.50, TRUE, NOW(6), NOW(6)),
('Salami', 2.50, TRUE, NOW(6), NOW(6)),
('Chorizo', 2.50, TRUE, NOW(6), NOW(6)),
('Pollo', 3.00, TRUE, NOW(6), NOW(6)),
('Carne Molida', 3.00, TRUE, NOW(6), NOW(6)),
('Jalapeños', 1.50, TRUE, NOW(6), NOW(6)),
('Maíz', 1.00, TRUE, NOW(6), NOW(6)),
('Atún', 2.50, TRUE, NOW(6), NOW(6)),
('Anchoas', 2.50, TRUE, NOW(6), NOW(6)),
('Rúcula', 1.50, TRUE, NOW(6), NOW(6)),
('Queso Azul', 2.50, TRUE, NOW(6), NOW(6)),
('Queso Parmesano', 2.00, TRUE, NOW(6), NOW(6)),
('Queso Cheddar', 2.00, TRUE, NOW(6), NOW(6)),
('Pesto', 1.50, TRUE, NOW(6), NOW(6)),
('Espinaca', 1.50, TRUE, NOW(6), NOW(6)),
('Berenjena', 1.50, TRUE, NOW(6), NOW(6)),
('Zucchini', 1.50, TRUE, NOW(6), NOW(6));

-- ===========================================
-- PIZZAS (Pizzas del menú)
-- ===========================================

-- Pizzas Clásicas (categoryId = 1)
INSERT INTO pizzas (categoryId, name, description, imageUrl, price, isAvailable, isPopular, createdAt, updatedAt) VALUES
(1, 'Margarita', 'La pizza clásica italiana con salsa de tomate, mozzarella y albahaca fresca', 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002', 12.99, TRUE, TRUE, NOW(6), NOW(6)),
(1, 'Pepperoni', 'Pizza tradicional con abundante pepperoni y queso mozzarella', 'https://images.unsplash.com/photo-1628840042765-356cda07504e', 14.99, TRUE, TRUE, NOW(6), NOW(6)),
(1, 'Hawaiana', 'La controversida combinación de jamón y piña sobre queso mozzarella', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38', 13.99, TRUE, FALSE, NOW(6), NOW(6)),
(1, 'Napolitana', 'Salsa de tomate, mozzarella, anchoas, alcaparras y aceitunas', 'https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f', 14.50, TRUE, FALSE, NOW(6), NOW(6));

-- Pizzas Especiales (categoryId = 2)
INSERT INTO pizzas (categoryId, name, description, imageUrl, price, isAvailable, isPopular, createdAt, updatedAt) VALUES
(2, 'Cuatro Quesos', 'Deliciosa mezcla de mozzarella, parmesano, queso azul y cheddar', 'https://images.unsplash.com/photo-1513104890138-7c749659a591', 16.99, TRUE, TRUE, NOW(6), NOW(6)),
(2, 'BBQ Chicken', 'Pollo marinado en salsa BBQ, cebolla morada, tocino y cilantro', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38', 17.99, TRUE, TRUE, NOW(6), NOW(6)),
(2, 'Supreme', 'La pizza con todo: pepperoni, salchicha, champiñones, pimientos y cebolla', 'https://images.unsplash.com/photo-1534308983496-4fabb1a015ee', 18.99, TRUE, FALSE, NOW(6), NOW(6)),
(2, 'Meat Lovers', 'Para los amantes de la carne: pepperoni, salami, jamón, tocino y chorizo', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38', 19.99, TRUE, TRUE, NOW(6), NOW(6));

-- Pizzas Vegetarianas (categoryId = 3)
INSERT INTO pizzas (categoryId, name, description, imageUrl, price, isAvailable, isPopular, createdAt, updatedAt) VALUES
(3, 'Vegetal', 'Champiñones, pimientos, cebolla, tomate, aceitunas y orégano', 'https://images.unsplash.com/photo-1511689660979-10d2b1aada49', 13.99, TRUE, FALSE, NOW(6), NOW(6)),
(3, 'Mediterránea', 'Tomates cherry, aceitunas kalamata, espinaca, queso feta y albahaca', 'https://images.unsplash.com/photo-1593560708920-61dd98c46a4e', 15.99, TRUE, TRUE, NOW(6), NOW(6)),
(3, 'Funghi', 'Variedad de champiñones salteados con ajo, mozzarella y trufa', 'https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f', 16.50, TRUE, FALSE, NOW(6), NOW(6));

-- Pizzas Gourmet (categoryId = 4)
INSERT INTO pizzas (categoryId, name, description, imageUrl, price, isAvailable, isPopular, createdAt, updatedAt) VALUES
(4, 'Prosciutto e Rucola', 'Prosciutto di Parma, rúcula fresca, parmesano y aceite de trufa', 'https://images.unsplash.com/photo-1574071318508-1cdbab80d002', 21.99, TRUE, TRUE, NOW(6), NOW(6)),
(4, 'Burrata', 'Burrata cremosa, tomates cherry confitados, pesto y albahaca', 'https://images.unsplash.com/photo-1595708684082-a173bb3a06c5', 22.99, TRUE, FALSE, NOW(6), NOW(6)),
(4, 'Trufa Negra', 'Crema de trufa, champiñones portobello, parmesano y huevo', 'https://images.unsplash.com/photo-1513104890138-7c749659a591', 24.99, TRUE, FALSE, NOW(6), NOW(6));

-- Pizzas de Carnes (categoryId = 5)
INSERT INTO pizzas (categoryId, name, description, imageUrl, price, isAvailable, isPopular, createdAt, updatedAt) VALUES
(5, 'Carnívora', 'Pepperoni, salami, chorizo, tocino y carne molida', 'https://images.unsplash.com/photo-1565299624946-b28f40a0ae38', 18.99, TRUE, TRUE, NOW(6), NOW(6)),
(5, 'Mexicana', 'Carne molida, jalapeños, cebolla, pimientos y salsa picante', 'https://images.unsplash.com/photo-1571997478779-2adcbbe9ab2f', 17.99, TRUE, FALSE, NOW(6), NOW(6)),
(5, 'Bacon & Cheese', 'Tocino crujiente, tres quesos y cebolla caramelizada', 'https://images.unsplash.com/photo-1534308983496-4fabb1a015ee', 17.50, TRUE, FALSE, NOW(6), NOW(6));


-- Métodos de pago por defecto
INSERT INTO payment_methods (name, description, isActive, displayOrder, updatedAt) VALUES
('Efectivo', 'Pago en efectivo al recibir el pedido', TRUE, 1, NOW(6)),
('Tarjeta', 'Pago con tarjeta de crédito o débito', TRUE, 2, NOW(6)),
('Yape', 'Pago mediante aplicación Yape', TRUE, 3, NOW(6)),
('Plin', 'Pago mediante aplicación Plin', TRUE, 4, NOW(6));

select * from payment_methods;

