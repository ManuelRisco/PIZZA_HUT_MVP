// ===================================
// INTERFACES PARA ADMINISTRACIÓN, STOCK Y FINANZAS
// ===================================

// Size (Tamaños)
export interface SizeDTO {
  id?: number;
  name: string;
  extraCost: number;
  recipeMultiplier?: number;
  stockQuantity?: number;
  minStock?: number;
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
  unitCost?: number;
  lineCost?: number;
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
  orderId: number;
  rating: number;
  comment?: string;
  createdAt?: string;
  updatedAt?: string;
  active?: boolean;
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

// Ingredient (Ingredientes e Insumos)
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
}

// ===================================
// INVENTARIO Y KARDEX
// ===================================
export interface StockAlertItemDTO {
  itemType: 'INGREDIENT' | 'EXTRA';
  itemId: number;
  name: string;
  unit: string;
  stockQuantity: number;
  minStock: number;
  unitCost: number;
  status: 'LOW_STOCK' | 'OUT_OF_STOCK';
}

export interface InventorySummaryDTO {
  totalInventoryValue: number;
  ingredientsInventoryValue: number;
  extrasInventoryValue: number;
  totalIngredients: number;
  totalExtras: number;
  lowStockCount: number;
  outOfStockCount: number;
  alerts: StockAlertItemDTO[];
}

export interface StockMovementDTO {
  id?: number;
  itemType: 'INGREDIENT' | 'EXTRA';
  ingredientId?: number;
  extraId?: number;
  itemName?: string;
  unit?: string;
  movementType: 'PURCHASE' | 'SALE' | 'LOSS' | 'ADJUSTMENT' | 'RETURN';
  quantity: number;
  previousStock: number;
  newStock: number;
  unitCost: number;
  totalCost: number;
  referenceType?: string;
  referenceId?: number;
  reason?: string;
  userId?: number;
  userName?: string;
  createdAt?: string;
}

export interface StockOperationRequestDTO {
  itemType: 'INGREDIENT' | 'EXTRA';
  itemId: number;
  quantity: number;
  unitCost?: number | null;
  minStock?: number | null;
  reason?: string;
}

// ===================================
// PÉRDIDAS DE PRODUCTOS (MERMAS)
// ===================================
export type LossCategory = 'EXPIRED' | 'DAMAGED' | 'KITCHEN_ERROR' | 'CANCELLED_ORDER' | 'OTHER';
export type LossItemType = 'INGREDIENT' | 'EXTRA' | 'PIZZA';

export interface ProductLossDTO {
  id?: number;
  itemType: LossItemType;
  ingredientId?: number;
  extraId?: number;
  pizzaId?: number;
  sizeId?: number;
  orderId?: number;
  itemName: string;
  unit: string;
  quantity: number;
  unitCost: number;
  totalLossCost: number;
  category: LossCategory;
  reason?: string;
  reportedByUserId?: number;
  reportedByName?: string;
  lossDate?: string;
  createdAt?: string;
}

export interface ProductLossCreateDTO {
  itemType: LossItemType;
  ingredientId?: number | null;
  extraId?: number | null;
  pizzaId?: number | null;
  sizeId?: number | null;
  quantity: number;
  category: LossCategory;
  reason?: string;
  deductStock?: boolean;
  lossDate?: string | null;
}

export interface LossesSummaryDTO {
  totalLossCost: number;
  totalCount?: number;
  totalRecords?: number;
  byCategory: { [key: string]: number };
  byItemType?: { [key: string]: number };
}

// ===================================
// ESTIMACIÓN DE COSTOS PARA PRODUCCIÓN
// ===================================
export interface RecipeLineCostDTO {
  ingredientId: number;
  ingredientName: string;
  unit: string;
  quantityPerUnit: number;
  unitCost: number;
  lineCost: number;
}

export interface PizzaSizeCostDTO {
  pizzaId: number;
  pizzaName: string;
  categoryName?: string;
  imageUrl?: string;
  available?: boolean;
  isAvailable?: boolean;
  sizeId: number;
  sizeName: string;
  recipeMultiplier: number;
  salePrice: number;
  directCost: number;
  unitProfit: number;
  profitMargin: number;
  maxProducibleUnits: number;
  ingredients: RecipeLineCostDTO[];
}

export interface ExtraCostDTO {
  extraId: number;
  name: string;
  category: string;
  imageUrl?: string;
  available?: boolean;
  isAvailable?: boolean;
  salePrice: number;
  directCost: number;
  unitProfit: number;
  profitMargin: number;
  availableStock: number;
}

export interface ProductionCatalogCostDTO {
  pizzaCosts: PizzaSizeCostDTO[];
  extraCosts: ExtraCostDTO[];
}

export interface BatchItemRequestDTO {
  itemType: 'PIZZA' | 'EXTRA';
  pizzaId?: number | null;
  sizeId?: number | null;
  extraId?: number | null;
  extraIngredientIds?: number[];
  quantity: number;
}

export interface ProductionEstimateRequestDTO {
  items: BatchItemRequestDTO[];
}

export interface BatchLineEstimateDTO {
  itemType: string;
  itemName: string;
  quantity: number;
  unitDirectCost: number;
  totalDirectCost: number;
  unitSalePrice: number;
  projectedRevenue: number;
  projectedGrossProfit: number;
  profitMargin: number;
}

export interface MaterialRequirementDTO {
  itemType: 'INGREDIENT' | 'EXTRA';
  itemId: number;
  name: string;
  unit: string;
  unitCost: number;
  requiredQuantity: number;
  availableStock: number;
  missingQuantity: number;
  totalDirectCost: number;
  missingPurchaseCost: number;
  sufficient: boolean;
}

export interface ProductionEstimateResponseDTO {
  totalUnits: number;
  totalProductionCost: number;
  projectedRevenue: number;
  projectedGrossProfit: number;
  projectedMarginPercent: number;
  totalMissingPurchaseCost: number;
  canProduceWithCurrentStock: boolean;
  lines: BatchLineEstimateDTO[];
  materials: MaterialRequirementDTO[];
}

// ===================================
// DASHBOARD DE PÉRDIDAS Y GANANCIAS (P&L)
// ===================================
export interface ProfitLossTimelinePointDTO {
  period: string;
  ordersCount: number;
  revenue: number;
  cogs: number;
  grossProfit: number;
  losses: number;
  netProfit: number;
}

export interface ProductProfitabilityDTO {
  itemType: string;
  itemName: string;
  unitsSold: number;
  revenue: number;
  cogs: number;
  profit: number;
  marginPercent: number;
}

export interface LossCategoryBreakdownDTO {
  category: string;
  label: string;
  count: number;
  totalCost: number;
  percentage: number;
}

export interface ProfitLossDashboardDTO {
  startDate: string;
  endDate: string;
  groupBy: string;
  validOrdersCount: number;
  cancelledOrdersCount: number;
  lossesCount: number;
  grossSalesSubtotal: number;
  deliveryFeesCollected: number;
  totalDiscounts: number;
  netRevenue: number;
  costOfGoodsSold: number;
  grossProfit: number;
  grossMarginPercent: number;
  totalProductLosses: number;
  netProfit: number;
  netMarginPercent: number;
  averageTicket: number;
  timeline: ProfitLossTimelinePointDTO[];
  productRanking: ProductProfitabilityDTO[];
  lossesByCategory: LossCategoryBreakdownDTO[];
}
