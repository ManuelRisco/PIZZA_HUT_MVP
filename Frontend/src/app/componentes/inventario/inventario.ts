import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize, forkJoin } from 'rxjs';
import { InventoryService } from '../../services/inventory.service';
import { IngredientService } from '../../services/ingredient.service';
import { ExtraService, Extra } from '../../services/extra.service';
import { ToastService } from '../../services/toast.service';
import { PaginationComponent } from '../pagination/pagination';
import {
  IngredientDTO,
  InventorySummaryDTO,
  StockAlertItemDTO,
  StockMovementDTO,
  StockOperationRequestDTO
} from '../../models/admin.interface';

interface UnifiedStockRow {
  itemType: 'INGREDIENT' | 'EXTRA';
  itemId: number;
  name: string;
  categoryLabel: string;
  unit: string;
  stockQuantity: number;
  minStock: number;
  unitCost: number;
  totalValue: number;
  status: 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK';
}

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './inventario.html',
  styleUrls: ['./inventario.css']
})
export class InventarioComponent implements OnInit {
  activeTab: 'alertas' | 'existencias' | 'kardex' = 'alertas';
  loading = false;
  loadingMovements = false;
  savingOperation = false;
  errorMessage = '';
  hasLoadedOnce = false;

  summary: InventorySummaryDTO = {
    totalInventoryValue: 0,
    ingredientsInventoryValue: 0,
    extrasInventoryValue: 0,
    totalIngredients: 0,
    totalExtras: 0,
    lowStockCount: 0,
    outOfStockCount: 0,
    alerts: []
  };

  ingredientes: IngredientDTO[] = [];
  extras: Extra[] = [];
  unifiedStock: UnifiedStockRow[] = [];

  // Paginación Tab 1: Alertas (cacheada)
  alertsPage = 1;
  alertsPerPage = 10;
  paginatedAlertsList: StockAlertItemDTO[] = [];

  // Filtros y Paginación Tab 2: Existencias (cacheadas)
  stockTypeFilter: 'TODOS' | 'INGREDIENT' | 'EXTRA' = 'TODOS';
  stockStatusFilter: 'TODOS' | 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK' = 'TODOS';
  stockSearch = '';
  stockPage = 1;
  stockPerPage = 10;
  filteredStockList: UnifiedStockRow[] = [];
  paginatedStockList: UnifiedStockRow[] = [];

  // Filtros y Paginación Tab 3: Kardex (cacheados)
  movements: StockMovementDTO[] = [];
  kardexItemType: 'TODOS' | 'INGREDIENT' | 'EXTRA' = 'TODOS';
  kardexMovementType: 'TODOS' | 'PURCHASE' | 'SALE' | 'LOSS' | 'ADJUSTMENT' | 'RETURN' = 'TODOS';
  kardexSearch = '';
  kardexLimit = 150;
  kardexPage = 1;
  kardexPerPage = 10;
  filteredMovementsList: StockMovementDTO[] = [];
  paginatedMovementsList: StockMovementDTO[] = [];

  // Modal Operación Rápida (Reabastecer / Ajuste Físico)
  showOperationModal = false;
  operationMode: 'RESTOCK' | 'ADJUST' = 'RESTOCK';
  operationForm: {
    itemType: 'INGREDIENT' | 'EXTRA';
    itemId: number | null;
    quantity: number;
    unitCost: number | null;
    minStock: number | null;
    reason: string;
  } = {
    itemType: 'INGREDIENT',
    itemId: null,
    quantity: 1,
    unitCost: null,
    minStock: null,
    reason: ''
  };

  constructor(
    private readonly inventoryService: InventoryService,
    private readonly ingredientService: IngredientService,
    private readonly extraService: ExtraService,
    private readonly toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.recargarTodo();
  }

  private resolverMensajeError(err: unknown, fallback: string): string {
    const httpErr = err as { status?: number; error?: { message?: string } };
    if (httpErr?.status === 0) {
      return 'No hay conexión con el servidor (Backend desconectado). Verifique que el servicio esté activo.';
    }
    return httpErr?.error?.message || fallback;
  }

