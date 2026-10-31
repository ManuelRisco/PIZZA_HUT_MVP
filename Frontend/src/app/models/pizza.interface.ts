// DTOs para categorías - coinciden con el backend
export interface CategoryDTO {
  id?: number;
  name: string;
  description?: string;
  displayOrder?: number;
  updatedAt?: string; // Fecha de actualización en formato ISO string
  deletedAt?: string | null;
}

// Ingrediente dentro de la receta de una pizza
export interface PizzaIngredientDTO {
  pizzaId?: number;
  ingredientId: number;
  ingredientName?: string;
  unit?: string;
  unitCost?: number;
  stockQuantity?: number;
  isDefault?: boolean;
  quantity: number;
  lineCost?: number;
}

// DTOs para pizzas - coinciden con el backend
export interface PizzaDTO {
  id?: number;
  categoryId?: number;
  categoryName?: string;
  name: string;
  description?: string;
  imageUrl?: string;
  price: number;
  isAvailable?: boolean;
  isPopular?: boolean;
  ingredients?: string[];
  recipe?: PizzaIngredientDTO[];
  availableStock?: number;
  productionCost?: number;
  profitMargin?: number;
  recipeCount?: number;
  lowStock?: boolean;
  outOfStock?: boolean;
  imageError?: boolean; // Para manejar errores de carga de imagen
  deletedAt?: string | null;
}

export interface PizzaCreateDTO {
  categoryId?: number;
  name: string;
  description?: string;
  imageUrl?: string;
  price: number;
  isAvailable?: boolean;
  isPopular?: boolean;
  recipe?: PizzaIngredientDTO[];
}

// DTOs para ingredientes
export interface IngredientDTO {
  id?: number;
  name: string;
  extraCost: number;
  isAvailable?: boolean;
  unit?: string;
  stockQuantity?: number;
  minStock?: number;
  unitCost?: number;
  extraPortionQuantity?: number;
  allowAsExtra?: boolean;
  lowStock?: boolean;
  outOfStock?: boolean;
  deletedAt?: string | null;
}

// Alias para compatibilidad con código existente
export interface Category extends CategoryDTO {}
export interface Pizza extends PizzaDTO {
  category?: CategoryDTO; // Para compatibilidad
}
