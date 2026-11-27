import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { ProductLossService } from '../../services/product-loss.service';
import { IngredientService } from '../../services/ingredient.service';
import { ExtraService, Extra } from '../../services/extra.service';
import { PizzaService } from '../../services/pizza.service';
import { Size } from '../../services/size.service';
import { ToastService } from '../../services/toast.service';
import { PaginationComponent } from '../pagination/pagination';
import { PizzaDTO } from '../../models/pizza.interface';
import {
  IngredientDTO,
  LossCategory,
  LossItemType,
  OrderLossCandidateDTO,
  OrderLossCreateDTO,
  ProductLossCreateDTO,
  ProductLossDTO,
  SizeDTO
} from '../../models/admin.interface';

export type LossFormMode = LossItemType | 'ORDER';

export interface PulledOrderItemRow {
  orderItemId: number;
  itemType: 'PIZZA' | 'EXTRA';
  itemName: string;
  extras: string[];
  maxQuantity: number;
  quantity: number;
  unitCost: number;
  unitPrice: number;
  selected: boolean;
}

export interface CategoryStatCard {
  code: LossCategory;
  label: string;
  icon: string;
  badgeClass: string;
  barClass: string;
  totalCost: number;
  count: number;
  percentage: number;
}

@Component({
  selector: 'app-perdidas',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './perdidas.html',
  styleUrls: ['./perdidas.css']
})
export class PerdidasComponent implements OnInit {
  loading = false;
  saving = false;
  deleting = false;
  errorMessage = '';

  losses: ProductLossDTO[] = [];
  ingredientes: IngredientDTO[] = [];
  extras: Extra[] = [];
  pizzas: PizzaDTO[] = [];
  sizes: SizeDTO[] = [];
  pullableOrders: OrderLossCandidateDTO[] = [];
  loadingOrders = false;
  orderSearchTerm = '';
  selectedOrderId: number | null = null;
  pulledOrderItems: PulledOrderItemRow[] = [];
  restoreUnselectedStock = true;
  deductStockIfPending = true;
  private autoOrderReason = '';

  filterStartDate = '';
  filterEndDate = '';
  filterCategory: 'TODOS' | LossCategory = 'TODOS';
  filterItemType: 'TODOS' | LossItemType = 'TODOS';
  searchTerm = '';

  currentPage = 1;
  itemsPerPage = 10;
  filteredLosses: ProductLossDTO[] = [];
  paginatedLosses: ProductLossDTO[] = [];
  totalLossCostFiltered = 0;
  categoryStats: CategoryStatCard[] = [];
  topLossCause: CategoryStatCard | null = null;

  showModal = false;
  editingLoss: ProductLossDTO | null = null;
  form: {
    itemType: LossFormMode;
    ingredientId: number | null;
    extraId: number | null;
    pizzaId: number | null;
    sizeId: number | null;
    quantity: number;
    category: LossCategory;
    reason: string;
    deductStock: boolean;
    lossDate: string;
  } = this.createDefaultForm();

  lossToDelete: ProductLossDTO | null = null;
  restoreStockOnDelete = true;

  readonly categoryMeta: Record<
    LossCategory,
    { label: string; icon: string; badgeClass: string; barClass: string }
  > = {
    EXPIRED: {
      label: 'Vencimiento / Caducidad',
      icon: 'bi-calendar-x-fill',
      badgeClass: 'bg-amber-100 text-amber-800',
      barClass: 'bg-amber-500'
    },
    DAMAGED: {
      label: 'Daño / Deterioro',
      icon: 'bi-bandaid-fill',
      badgeClass: 'bg-orange-100 text-orange-800',
      barClass: 'bg-orange-500'
    },
    KITCHEN_ERROR: {
      label: 'Error en Cocina / Horno',
      icon: 'bi-fire',
      badgeClass: 'bg-red-100 text-red-700',
      barClass: 'bg-red-600'
    },
    CANCELLED_ORDER: {
      label: 'Pedido Cancelado en Preparación',
      icon: 'bi-x-octagon-fill',
      badgeClass: 'bg-purple-100 text-purple-800',
      barClass: 'bg-purple-600'
    },
    OTHER: {
      label: 'Otras Mermas',
      icon: 'bi-question-diamond-fill',
      badgeClass: 'bg-gray-200 text-gray-800',
      barClass: 'bg-gray-600'
    }
  };

