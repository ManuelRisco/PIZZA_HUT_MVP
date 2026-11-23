export interface CategoryDTO {
  id?: number;
  name: string;
  description?: string;
  displayOrder?: number;
  updatedAt?: string;
  deletedAt?: string | null;
}

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
  imageError?: boolean;
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

export interface Category extends CategoryDTO {}
export interface Pizza extends PizzaDTO {
  category?: CategoryDTO;
}