  reintentarCarga(): void {
    this.recargarTodo();
  }

  recargarTodo(): void {
    this.loading = true;
    this.errorMessage = '';

    forkJoin({
      summary: this.inventoryService.obtenerResumen(),
      ingredientes: this.ingredientService.obtenerTodos(),
      extras: this.extraService.listarTodos(),
      movements: this.inventoryService.obtenerMovimientos(
        this.kardexItemType,
        this.kardexMovementType,
        this.kardexLimit
      )
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ summary, ingredientes, extras, movements }) => {
          this.summary = summary || {
            totalInventoryValue: 0,
            ingredientsInventoryValue: 0,
            extrasInventoryValue: 0,
            totalIngredients: 0,
            totalExtras: 0,
            lowStockCount: 0,
            outOfStockCount: 0,
            alerts: []
          };
          if (!this.summary.alerts) {
            this.summary.alerts = [];
          }
          this.ingredientes = ingredientes || [];
          this.extras = extras || [];
          this.movements = movements || [];
          this.hasLoadedOnce = true;

          this.recalcularAlertas();
          this.reconstruirExistenciasUnificadas();
          this.recalcularMovimientos();
        },
        error: (err) => {
          this.errorMessage = this.resolverMensajeError(
            err,
            'Error al cargar el resumen de inventario, existencias y Kardex.'
          );
        }
      });
  }

  cargarCatalogosExistencias(): void {
    forkJoin({
      ingredientes: this.ingredientService.obtenerTodos(),
      extras: this.extraService.listarTodos()
    }).subscribe({
      next: ({ ingredientes, extras }) => {
        this.ingredientes = ingredientes || [];
        this.extras = extras || [];
        this.reconstruirExistenciasUnificadas();
      },
      error: (err) => {
        this.errorMessage = this.resolverMensajeError(
          err,
          'Error al actualizar los catálogos de ingredientes y extras.'
        );
      }
    });
  }

  reconstruirExistenciasUnificadas(): void {
    const rows: UnifiedStockRow[] = [];

    for (const ing of this.ingredientes) {
      if (!ing.id) continue;
      const stock = Number(ing.stockQuantity ?? 0);
      const min = Number(ing.minStock ?? 0);
      const cost = Number(ing.unitCost ?? 0);
      const status: 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK' =
        stock <= 0 ? 'OUT_OF_STOCK' : stock <= min ? 'LOW_STOCK' : 'OK';
      rows.push({
        itemType: 'INGREDIENT',
        itemId: ing.id,
        name: ing.name,
        categoryLabel: ing.allowAsExtra === false ? 'Insumo Base Receta' : 'Ingrediente / Topping',
        unit: ing.unit || 'g',
        stockQuantity: stock,
        minStock: min,
        unitCost: cost,
        totalValue: stock * cost,
        status
      });
    }

    for (const ext of this.extras) {
      if (!ext.id) continue;
      const stock = Number(ext.stockQuantity ?? 0);
      const min = Number(ext.minStock ?? 0);
      const cost = Number(ext.unitCost ?? 0);
      const status: 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK' =
        stock <= 0 ? 'OUT_OF_STOCK' : stock <= min ? 'LOW_STOCK' : 'OK';
      rows.push({
        itemType: 'EXTRA',
        itemId: ext.id,
        name: ext.name,
        categoryLabel: `Extra · ${ext.category}`,
        unit: 'und',
        stockQuantity: stock,
        minStock: min,
        unitCost: cost,
        totalValue: stock * cost,
        status
      });
    }

    this.unifiedStock = rows.sort((a, b) => {
      const rank = (s: string) => (s === 'OUT_OF_STOCK' ? 0 : s === 'LOW_STOCK' ? 1 : 2);
      if (rank(a.status) !== rank(b.status)) {
        return rank(a.status) - rank(b.status);
      }
      return a.name.localeCompare(b.name);
    });

    this.recalcularExistencias();
  }

  cargarMovimientos(): void {
    this.loadingMovements = true;
    this.errorMessage = '';
    this.inventoryService
      .obtenerMovimientos(this.kardexItemType, this.kardexMovementType, this.kardexLimit)
      .pipe(finalize(() => (this.loadingMovements = false)))
      .subscribe({
        next: (movs) => {
          this.movements = movs || [];
          this.kardexPage = 1;
          this.recalcularMovimientos();
        },
        error: (err) => {
          this.errorMessage = this.resolverMensajeError(
            err,
            'No se pudieron cargar los movimientos del Kardex.'
          );
        }
      });
  }

  // ==================== RECÁLCULOS CACHEADOS ====================
  recalcularAlertas(): void {
    const alerts = this.summary?.alerts || [];
    const totalPages = Math.max(1, Math.ceil(alerts.length / this.alertsPerPage));
    if (this.alertsPage > totalPages) {
      this.alertsPage = totalPages;
    }
    const start = (this.alertsPage - 1) * this.alertsPerPage;
    this.paginatedAlertsList = alerts.slice(start, start + this.alertsPerPage);
  }

  onAlertsPageChange(page: number): void {
    this.alertsPage = page;
    this.recalcularAlertas();
  }

  onAlertsPerPageChange(size: number): void {
    this.alertsPerPage = size;
    this.alertsPage = 1;
    this.recalcularAlertas();
  }

  recalcularExistencias(): void {
    const term = this.stockSearch.trim().toLowerCase();
    const filtered = this.unifiedStock.filter((row) => {
      if (this.stockTypeFilter !== 'TODOS' && row.itemType !== this.stockTypeFilter) return false;
      if (this.stockStatusFilter !== 'TODOS' && row.status !== this.stockStatusFilter) return false;
      if (
        term &&
        !row.name.toLowerCase().includes(term) &&
        !row.categoryLabel.toLowerCase().includes(term)
      ) {
        return false;
      }
      return true;
    });

    this.filteredStockList = filtered;

    const totalPages = Math.max(1, Math.ceil(filtered.length / this.stockPerPage));
    if (this.stockPage > totalPages) {
      this.stockPage = totalPages;
    }
    const start = (this.stockPage - 1) * this.stockPerPage;
    this.paginatedStockList = filtered.slice(start, start + this.stockPerPage);
  }

  onStockFilterChange(): void {
    this.stockPage = 1;
    this.recalcularExistencias();
  }

  onStockPageChange(page: number): void {
    this.stockPage = page;
    this.recalcularExistencias();
  }

  onStockPerPageChange(size: number): void {
    this.stockPerPage = size;
    this.stockPage = 1;
    this.recalcularExistencias();
  }

  recalcularMovimientos(): void {
    const term = this.kardexSearch.trim().toLowerCase();
    const filtered = !term
      ? [...this.movements]
      : this.movements.filter(
          (m) =>
            (m.itemName || '').toLowerCase().includes(term) ||
            (m.reason || '').toLowerCase().includes(term) ||
            (m.userName || '').toLowerCase().includes(term)
        );

    this.filteredMovementsList = filtered;

    const totalPages = Math.max(1, Math.ceil(filtered.length / this.kardexPerPage));
    if (this.kardexPage > totalPages) {
      this.kardexPage = totalPages;
    }
    const start = (this.kardexPage - 1) * this.kardexPerPage;
    this.paginatedMovementsList = filtered.slice(start, start + this.kardexPerPage);
  }

  onKardexSearchChange(): void {
    this.kardexPage = 1;
    this.recalcularMovimientos();
  }

  onKardexPageChange(page: number): void {
    this.kardexPage = page;
    this.recalcularMovimientos();
  }

  onKardexPerPageChange(size: number): void {
    this.kardexPerPage = size;
    this.kardexPage = 1;
    this.recalcularMovimientos();
  }

  // ==================== TRACKBY ====================
  trackByAlert(index: number, alert: StockAlertItemDTO): string {
    return `${alert.itemType}-${alert.itemId}-${index}`;
  }

  trackByStockRow(_index: number, row: UnifiedStockRow): string {
    return `${row.itemType}-${row.itemId}`;
  }

  trackByMovement(index: number, mov: StockMovementDTO): string | number {
    return mov.id ?? `${mov.itemType}-${mov.ingredientId || mov.extraId || 0}-${mov.createdAt}-${index}`;
  }

  trackByIngredient(index: number, ing: IngredientDTO): number {
    return ing.id ?? index;
  }

  trackByExtra(index: number, ext: Extra): number {
    return ext.id ?? index;
  }

  // ==================== MODAL DE REABASTECIMIENTO / AJUSTE ====================
  abrirModalOperacion(
    mode: 'RESTOCK' | 'ADJUST',
    preset?: {
      itemType: 'INGREDIENT' | 'EXTRA';
      itemId: number;
      currentStock?: number;
      unitCost?: number;
      minStock?: number;
    }
  ): void {
    this.operationMode = mode;
    if (preset) {
      this.operationForm = {
        itemType: preset.itemType,
        itemId: preset.itemId,
        quantity:
          mode === 'ADJUST'
            ? Number(preset.currentStock ?? 0)
            : preset.itemType === 'INGREDIENT'
              ? 500
              : 10,
        unitCost: preset.unitCost !== undefined ? Number(preset.unitCost) : null,
        minStock: preset.minStock !== undefined ? Number(preset.minStock) : null,
        reason:
          mode === 'RESTOCK' ? 'Compra / reposición de almacén' : 'Conteo físico de inventario'
      };
    } else {
      const firstIng = this.ingredientes[0];
      this.operationForm = {
        itemType: 'INGREDIENT',
        itemId: firstIng?.id ?? null,
        quantity: mode === 'ADJUST' ? Number(firstIng?.stockQuantity ?? 0) : 500,
        unitCost: firstIng?.unitCost !== undefined ? Number(firstIng.unitCost) : null,
        minStock: firstIng?.minStock !== undefined ? Number(firstIng.minStock) : null,
        reason:
          mode === 'RESTOCK' ? 'Compra / reposición de almacén' : 'Conteo físico de inventario'
      };
    }
    this.showOperationModal = true;
  }

  abrirDesdeAlerta(alert: StockAlertItemDTO, mode: 'RESTOCK' | 'ADJUST'): void {
    this.abrirModalOperacion(mode, {
      itemType: alert.itemType,
      itemId: alert.itemId,
      currentStock: alert.stockQuantity,
      unitCost: alert.unitCost,
      minStock: alert.minStock
    });
  }

  abrirDesdeFilaStock(row: UnifiedStockRow, mode: 'RESTOCK' | 'ADJUST'): void {
    this.abrirModalOperacion(mode, {
      itemType: row.itemType,
      itemId: row.itemId,
      currentStock: row.stockQuantity,
      unitCost: row.unitCost,
      minStock: row.minStock
    });
  }

  onChangeOperationItemType(): void {
    if (this.operationForm.itemType === 'INGREDIENT') {
      const first = this.ingredientes[0];
      this.operationForm.itemId = first?.id ?? null;
    } else {
      const first = this.extras[0];
      this.operationForm.itemId = first?.id ?? null;
    }
    this.onChangeOperationItem();
  }

  onChangeOperationItem(): void {
    const selected = this.selectedCatalogItem;
    if (!selected) return;
    this.operationForm.unitCost = selected.unitCost;
    this.operationForm.minStock = selected.minStock;
    if (this.operationMode === 'ADJUST') {
      this.operationForm.quantity = selected.stockQuantity;
    } else {
      this.operationForm.quantity = this.operationForm.itemType === 'INGREDIENT' ? 500 : 10;
    }
  }

  get selectedCatalogItem(): {
    name: string;
    unit: string;
    stockQuantity: number;
    minStock: number;
    unitCost: number;
  } | null {
    const id = Number(this.operationForm.itemId);
    if (!id) return null;
    if (this.operationForm.itemType === 'INGREDIENT') {
      const ing = this.ingredientes.find((i) => i.id === id);
      if (!ing) return null;
      return {
        name: ing.name,
        unit: ing.unit || 'g',
        stockQuantity: Number(ing.stockQuantity ?? 0),
        minStock: Number(ing.minStock ?? 0),
        unitCost: Number(ing.unitCost ?? 0)
      };
    } else {
      const ext = this.extras.find((e) => e.id === id);
      if (!ext) return null;
      return {
        name: ext.name,
        unit: 'und',
        stockQuantity: Number(ext.stockQuantity ?? 0),
        minStock: Number(ext.minStock ?? 0),
        unitCost: Number(ext.unitCost ?? 0)
      };
    }
  }

  get projectedStockAfterOperation(): number {
    const current = this.selectedCatalogItem?.stockQuantity ?? 0;
    const qty = Number(this.operationForm.quantity ?? 0);
    if (this.operationMode === 'RESTOCK') {
      return current + Math.max(0, qty);
    }
    return Math.max(0, qty);
  }

  cerrarModalOperacion(): void {
    if (this.savingOperation) return;
    this.showOperationModal = false;
  }

  guardarOperacion(): void {
    if (this.savingOperation) return;
    if (!this.operationForm.itemId) {
      this.toastService.showWarning('Selecciona un insumo o extra.');
      return;
    }
    const qty = Number(this.operationForm.quantity);
    if (this.operationMode === 'RESTOCK' && (Number.isNaN(qty) || qty <= 0)) {
      this.toastService.showWarning('La cantidad a reabastecer debe ser mayor a 0.');
      return;
    }
    if (this.operationMode === 'ADJUST' && (Number.isNaN(qty) || qty < 0)) {
      this.toastService.showWarning('El conteo físico no puede ser negativo.');
      return;
    }

    const payload: StockOperationRequestDTO = {
      itemType: this.operationForm.itemType,
      itemId: Number(this.operationForm.itemId),
      quantity: this.operationForm.itemType === 'EXTRA' ? Math.round(qty) : qty,
      unitCost: this.operationForm.unitCost !== null ? Number(this.operationForm.unitCost) : null,
      minStock: this.operationForm.minStock !== null ? Number(this.operationForm.minStock) : null,
      reason: this.operationForm.reason?.trim() || undefined
    };

    this.savingOperation = true;
    const obs =
      this.operationMode === 'RESTOCK'
        ? this.inventoryService.reabastecer(payload)
        : this.inventoryService.ajustarStock(payload);

    obs.pipe(finalize(() => (this.savingOperation = false))).subscribe({
      next: () => {
        this.showOperationModal = false;
        this.toastService.showSuccess(
          this.operationMode === 'RESTOCK'
            ? 'Stock reabastecido y registrado en Kardex'
            : 'Ajuste físico aplicado y registrado en Kardex'
        );
        this.recargarTodo();
      },
      error: (err) => {
        this.toastService.showError(
          this.resolverMensajeError(err, 'No se pudo aplicar la operación de inventario.')
        );
      }
    });
  }

  getMovementLabel(type: string): string {
    switch (type) {
      case 'PURCHASE':
        return 'Compra / Entrada';
      case 'SALE':
        return 'Venta Pedido';
      case 'LOSS':
        return 'Merma / Pérdida';
      case 'ADJUSTMENT':
        return 'Ajuste Físico';
      case 'RETURN':
        return 'Devolución';
      default:
        return type;
    }
  }

  getMovementBadgeClass(type: string): string {
    switch (type) {
      case 'PURCHASE':
        return 'bg-emerald-100 text-emerald-700';
      case 'SALE':
        return 'bg-blue-100 text-blue-700';
      case 'LOSS':
        return 'bg-red-100 text-red-700';
      case 'ADJUSTMENT':
        return 'bg-purple-100 text-purple-700';
      case 'RETURN':
        return 'bg-amber-100 text-amber-800';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }
}
