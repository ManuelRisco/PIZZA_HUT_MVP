// ===================================
// INTERFACES PARA NUEVAS TABLAS
// ===================================

// Size (Tamaños)
export interface SizeDTO {
  id?: number;
  name: string;
  extraCost: number;
  description?: string;
  displayOrder?: number;
}

// Order (Pedidos)
export interface OrderDTO {
  id?: number;
  userId: number;
  addressId?: number | null;
  deliveryType?: 'PICKUP' | 'DELIVERY';
  status: 'PENDING' | 'CONFIRMED' | 'PREPARING' | 'OUT_FOR_DELIVERY' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';
  paymentMethod?: 'CASH' | 'CARD' | 'WALLET';
  paymentMethodId?: number | null;
  subtotal: number;
  deliveryFee: number;
  total: number;
  notes?: string | null;
  estimatedDelivery?: string | null;
  createdAt?: string;
}

// OrderItem (Items de pedido)
export interface OrderItemDTO {
  id?: number;
  orderId: number;
  pizzaId?: number; // Opcional porque puede ser un extra
  pizzaName?: string; // Nombre de la pizza
  extraId?: number; // ID del extra (bebida, postre, etc.)
  extraName?: string; // Nombre del extra
  itemType?: 'PIZZA' | 'EXTRA'; // Tipo de item
  sizeId?: number;
  sizeName?: string; // Nombre del tamaño
  quantity: number;
  unitPrice: number;
  sizeExtra: number;
  lineTotal: number;
  extras?: string[]; // Ingredientes extras (solo para pizzas)
}

// Pedido completo con toda la información
export interface OrderCompleteDTO {
  // Datos del pedido
  id?: number;
  userId: number;
  userName?: string;
  userEmail?: string;
  userPhone?: string;
  
  // Dirección
  addressId?: number | null;
  addressLine1?: string;
  addressCity?: string;
  addressDistrict?: string;
  addressReference?: string;
  
  // Estado y tipo
  deliveryType?: 'PICKUP' | 'DELIVERY';
  status: 'PENDING' | 'CONFIRMED' | 'PREPARING' | 'OUT_FOR_DELIVERY' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';
  
  // Pago
  paymentMethodId?: number | null;
  paymentMethodName?: string;
  
  // Totales
  subtotal: number;
  deliveryFee: number;
  discount: number;
  total: number;
  
  // Promoción
  promoCode?: string | null;
  
  // Adicionales
  notes?: string | null;
  estimatedDelivery?: string | null;
  createdAt?: string;
  
  // Items del pedido
  items?: OrderItemDTO[];
}

// Payment (Pagos)
export interface PaymentDTO {
  id?: number;
  orderId: number;
  amount: number;
  paymentMethod?: 'CASH' | 'CARD' | 'WALLET';
  paymentMethodId?: number;
  status: 'PENDING' | 'PAID' | 'FAILED' | 'REFUNDED';
  transactionId?: string | null;
  createdAt?: string;
}

// OrderTracking (Seguimiento)
export interface OrderTrackingDTO {
  id?: number;
  orderId: number;
  status: 'PENDING' | 'CONFIRMED' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';
  description?: string;
  createdAt?: string;
}

// Review (Reseñas)
export interface ReviewDTO {
  id?: number;
  userId: number;
  orderId: number;  // Cambiado de pizzaId a orderId
  rating: number;
  comment?: string;
  createdAt?: string;
  updatedAt?: string;
  active?: boolean;  // Campo para desactivar/activar reseñas
}

// Favorite (Favoritos)
export interface FavoriteDTO {
  userId: number;
  pizzaId: number;
}

// Address (Direcciones) - Para clientes
export interface AddressDTO {
  id?: number;
  userId: number;
  line1: string;
  city: string;
  district?: string;
  reference?: string;
  isDefault?: boolean;
}

// Ingredient (Ingredientes)
export interface IngredientDTO {
  id?: number;
  name: string;
  extraCost: number;
  isAvailable?: boolean;
}
