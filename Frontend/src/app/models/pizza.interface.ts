// DTOs para categorías - coinciden con el backend
export interface CategoryDTO {
  id?: number;
  name: string;
  description?: string;
  imageUrl?: string;
  displayOrder?: number;
  updatedAt?: string; // Fecha de actualización en formato ISO string
}

// DTOs para pizzas - coinciden con el backend
export interface PizzaDTO {
  id?: number;
  categoryName?: string;
  name: string;
  description?: string;
  imageUrl?: string;
  price: number;
  isAvailable?: boolean;
  isPopular?: boolean;
  ingredients?: string[];
  imageError?: boolean; // Para manejar errores de carga de imagen
}

export interface PizzaCreateDTO {
  categoryId?: number;
  name: string;
  description?: string;
  imageUrl?: string;
  price: number;
  isAvailable?: boolean;
  isPopular?: boolean;
}

// DTOs para ingredientes
export interface IngredientDTO {
  id?: number;
  name: string;
  extraCost?: number;
  isAvailable?: boolean;
}

// Alias para compatibilidad con código existente
export interface Category extends CategoryDTO {}
export interface Pizza extends PizzaDTO {
  category?: CategoryDTO; // Para compatibilidad
}