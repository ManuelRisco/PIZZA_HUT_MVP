import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { catchError, finalize, forkJoin, of } from 'rxjs';
import { InventoryService } from '../../services/inventory.service';
import { IngredientService } from '../../services/ingredient.service';
import { ExtraService, Extra } from '../../services/extra.service';
import { ToastService } from '../../services/toast.service';
import { PaginationComponent } from '../pagination/pagination';
import {
  IngredientDTO,
  InventorySummaryDTO,
  StockAlertBatchStatusDTO,
  StockAlertItemDTO,
  StockMovementDTO,
  StockOperationRequestDTO,
  TriggerStockAlertDTO
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
export class InventarioComponent implements OnInit, OnDestroy {
  private static readonly ACK_SENT_AT_KEY = 'pizzahut_ack_stock_alert_sent_at';

  activeTab: 'alertas' | 'existencias' | 'kardex' = 'alertas';
  loading = false;
  loadingMovements = false;
  savingOperation = false;
  sendingBatchNow = false;
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

  batchStatus: StockAlertBatchStatusDTO = this.crearBatchStatusPorDefecto();
  activeAlertMailMap: Map<string, TriggerStockAlertDTO> = new Map();
  showSentBanner = false;
  lastDispatchedBatchCount = 0;

  private batchCountdownInterval: ReturnType<typeof setInterval> | null = null;
  private sentBannerTimeout: ReturnType<typeof setTimeout> | null = null;
  private zeroCountdownPollTicks = 0;

  ingredientes: IngredientDTO[] = [];
  extras: Extra[] = [];
  unifiedStock: UnifiedStockRow[] = [];

  // Paginacion Tab 1: Alertas (cacheada)
  alertsPage = 1;
  alertsPerPage = 10;
  paginatedAlertsList: StockAlertItemDTO[] = [];

  // Filtros y Paginacion Tab 2: Existencias (cacheadas)
  stockTypeFilter: 'TODOS' | 'INGREDIENT' | 'EXTRA' = 'TODOS';
  stockStatusFilter: 'TODOS' | 'OK' | 'LOW_STOCK' | 'OUT_OF_STOCK' = 'TODOS';
  stockSearch = '';
  stockPage = 1;
  stockPerPage = 10;
  filteredStockList: UnifiedStockRow[] = [];
  paginatedStockList: UnifiedStockRow[] = [];

  // Filtros y Paginacion Tab 3: Kardex (cacheados)
  movements: StockMovementDTO[] = [];
  kardexItemType: 'TODOS' | 'INGREDIENT' | 'EXTRA' = 'TODOS';
  kardexMovementType: 'TODOS' | 'PURCHASE' | 'SALE' | 'LOSS' | 'ADJUSTMENT' | 'RETURN' = 'TODOS';
  kardexSearch = '';
  kardexLimit = 150;
  kardexPage = 1;
  kardexPerPage = 10;
  filteredMovementsList: StockMovementDTO[] = [];
  paginatedMovementsList: StockMovementDTO[] = [];

  // Modal Operacion Rapida (Reabastecer / Ajuste Fisico)
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

  ngOnDestroy(): void {
    this.clearBatchTimers();
    this.clearSentBannerTimer();
  }

  private crearBatchStatusPorDefecto(): StockAlertBatchStatusDTO {
    return {
      mailOperational: false,
      batchWindowSeconds: 60,
      pendingBatchCount: 0,
      secondsRemaining: 0,
      notifiedActiveCount: 0,
      lastSentAt: null,
      lastRecipients: null,
      activeAlerts: []
    };
  }

  private clearBatchTimers(): void {
    if (this.batchCountdownInterval !== null) {
      clearInterval(this.batchCountdownInterval);
      this.batchCountdownInterval = null;
    }
    this.zeroCountdownPollTicks = 0;
  }

  private clearSentBannerTimer(): void {
    if (this.sentBannerTimeout !== null) {
      clearTimeout(this.sentBannerTimeout);
      this.sentBannerTimeout = null;
    }
  }

  private mostrarBannerCorreoTemporal(count: number, sentAtKey?: string | null): void {
    this.lastDispatchedBatchCount = Math.max(1, count);
    this.showSentBanner = true;
    if (sentAtKey) {
      try {
        sessionStorage.setItem(InventarioComponent.ACK_SENT_AT_KEY, sentAtKey);
      } catch {
        // Ignorar si sessionStorage no esta disponible
      }
    }
    this.clearSentBannerTimer();
    this.sentBannerTimeout = setTimeout(() => {
      this.showSentBanner = false;
      this.sentBannerTimeout = null;
    }, 8000);
  }

  cerrarBannerCorreoEnviado(): void {
    this.clearSentBannerTimer();
    this.showSentBanner = false;
  }

  private aplicarBatchStatus(
    nextStatus: StockAlertBatchStatusDTO | null | undefined,
    emitToastWhenDispatched = false
  ): void {
    const prevPending = this.batchStatus?.pendingBatchCount ?? 0;
    const prevNotified = this.batchStatus?.notifiedActiveCount ?? 0;

    const normalized: StockAlertBatchStatusDTO = {
      ...this.crearBatchStatusPorDefecto(),
      ...(nextStatus || {}),
      activeAlerts: Array.isArray(nextStatus?.activeAlerts) ? nextStatus!.activeAlerts : []
    };

    this.batchStatus = normalized;
    this.activeAlertMailMap.clear();
    for (const alert of normalized.activeAlerts) {
      this.activeAlertMailMap.set(`${alert.itemType}:${alert.itemId}`, alert);
    }

    if (normalized.pendingBatchCount > 0) {
      this.cerrarBannerCorreoEnviado();
    } else if (
      emitToastWhenDispatched &&
      prevPending > 0 &&
      normalized.notifiedActiveCount > prevNotified
    ) {
      const newlyNotified = Math.max(1, normalized.notifiedActiveCount - prevNotified);
      const recipientsText = normalized.lastRecipients
        ? ` a ${normalized.lastRecipients}`
        : ' a los administradores';
      this.toastService.showSuccess(
        `\u00a1Se envi\u00f3 el correo consolidado de alerta de stock (${newlyNotified} producto(s))${recipientsText}!`
      );
      this.mostrarBannerCorreoTemporal(newlyNotified, normalized.lastSentAt);
    } else if (normalized.lastSentAt && normalized.notifiedActiveCount > 0) {
      let alreadyAck = false;
      try {
        alreadyAck = sessionStorage.getItem(InventarioComponent.ACK_SENT_AT_KEY) === normalized.lastSentAt;
      } catch {
        alreadyAck = true;
      }
      if (!alreadyAck) {
        const sentMs = Date.parse(normalized.lastSentAt);
        if (!Number.isNaN(sentMs) && Math.abs(Date.now() - sentMs) <= 30000) {
          this.mostrarBannerCorreoTemporal(normalized.notifiedActiveCount, normalized.lastSentAt);
        } else {
          try {
            sessionStorage.setItem(InventarioComponent.ACK_SENT_AT_KEY, normalized.lastSentAt);
          } catch {
            // Ignorar
          }
        }
      }
    }

    this.syncBatchCountdownTimer();
  }

  private syncBatchCountdownTimer(): void {
    if (this.batchStatus.pendingBatchCount <= 0) {
      this.clearBatchTimers();
      return;
    }

    if (this.batchCountdownInterval !== null) {
      return;
    }

    this.zeroCountdownPollTicks = 0;
    this.batchCountdownInterval = setInterval(() => {
      if (this.batchStatus.pendingBatchCount <= 0) {
        this.clearBatchTimers();
        return;
      }

      if (this.batchStatus.secondsRemaining > 0) {
        this.batchStatus.secondsRemaining = Math.max(0, this.batchStatus.secondsRemaining - 1);
        this.zeroCountdownPollTicks = 0;
        return;
      }

      if (!this.batchStatus.mailOperational) {
        this.clearBatchTimers();
        return;
      }

      this.zeroCountdownPollTicks += 1;
      if (this.zeroCountdownPollTicks % 4 === 2) {
        this.cargarEstadoNotificaciones(true);
      }
      if (this.zeroCountdownPollTicks >= 20) {
        this.clearBatchTimers();
      }
    }, 1000);
  }

  cargarEstadoNotificaciones(emitToastWhenDispatched = false): void {
    this.inventoryService
      .obtenerEstadoNotificacionesAlertas()
      .pipe(catchError(() => of(null)))
      .subscribe((status) => {
        if (status) {
          this.aplicarBatchStatus(status, emitToastWhenDispatched);
        }
      });
  }

  enviarLoteCorreoAhora(): void {
    if (this.sendingBatchNow) return;
    this.sendingBatchNow = true;
    this.inventoryService
      .despacharAlertasPendientesCorreo()
      .pipe(finalize(() => (this.sendingBatchNow = false)))
      .subscribe({
        next: (res) => {
          if (res.dispatchedCount > 0) {
            this.toastService.showSuccess(
              res.message ||
                `\u00a1Se envi\u00f3 el correo consolidado (${res.dispatchedCount} producto(s) en alerta)!`
            );
            this.mostrarBannerCorreoTemporal(res.dispatchedCount, new Date().toISOString());
          } else if (!this.batchStatus.mailOperational) {
            this.toastService.showWarning(
              'El env\u00edo SMTP a\u00fan no est\u00e1 operativo. Verifica las credenciales de Gmail en el archivo .env.'
            );
          } else {
            this.toastService.showInfo(
              res.message || 'No hay alertas pendientes de env\u00edo en este momento.'
            );
          }
          this.cargarEstadoNotificaciones(false);
        },
        error: (err) => {
          this.toastService.showError(
            this.resolverMensajeError(err, 'No se pudo enviar el lote consolidado de alertas.')
          );
        }
      });
  }

  getMailAlertFor(alert: StockAlertItemDTO): TriggerStockAlertDTO | undefined {
    return this.activeAlertMailMap.get(`${alert.itemType}:${alert.itemId}`);
  }

  private resolverMensajeError(err: unknown, fallback: string): string {
    const httpErr = err as { status?: number; error?: { message?: string } };
    if (httpErr?.status === 0) {
      return 'No hay conexi\u00f3n con el servidor (Backend desconectado). Verifique que el servicio est\u00e9 activo.';
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
      ),
      batchStatus: this.inventoryService
        .obtenerEstadoNotificacionesAlertas()
        .pipe(catchError(() => of(this.crearBatchStatusPorDefecto())))
    })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: ({ summary, ingredientes, extras, movements, batchStatus }) => {
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

          this.aplicarBatchStatus(batchStatus, false);
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
          'Error al actualizar los cat\u00e1logos de ingredientes y extras.'
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
        categoryLabel: `Extra \u00b7 ${ext.category}`,
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

  // ==================== RECALCULOS CACHEADOS ====================
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
          mode === 'RESTOCK' ? 'Compra / reposici\u00f3n de almac\u00e9n' : 'Conteo f\u00edsico de inventario'
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
          mode === 'RESTOCK' ? 'Compra / reposici\u00f3n de almac\u00e9n' : 'Conteo f\u00edsico de inventario'
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
      this.toastService.showWarning('El conteo f\u00edsico no puede ser negativo.');
      return;
    }

    const itemKey = `${this.operationForm.itemType}:${Number(this.operationForm.itemId)}`;
    const activeMailAlert = this.activeAlertMailMap.get(itemKey);
    const effectiveMinStock =
      this.operationForm.minStock !== null && this.operationForm.minStock !== undefined
        ? Number(this.operationForm.minStock)
        : Number(this.selectedCatalogItem?.minStock ?? 0);
    const projectedStock = this.projectedStockAfterOperation;

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
        if (this.operationMode === 'RESTOCK') {
          if (activeMailAlert && projectedStock > effectiveMinStock && projectedStock > 0) {
            this.toastService.showSuccess(
              'Stock reabastecido sobre el m\u00ednimo \u00b7 Alerta de correo restablecida'
            );
          } else if (activeMailAlert?.emailSent && projectedStock <= effectiveMinStock) {
            this.toastService.showInfo(
              'Stock actualizado (sigue \u2264 m\u00ednimo: no se reenviar\u00e1 correo duplicado)'
            );
          } else {
            this.toastService.showSuccess('Stock reabastecido y registrado en Kardex');
          }
        } else {
          this.toastService.showSuccess('Ajuste f\u00edsico aplicado y registrado en Kardex');
        }
        this.recargarTodo();
      },
      error: (err) => {
        this.toastService.showError(
          this.resolverMensajeError(err, 'No se pudo aplicar la operaci\u00f3n de inventario.')
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
        return 'Merma / P\u00e9rdida';
      case 'ADJUSTMENT':
        return 'Ajuste F\u00edsico';
      case 'RETURN':
        return 'Devoluci\u00f3n';
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

