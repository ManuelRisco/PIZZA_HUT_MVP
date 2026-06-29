import { PizzaDTO } from './pizza.interface';

export interface CartItem {
  /** Unique key composed of pizza id or name plus the selected size and extras */
  id: string;
  pizzaId?: number;
  extraId?: number; // ID del extra (bebida, postre, etc.)
  type: 'pizza' | 'extra'; // Tipo de item
  name: string;
  size: string;
  sizeId?: number; // ID del tamaño de la pizza
  quantity: number;
  unitPrice: number;
  imageUrl?: string;
  notes?: string;
  /** IDs de ingredientes extras seleccionados */
  extras?: string[];
  category?: string; // Categoría del extra (BEBIDA, POSTRE, etc.)
}

export interface CartTotals {
  itemsCount: number;
  subtotal: number;
  deliveryFee: number;
  total: number;
}

export interface AddToCartPayload {
  pizza: PizzaDTO;
  size: string;
  sizeId?: number; // ID del tamaño
  quantity?: number;
  /** IDs de ingredientes extras seleccionados */
  extras?: string[];
}
