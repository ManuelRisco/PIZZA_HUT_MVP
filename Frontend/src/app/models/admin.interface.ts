
export interface SizeDTO {
  id?: number;
  name: string;
  extraCost: number;
  recipeMultiplier?: number;
  stockQuantity?: number;
  minStock?: number;
  description?: string;
  displayOrder?: number;
  deletedAt?: string | null;
}

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
  emailSent?: boolean;
  emailSentTo?: string | null;
  adminEmailSent?: boolean;
  wantsBoleta?: boolean;
  boletaTipoComprobante?: number | null;
  boletaSerie?: string | null;
  boletaNumero?: number | null;
  boletaComprobante?: string | null;
  boletaClienteDni?: string | null;
  boletaClienteNombre?: string | null;
  boletaClienteDireccion?: string | null;
  boletaPdfUrl?: string | null;
  boletaXmlUrl?: string | null;
  boletaCdrUrl?: string | null;
  boletaEnlace?: string | null;
  boletaHash?: string | null;
  boletaQr?: string | null;
  boletaEstado?: string | null;
  boletaError?: string | null;
  boletaTotalGravada?: number | null;
  boletaTotalIgv?: number | null;
  boletaEmitidaAt?: string | null;
  ncSerie?: string | null;
  ncNumero?: number | null;
  ncComprobante?: string | null;
  ncTipoNota?: number | null;
  ncMotivo?: string | null;
  ncPdfUrl?: string | null;
  ncXmlUrl?: string | null;
  ncCdrUrl?: string | null;
  ncEnlace?: string | null;
  ncHash?: string | null;
  ncQr?: string | null;
  ncEstado?: string | null;
  ncError?: string | null;
  ncEmitidaAt?: string | null;
}

export interface OrderItemDTO {
  id?: number;
  orderId: number;
  pizzaId?: number;
  pizzaName?: string;
  extraId?: number;
  extraName?: string;
  itemType?: 'PIZZA' | 'EXTRA';
  sizeId?: number;
  sizeName?: string;
  quantity: number;
  unitPrice: number;
  sizeExtra: number;
  lineTotal: number;
  unitCost?: number;
  lineCost?: number;
  extras?: string[];
}

export interface OrderCompleteDTO {
  id?: number;
  userId: number;
  userName?: string;
  userEmail?: string;
  userPhone?: string;
  userDni?: string | null;
  userRuc?: string | null;

  addressId?: number | null;
  addressLine1?: string;
  addressCity?: string;
  addressDistrict?: string;
  addressReference?: string;

  deliveryType?: 'PICKUP' | 'DELIVERY';
  status: 'PENDING' | 'CONFIRMED' | 'PREPARING' | 'OUT_FOR_DELIVERY' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';

  paymentMethodId?: number | null;
  paymentMethodName?: string;

  subtotal: number;
  deliveryFee: number;
  discount: number;
  total: number;

  promoCode?: string | null;

  notes?: string | null;
  estimatedDelivery?: string | null;
  createdAt?: string;

  wantsBoleta?: boolean;
  boletaTipoComprobante?: number | null;
  boletaSerie?: string | null;
  boletaNumero?: number | null;
  boletaComprobante?: string | null;
  boletaClienteDni?: string | null;
  boletaClienteNombre?: string | null;
  boletaClienteDireccion?: string | null;
  boletaPdfUrl?: string | null;
  boletaXmlUrl?: string | null;
  boletaCdrUrl?: string | null;
  boletaEnlace?: string | null;
  boletaHash?: string | null;
  boletaQr?: string | null;
  boletaEstado?: string | null;
  boletaError?: string | null;
  boletaTotalGravada?: number | null;
  boletaTotalIgv?: number | null;
  boletaEmitidaAt?: string | null;

  ncSerie?: string | null;
  ncNumero?: number | null;
  ncComprobante?: string | null;
  ncTipoNota?: number | null;
  ncMotivo?: string | null;
  ncPdfUrl?: string | null;
  ncXmlUrl?: string | null;
  ncCdrUrl?: string | null;
  ncEnlace?: string | null;
  ncHash?: string | null;
  ncQr?: string | null;
  ncEstado?: string | null;
  ncError?: string | null;
  ncEmitidaAt?: string | null;

  items?: OrderItemDTO[];
}

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

export interface OrderTrackingDTO {
  id?: number;
  orderId: number;
  status: 'PENDING' | 'CONFIRMED' | 'IN_DELIVERY' | 'DELIVERED' | 'CANCELLED';
  description?: string;
  createdAt?: string;
}

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

export interface FavoriteDTO {
  userId: number;
  pizzaId: number;
}

export interface AddressDTO {
  id?: number;
  userId: number;
  line1: string;
  city: string;
  district?: string;
  reference?: string;
  isDefault?: boolean;
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

export interface TriggerStockAlertDTO {
  id: number;
  itemType: 'INGREDIENT' | 'EXTRA' | 'SIZE';
  itemId: number;
  itemName: string;
  unit: string;
  newStock: number;
  minStock: number;
  deficitAmount?: number;
  alertLevel: 'LOW_STOCK' | 'OUT_OF_STOCK';
  resolved: boolean;
  emailSent: boolean;
  emailRecipients?: string | null;
  emailError?: string | null;
  createdAt?: string | null;
  sentAt?: string | null;
}

export interface StockAlertBatchStatusDTO {
  mailOperational: boolean;
  batchWindowSeconds: number;
  pendingBatchCount: number;
  secondsRemaining: number;
  notifiedActiveCount: number;
  lastSentAt?: string | null;
  lastRecipients?: string | null;
  activeAlerts: TriggerStockAlertDTO[];
}

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

export interface OrderLossCandidateItemDTO {
  orderItemId: number;
  itemType: 'PIZZA' | 'EXTRA';
  pizzaId?: number | null;
  extraId?: number | null;
  sizeId?: number | null;
  itemName: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  unitCost: number;
  lineCost: number;
  extras?: string[];
}

export interface OrderLossCandidateDTO {
  orderId: number;
  status: string;
  deliveryType?: string;
  stockSettled?: string;
  customerName: string;
  customerEmail?: string;
  total: number;
  notes?: string | null;
  createdAt?: string;
  items: OrderLossCandidateItemDTO[];
}

export interface OrderLossItemSelectionDTO {
  orderItemId: number;
  quantity: number;
}

export interface OrderLossCreateDTO {
  orderId: number;
  category?: LossCategory;
  reason?: string;
  lossDate?: string | null;
  restoreUnselectedStock?: boolean;
  deductStockIfPending?: boolean;
  items?: OrderLossItemSelectionDTO[];
}

export interface LossesSummaryDTO {
  totalLossCost: number;
  totalCount?: number;
  totalRecords?: number;
  byCategory: { [key: string]: number };
  byItemType?: { [key: string]: number };
}

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