  readonly categoryCodes: LossCategory[] = [
    'EXPIRED',
    'DAMAGED',
    'KITCHEN_ERROR',
    'CANCELLED_ORDER',
    'OTHER'
  ];

  constructor(
    private readonly productLossService: ProductLossService,
    private readonly ingredientService: IngredientService,
    private readonly extraService: ExtraService,
    private readonly pizzaService: PizzaService,
    private readonly sizeService: Size,
    private readonly toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarCatalogos();
    this.cargarPerdidas();
  }

  private createDefaultForm() {
    const now = new Date();
    const localIso = new Date(now.getTime() - now.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);
    return {
      itemType: 'INGREDIENT' as LossFormMode,
      ingredientId: null as number | null,
      extraId: null as number | null,
      pizzaId: null as number | null,
      sizeId: null as number | null,
      quantity: 100,
      category: 'EXPIRED' as LossCategory,
      reason: '',
      deductStock: true,
      lossDate: localIso
    };
  }

  cargarCatalogos(): void {
    this.ingredientService.obtenerTodos().subscribe({
      next: (res) => (this.ingredientes = res || [])
    });
    this.extraService.listarTodos().subscribe({
      next: (res) => (this.extras = res || [])
    });
    this.pizzaService.listarPizzas().subscribe({
      next: (res) => (this.pizzas = res || [])
    });
    this.sizeService.obtenerTodos().subscribe({
      next: (res) => (this.sizes = res || [])
    });
    this.cargarPedidosJalables();
  }

  cargarPedidosJalables(autoSelectIfOrderMode = false): void {
    this.loadingOrders = true;
    this.productLossService
      .listarPedidosJalables()
      .pipe(finalize(() => { this.loadingOrders = false; }))
      .subscribe({
        next: (res) => {
          this.pullableOrders = res || [];
          if (this.selectedOrderId && !this.pullableOrders.some((o) => o.orderId === this.selectedOrderId)) {
            this.selectedOrderId = null;
            this.pulledOrderItems = [];
          }
          if (autoSelectIfOrderMode && this.form.itemType === 'ORDER' && !this.selectedOrderId && this.pullableOrders.length > 0) {
            this.onSelectOrder(this.pullableOrders[0].orderId);
          }
        },
        error: () => {
          this.pullableOrders = [];
        }
      });
  }

  cargarPerdidas(): void {
    this.loading = true;
    this.errorMessage = '';
    const bothDatesProvided = Boolean(this.filterStartDate && this.filterEndDate);

    this.productLossService
      .listarPerdidas({
        startDate: bothDatesProvided ? this.filterStartDate : undefined,
        endDate: bothDatesProvided ? this.filterEndDate : undefined,
        category: this.filterCategory,
        itemType: this.filterItemType
      })
      .pipe(finalize(() => { this.loading = false; }))
      .subscribe({
        next: (data) => {
          this.losses = data || [];
          this.currentPage = 1;
          this.recalcularVista();
        },
        error: (err) => {
          console.error('Error al cargar pérdidas:', err);
          this.errorMessage = err?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica que el servicio esté encendido.'
            : (err?.error?.message || 'Error al cargar el registro de pérdidas.');
          this.losses = [];
          this.recalcularVista();
        }
      });
  }

  recalcularVista(): void {
    const term = this.searchTerm.trim().toLowerCase();
    const filtered = this.losses.filter((l) => {
      if (this.filterStartDate && !this.filterEndDate && l.lossDate) {
        if (l.lossDate.slice(0, 10) < this.filterStartDate) return false;
      }
      if (!this.filterStartDate && this.filterEndDate && l.lossDate) {
        if (l.lossDate.slice(0, 10) > this.filterEndDate) return false;
      }
      if (
        term &&
        !(l.itemName || '').toLowerCase().includes(term) &&
        !(l.reason || '').toLowerCase().includes(term) &&
        !(l.reportedByName || '').toLowerCase().includes(term) &&
        !String(l.orderId || '').includes(term) &&
        !(`#ep-${l.orderId || ''}`).includes(term) &&
        !(`#${l.orderId || ''}`).includes(term)
      ) {
        return false;
      }
      return true;
    });

    this.filteredLosses = filtered;

    const total = filtered.reduce((sum, l) => sum + Number(l.totalLossCost ?? 0), 0);
    this.totalLossCostFiltered = total;

    this.categoryStats = this.categoryCodes.map((code) => {
      const meta = this.categoryMeta[code];
      const matching = filtered.filter((l) => l.category === code);
      const catCost = matching.reduce((s, l) => s + Number(l.totalLossCost ?? 0), 0);
      return {
        code,
        label: meta.label,
        icon: meta.icon,
        badgeClass: meta.badgeClass,
        barClass: meta.barClass,
        totalCost: catCost,
        count: matching.length,
        percentage: total > 0 ? (catCost / total) * 100 : 0
      };
    });

    const sorted = [...this.categoryStats].sort((a, b) => b.totalCost - a.totalCost);
    this.topLossCause = sorted.length > 0 && sorted[0].totalCost > 0 ? sorted[0] : null;

    const totalPages = Math.ceil(filtered.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }

    const start = (this.currentPage - 1) * this.itemsPerPage;
    this.paginatedLosses = filtered.slice(start, start + this.itemsPerPage);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.recalcularVista();
  }

  onItemsPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.recalcularVista();
  }

  limpiarFiltros(): void {
    this.filterStartDate = '';
    this.filterEndDate = '';
    this.filterCategory = 'TODOS';
    this.filterItemType = 'TODOS';
    this.searchTerm = '';
    this.cargarPerdidas();
  }

  filtrarPorCategoriaRapida(cat: LossCategory): void {
    this.filterCategory = this.filterCategory === cat ? 'TODOS' : cat;
    this.cargarPerdidas();
  }

  getCategoryLabel(category: string): string {
    const meta = this.categoryMeta[category as LossCategory];
    return meta ? meta.label : category;
  }

  getCategoryBadgeClass(category: string): string {
    const meta = this.categoryMeta[category as LossCategory];
    return meta ? meta.badgeClass : 'bg-gray-100 text-gray-700';
  }

  abrirModalNueva(): void {
    this.editingLoss = null;
    this.form = this.createDefaultForm();
    this.form.ingredientId = this.ingredientes[0]?.id ?? null;
    this.form.extraId = this.extras[0]?.id ?? null;
    this.form.pizzaId = this.pizzas[0]?.id ?? null;
    this.form.sizeId = this.sizes[0]?.id ?? null;
    this.orderSearchTerm = '';
    this.selectedOrderId = null;
    this.pulledOrderItems = [];
    this.restoreUnselectedStock = true;
    this.deductStockIfPending = true;
    this.autoOrderReason = '';
    this.cargarPedidosJalables();
    this.showModal = true;
  }

  abrirModalEditar(loss: ProductLossDTO): void {
    this.editingLoss = loss;
    let formattedDate = '';
    if (loss.lossDate) {
      formattedDate = loss.lossDate.slice(0, 16);
    }
    this.form = {
      itemType: loss.itemType,
      ingredientId: loss.ingredientId ?? null,
      extraId: loss.extraId ?? null,
      pizzaId: loss.pizzaId ?? null,
      sizeId: loss.sizeId ?? null,
      quantity: Number(loss.quantity ?? 1),
      category: loss.category,
      reason: loss.reason || '',
      deductStock: false,
      lossDate: formattedDate
    };
    this.showModal = true;
  }

  cerrarModal(): void {
    this.showModal = false;
    this.editingLoss = null;
    this.selectedOrderId = null;
    this.pulledOrderItems = [];
    this.orderSearchTerm = '';
    this.autoOrderReason = '';
  }

  onChangeFormItemType(): void {
    if (this.form.itemType === 'INGREDIENT') {
      this.form.ingredientId = this.ingredientes[0]?.id ?? null;
      this.form.quantity = 100;
    } else if (this.form.itemType === 'EXTRA') {
      this.form.extraId = this.extras[0]?.id ?? null;
      this.form.quantity = 1;
    } else if (this.form.itemType === 'PIZZA') {
      this.form.pizzaId = this.pizzas[0]?.id ?? null;
      this.form.sizeId = this.sizes[0]?.id ?? null;
      this.form.quantity = 1;
    } else if (this.form.itemType === 'ORDER') {
      this.form.category = 'CANCELLED_ORDER';
      this.orderSearchTerm = '';
      this.restoreUnselectedStock = true;
      this.deductStockIfPending = true;
      if (this.pullableOrders.length > 0) {
        const targetId = this.selectedOrderId && this.pullableOrders.some((o) => o.orderId === this.selectedOrderId)
          ? this.selectedOrderId
          : this.pullableOrders[0].orderId;
        this.onSelectOrder(targetId);
      } else {
        this.selectedOrderId = null;
        this.pulledOrderItems = [];
      }
      this.cargarPedidosJalables(true);
    }
  }

  onSelectOrder(orderId: number | null): void {
    const numericId = orderId !== null && orderId !== undefined ? Number(orderId) : null;
    this.selectedOrderId = numericId;
    const order = this.pullableOrders.find((o) => o.orderId === numericId) || null;
    if (!order) {
      this.pulledOrderItems = [];
      return;
    }
    this.pulledOrderItems = (order.items || []).map((item) => ({
      orderItemId: item.orderItemId,
      itemType: item.itemType,
      itemName: item.itemName,
      extras: item.extras || [],
      maxQuantity: Number(item.quantity ?? 1),
      quantity: Number(item.quantity ?? 1),
      unitCost: Number(item.unitCost ?? 0),
      unitPrice: Number(item.unitPrice ?? 0),
      selected: true
    }));
    const suggestedReason = `Anulación de pedido #EP-${String(order.orderId).padStart(4, '0')} (${order.customerName || 'Cliente'})`;
    if (!this.form.reason.trim() || this.form.reason.trim() === this.autoOrderReason) {
      this.form.reason = suggestedReason;
    }
    this.autoOrderReason = suggestedReason;
  }

  get filteredPullableOrders(): OrderLossCandidateDTO[] {
    const term = this.orderSearchTerm.trim().toLowerCase();
    if (!term) return this.pullableOrders;
    return this.pullableOrders.filter((o) => {
      const code = `#ep-${String(o.orderId).padStart(4, '0')}`;
      const statusLabel = this.getOrderStatusLabel(o.status).toLowerCase();
      const itemNames = (o.items || []).map((i) => i.itemName.toLowerCase()).join(' ');
      return (
        String(o.orderId).includes(term) ||
        code.includes(term) ||
        (o.customerName || '').toLowerCase().includes(term) ||
        (o.customerEmail || '').toLowerCase().includes(term) ||
        statusLabel.includes(term) ||
        itemNames.includes(term)
      );
    });
  }

  get selectedPullableOrder(): OrderLossCandidateDTO | null {
    if (this.selectedOrderId === null) return null;
    return this.pullableOrders.find((o) => o.orderId === Number(this.selectedOrderId)) || null;
  }

  get selectedPulledItemsCount(): number {
    return this.pulledOrderItems.filter((i) => i.selected && i.quantity > 0).length;
  }

  get allPulledItemsSelected(): boolean {
    return this.pulledOrderItems.length > 0 && this.pulledOrderItems.every((i) => i.selected);
  }

  get hasUnselectedOrderItems(): boolean {
    return this.pulledOrderItems.some((i) => !i.selected || i.quantity < i.maxQuantity);
  }

  get estimatedOrderLossTotal(): number {
    return this.pulledOrderItems
      .filter((i) => i.selected && i.quantity > 0)
      .reduce((sum, i) => sum + i.quantity * i.unitCost, 0);
  }

  toggleAllPulledItems(checked: boolean): void {
    this.pulledOrderItems.forEach((item) => {
      item.selected = checked;
      if (checked && item.quantity < 1) {
        item.quantity = item.maxQuantity;
      }
    });
  }

  clampPulledItemQuantity(item: PulledOrderItemRow): void {
    let q = Math.round(Number(item.quantity));
    if (Number.isNaN(q) || q < 1) q = 1;
    if (q > item.maxQuantity) q = item.maxQuantity;
    item.quantity = q;
  }

  adjustPulledItemQuantity(item: PulledOrderItemRow, delta: number): void {
    item.quantity = item.quantity + delta;
    this.clampPulledItemQuantity(item);
    item.selected = true;
  }

  getOrderStatusLabel(status?: string): string {
    switch ((status || '').toUpperCase()) {
      case 'PENDING': return 'Pendiente';
      case 'CONFIRMED': return 'Confirmado';
      case 'PREPARING': return 'En Preparación';
      case 'OUT_FOR_DELIVERY':
      case 'IN_DELIVERY': return 'En Camino';
      case 'DELIVERED': return 'Entregado';
      case 'CANCELLED': return 'Anulado';
      default: return status || 'Activo';
    }
  }

  getOrderStatusBadgeClass(status?: string): string {
    switch ((status || '').toUpperCase()) {
      case 'PENDING': return 'bg-amber-100 text-amber-800';
      case 'CONFIRMED': return 'bg-blue-100 text-blue-800';
      case 'PREPARING': return 'bg-orange-100 text-orange-800';
      case 'OUT_FOR_DELIVERY':
      case 'IN_DELIVERY': return 'bg-indigo-100 text-indigo-800';
      case 'DELIVERED': return 'bg-emerald-100 text-emerald-800';
      case 'CANCELLED': return 'bg-red-100 text-red-700';
      default: return 'bg-gray-100 text-gray-700';
    }
  }

  get selectedUnitLabel(): string {
    if (this.form.itemType === 'INGREDIENT') {
      const ing = this.ingredientes.find((i) => i.id === Number(this.form.ingredientId));
      return ing?.unit || 'g';
    }
    return 'und';
  }

  get selectedCurrentStockInfo(): string {
    if (this.form.itemType === 'INGREDIENT') {
      const ing = this.ingredientes.find((i) => i.id === Number(this.form.ingredientId));
      if (!ing) return '—';
      return `${Number(ing.stockQuantity ?? 0).toFixed(2)} ${ing.unit || 'g'} disponibles`;
    }
    if (this.form.itemType === 'EXTRA') {
      const ext = this.extras.find((e) => e.id === Number(this.form.extraId));
      if (!ext) return '—';
      return `${Number(ext.stockQuantity ?? 0)} und disponibles`;
    }
    const piz = this.pizzas.find((p) => p.id === Number(this.form.pizzaId));
    const sz = this.sizes.find((s) => s.id === Number(this.form.sizeId));
    if (!piz) return '—';
    const mult = Number(sz?.recipeMultiplier ?? 1);
    const baseAvail = Number(piz.availableStock ?? 0);
    const scaledAvail = mult > 0 ? Math.floor(baseAvail / mult) : baseAvail;
    return `Hasta ~${scaledAvail} pizzas producibles con insumos actuales (x${mult.toFixed(2)})`;
  }

  get estimatedUnitDirectCost(): number {
    if (this.editingLoss) {
      return Number(this.editingLoss.unitCost ?? 0);
    }
    if (this.form.itemType === 'INGREDIENT') {
      const ing = this.ingredientes.find((i) => i.id === Number(this.form.ingredientId));
      return Number(ing?.unitCost ?? 0);
    }
    if (this.form.itemType === 'EXTRA') {
      const ext = this.extras.find((e) => e.id === Number(this.form.extraId));
      return Number(ext?.unitCost ?? 0);
    }
    const piz = this.pizzas.find((p) => p.id === Number(this.form.pizzaId));
    const sz = this.sizes.find((s) => s.id === Number(this.form.sizeId));
    const baseCost = Number(piz?.productionCost ?? 0);
    const mult = Number(sz?.recipeMultiplier ?? 1);
    return baseCost * (mult > 0 ? mult : 1);
  }

  get estimatedTotalLossCost(): number {
    if (this.editingLoss) {
      return Number(this.editingLoss.totalLossCost ?? 0);
    }
    const qty =
      this.form.itemType === 'INGREDIENT'
        ? Math.max(0, Number(this.form.quantity ?? 0))
        : Math.max(0, Math.round(Number(this.form.quantity ?? 0)));
    return qty * this.estimatedUnitDirectCost;
  }

  guardarPerdida(): void {
    if (!this.editingLoss && this.form.itemType === 'ORDER') {
      if (!this.selectedOrderId) {
        this.toastService.showWarning('Selecciona el pedido que deseas jalar y anular.');
        return;
      }
      const selectedItems = this.pulledOrderItems
        .filter((i) => i.selected)
        .map((i) => {
          this.clampPulledItemQuantity(i);
          return { orderItemId: i.orderItemId, quantity: i.quantity };
        });

      if (selectedItems.length === 0) {
        this.toastService.showWarning('Selecciona al menos un producto del pedido para registrar la pérdida.');
        return;
      }

      const orderPayload: OrderLossCreateDTO = {
        orderId: Number(this.selectedOrderId),
        category: this.form.category,
        reason: this.form.reason?.trim() || undefined,
        lossDate: this.form.lossDate ? `${this.form.lossDate}:00` : undefined,
        restoreUnselectedStock: this.restoreUnselectedStock,
        deductStockIfPending: this.deductStockIfPending,
        items: selectedItems
      };

      this.saving = true;
      this.productLossService
        .registrarPerdidaDesdePedido(orderPayload)
        .pipe(finalize(() => { this.saving = false; }))
        .subscribe({
          next: (created) => {
            const count = Array.isArray(created) ? created.length : selectedItems.length;
            const code = String(orderPayload.orderId).padStart(4, '0');
            this.toastService.showSuccess(
              `Pedido #EP-${code} anulado y ${count} producto(s) registrado(s) como pérdida.`
            );
            this.cerrarModal();
            this.cargarPerdidas();
            this.cargarCatalogos();
          },
          error: (err) => {
            const msg = err?.error?.message || 'No se pudo anular el pedido y registrar la pérdida.';
            this.toastService.showError(msg);
          }
        });
      return;
    }

    const qty = Number(this.form.quantity);
    if (!this.editingLoss && (Number.isNaN(qty) || qty <= 0)) {
      this.toastService.showWarning('La cantidad de pérdida debe ser mayor a 0.');
      return;
    }

    if (!this.editingLoss) {
      if (this.form.itemType === 'INGREDIENT' && !this.form.ingredientId) {
        this.toastService.showWarning('Selecciona el ingrediente afectado.');
        return;
      }
      if (this.form.itemType === 'EXTRA' && !this.form.extraId) {
        this.toastService.showWarning('Selecciona el extra afectado.');
        return;
      }
      if (this.form.itemType === 'PIZZA' && !this.form.pizzaId) {
        this.toastService.showWarning('Selecciona la pizza afectada.');
        return;
      }
    }

    const resolvedItemType: LossItemType = this.form.itemType === 'ORDER' ? 'PIZZA' : this.form.itemType;

    const payload: ProductLossCreateDTO = {
      itemType: resolvedItemType,
      ingredientId: resolvedItemType === 'INGREDIENT' ? Number(this.form.ingredientId) : undefined,
      extraId: resolvedItemType === 'EXTRA' ? Number(this.form.extraId) : undefined,
      pizzaId: resolvedItemType === 'PIZZA' ? Number(this.form.pizzaId) : undefined,
      sizeId: resolvedItemType === 'PIZZA' && this.form.sizeId ? Number(this.form.sizeId) : undefined,
      quantity: qty,
      category: this.form.category,
      reason: this.form.reason?.trim() || undefined,
      deductStock: this.form.deductStock,
      lossDate: this.form.lossDate ? `${this.form.lossDate}:00` : undefined
    };

    this.saving = true;

    const req$ = this.editingLoss?.id
      ? this.productLossService.actualizarPerdida(this.editingLoss.id, payload)
      : this.productLossService.registrarPerdida(payload);

    req$.pipe(finalize(() => { this.saving = false; })).subscribe({
      next: () => {
        this.toastService.showSuccess(
          this.editingLoss
            ? 'Registro de pérdida actualizado.'
            : 'Pérdida registrada correctamente en el sistema.'
        );
        this.cerrarModal();
        this.cargarPerdidas();
        this.cargarCatalogos();
      },
      error: (err) => {
        const msg = err?.error?.message || 'No se pudo guardar la pérdida.';
        this.toastService.showError(msg);
      }
    });
  }

  confirmarEliminar(loss: ProductLossDTO): void {
    this.lossToDelete = loss;
    this.restoreStockOnDelete = !loss.orderId;
  }

  cancelarEliminar(): void {
    this.lossToDelete = null;
  }

  ejecutarEliminar(): void {
    if (!this.lossToDelete?.id) return;
    this.deleting = true;
    this.productLossService
      .eliminarPerdida(this.lossToDelete.id, this.restoreStockOnDelete)
      .pipe(finalize(() => { this.deleting = false; }))
      .subscribe({
        next: () => {
          this.toastService.showSuccess('Registro de pérdida eliminado.');
          this.lossToDelete = null;
          this.cargarPerdidas();
          this.cargarCatalogos();
        },
        error: (err) => {
          this.toastService.showError(err?.error?.message || 'No se pudo eliminar el registro.');
        }
      });
  }

  trackByLoss(index: number, loss: ProductLossDTO): number {
    return loss.id ?? index;
  }

  trackByCategoryStat(_index: number, stat: CategoryStatCard): string {
    return stat.code;
  }

  trackByPulledOrderItem(_index: number, item: PulledOrderItemRow): number {
    return item.orderItemId;
  }
}
