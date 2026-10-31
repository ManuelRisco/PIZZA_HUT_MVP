import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { ProductionCostService } from '../../services/production-cost.service';
import { PizzaService } from '../../services/pizza.service';
import { Size } from '../../services/size.service';
import { ExtraService, Extra } from '../../services/extra.service';
import { IngredientService } from '../../services/ingredient.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { ToastService } from '../../services/toast.service';
import { PaginationComponent } from '../pagination/pagination';
import { PizzaDTO } from '../../models/pizza.interface';
import {
  BatchItemRequestDTO,
  ExtraCostDTO,
  IngredientDTO,
  PizzaSizeCostDTO,
  ProductionEstimateResponseDTO,
  SizeDTO
} from '../../models/admin.interface';

export interface BatchBuilderRow extends BatchItemRequestDTO {
  uid: number;
  label: string;
  subLabel: string;
}

@Component({
  selector: 'app-costos-produccion',
  standalone: true,
  imports: [CommonModule, FormsModule, PaginationComponent],
  templateUrl: './costos-produccion.html',
  styleUrls: ['./costos-produccion.css']
})
export class CostosProduccionComponent implements OnInit, OnDestroy {
  activeTab: 'catalogo' | 'simulador' = 'catalogo';
  catalogView: 'PIZZAS' | 'EXTRAS' = 'PIZZAS';
  loadingCatalog = false;
  calculatingBatch = false;
  loadError = '';

  pizzaCosts: PizzaSizeCostDTO[] = [];
  extraCosts: ExtraCostDTO[] = [];

  pizzas: PizzaDTO[] = [];
  sizes: SizeDTO[] = [];
  extras: Extra[] = [];
  extraToppings: IngredientDTO[] = [];

  // KPIs precalculados para evitar getters durante Change Detection
  avgPizzaDirectCost = 0;
  avgPizzaMargin = 0;
  avgExtraMargin = 0;

  // Filtros Catálogo
  searchCatalog = '';
  selectedSizeFilter: number | 'TODOS' = 'TODOS';
  selectedExtraCategoryFilter = 'TODOS';
  pizzaCatalogPage = 1;
  extraCatalogPage = 1;
  itemsPerPage = 12;

  filteredPizzaCosts: PizzaSizeCostDTO[] = [];
  paginatedPizzaCosts: PizzaSizeCostDTO[] = [];
  filteredExtraCosts: ExtraCostDTO[] = [];
  paginatedExtraCosts: ExtraCostDTO[] = [];

  // Modal Desglose de Receta
  selectedRecipeDetail: PizzaSizeCostDTO | null = null;

  // Simulador de Lote de Producción
  private uidCounter = 1;
  private batchDebounceTimer: any = null;

  builderForm: {
    itemType: 'PIZZA' | 'EXTRA';
    pizzaId: number | null;
    sizeId: number | null;
    extraId: number | null;
    selectedToppingIds: number[];
    quantity: number;
  } = {
    itemType: 'PIZZA',
    pizzaId: null,
    sizeId: null,
    extraId: null,
    selectedToppingIds: [],
    quantity: 5
  };

  batchItems: BatchBuilderRow[] = [];
  estimateResult: ProductionEstimateResponseDTO | null = null;

  constructor(
    private readonly productionCostService: ProductionCostService,
    private readonly pizzaService: PizzaService,
    private readonly sizeService: Size,
    private readonly extraService: ExtraService,
    private readonly ingredientService: IngredientService,
    private readonly imageOptimizer: ImageOptimizerService,
    private readonly toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.cargarCatalogoCostos();
    this.cargarListasAuxiliares();
  }

  ngOnDestroy(): void {
    if (this.batchDebounceTimer) {
      clearTimeout(this.batchDebounceTimer);
    }
  }

  cargarCatalogoCostos(): void {
    this.loadingCatalog = true;
    this.loadError = '';

    this.productionCostService.obtenerCatalogoCostos()
      .pipe(finalize(() => { this.loadingCatalog = false; }))
      .subscribe({
        next: (res) => {
          this.pizzaCosts = res?.pizzaCosts || [];
          this.extraCosts = res?.extraCosts || [];
          this.recalcularVistasCatalogo();
        },
        error: (err) => {
          console.error('Error al cargar catálogo de costos:', err);
          this.loadError = err?.status === 0
            ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
            : (err?.error?.message || 'Error al cargar el catálogo de costos de producción.');
          this.pizzaCosts = [];
          this.extraCosts = [];
          this.recalcularVistasCatalogo();
        }
      });
  }

  cargarListasAuxiliares(): void {
    this.pizzaService.listarPizzas().subscribe({
      next: (res) => {
        this.pizzas = res || [];
        if (!this.builderForm.pizzaId && this.pizzas.length > 0) {
          this.builderForm.pizzaId = this.pizzas[0].id ?? null;
        }
      }
    });

    this.sizeService.obtenerTodos().subscribe({
      next: (res) => {
        this.sizes = res || [];
        if (!this.builderForm.sizeId && this.sizes.length > 0) {
          this.builderForm.sizeId = this.sizes[0].id ?? null;
        }
      }
    });

    this.extraService.listarTodos().subscribe({
      next: (res) => {
        this.extras = res || [];
        if (!this.builderForm.extraId && this.extras.length > 0) {
          this.builderForm.extraId = this.extras[0].id ?? null;
        }
      }
    });

    this.ingredientService.obtenerTodos().subscribe({
      next: (res) => {
        this.extraToppings = (res || []).filter((i) => i.allowAsExtra !== false);
      }
    });
  }

  recalcularVistasCatalogo(): void {
    // 1. KPIs
    if (this.pizzaCosts.length === 0) {
      this.avgPizzaDirectCost = 0;
      this.avgPizzaMargin = 0;
    } else {
      const baseRows = this.pizzaCosts.filter((p) => Number(p.recipeMultiplier) === 1);
      const source = baseRows.length > 0 ? baseRows : this.pizzaCosts;
      this.avgPizzaDirectCost = source.reduce((s, r) => s + Number(r.directCost ?? 0), 0) / source.length;
      this.avgPizzaMargin = this.pizzaCosts.reduce((s, r) => s + Number(r.profitMargin ?? 0), 0) / this.pizzaCosts.length;
    }

    this.avgExtraMargin = this.extraCosts.length > 0
      ? this.extraCosts.reduce((s, r) => s + Number(r.profitMargin ?? 0), 0) / this.extraCosts.length
      : 0;

    // 2. Filtros Pizzas
    const term = this.searchCatalog.trim().toLowerCase();
    const filteredPizzas = this.pizzaCosts.filter((row) => {
      if (this.selectedSizeFilter !== 'TODOS' && row.sizeId !== Number(this.selectedSizeFilter)) {
        return false;
      }
      if (
        term &&
        !row.pizzaName.toLowerCase().includes(term) &&
        !(row.categoryName || '').toLowerCase().includes(term) &&
        !row.sizeName.toLowerCase().includes(term)
      ) {
        return false;
      }
      return true;
    });

    this.filteredPizzaCosts = filteredPizzas;
    const totalPizzaPages = Math.ceil(filteredPizzas.length / this.itemsPerPage);
    if (this.pizzaCatalogPage > totalPizzaPages && totalPizzaPages > 0) {
      this.pizzaCatalogPage = totalPizzaPages;
    } else if (this.pizzaCatalogPage < 1) {
      this.pizzaCatalogPage = 1;
    }
    const pizzaStart = (this.pizzaCatalogPage - 1) * this.itemsPerPage;
    this.paginatedPizzaCosts = filteredPizzas.slice(pizzaStart, pizzaStart + this.itemsPerPage);

    // 3. Filtros Extras
    const filteredExtras = this.extraCosts.filter((row) => {
      if (
        this.selectedExtraCategoryFilter !== 'TODOS' &&
        row.category !== this.selectedExtraCategoryFilter
      ) {
        return false;
      }
      if (
        term &&
        !row.name.toLowerCase().includes(term) &&
        !(row.category || '').toLowerCase().includes(term)
      ) {
        return false;
      }
      return true;
    });

    this.filteredExtraCosts = filteredExtras;
    const totalExtraPages = Math.ceil(filteredExtras.length / this.itemsPerPage);
    if (this.extraCatalogPage > totalExtraPages && totalExtraPages > 0) {
      this.extraCatalogPage = totalExtraPages;
    } else if (this.extraCatalogPage < 1) {
      this.extraCatalogPage = 1;
    }
    const extraStart = (this.extraCatalogPage - 1) * this.itemsPerPage;
    this.paginatedExtraCosts = filteredExtras.slice(extraStart, extraStart + this.itemsPerPage);
  }

  onPizzaCatalogPageChange(page: number): void {
    this.pizzaCatalogPage = page;
    this.recalcularVistasCatalogo();
  }

  onPizzaCatalogPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.pizzaCatalogPage = 1;
    this.recalcularVistasCatalogo();
  }

  onExtraCatalogPageChange(page: number): void {
    this.extraCatalogPage = page;
    this.recalcularVistasCatalogo();
  }

  onExtraCatalogPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.extraCatalogPage = 1;
    this.recalcularVistasCatalogo();
  }

  getThumbUrl(imageUrl?: string): string {
    return this.imageOptimizer.resolveImageUrl(imageUrl, true);
  }

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    if (img && !img.src.endsWith('/combo1.webp')) {
      img.src = '/combo1.webp';
    }
  }

  isItemAvailable(item: { available?: boolean; isAvailable?: boolean }): boolean {
    return item.available ?? item.isAvailable ?? true;
  }

  verDesgloseReceta(row: PizzaSizeCostDTO): void {
    this.selectedRecipeDetail = row;
  }

  cerrarDesgloseReceta(): void {
    this.selectedRecipeDetail = null;
  }

  getVariantesTamanoMismaPizza(detail: PizzaSizeCostDTO | null): PizzaSizeCostDTO[] {
    if (!detail) return [];
    return this.pizzaCosts.filter(p => p.pizzaId === detail.pizzaId);
  }

  cambiarTamanoDesglose(row: PizzaSizeCostDTO): void {
    this.selectedRecipeDetail = row;
  }

  getIngredientCostShare(lineCost: number, totalDirectCost: number): number {
    if (!totalDirectCost || totalDirectCost <= 0) return 0;
    return (Number(lineCost) / Number(totalDirectCost)) * 100;
  }

  // Simulador de Lote
  agregarPizzaDesdeCatalogo(row: PizzaSizeCostDTO): void {
    const existing = this.batchItems.find(
      (b) =>
        b.itemType === 'PIZZA' &&
        b.pizzaId === row.pizzaId &&
        b.sizeId === row.sizeId &&
        (!b.extraIngredientIds || b.extraIngredientIds.length === 0)
    );
    if (existing) {
      existing.quantity += 5;
    } else {
      this.batchItems.push({
        uid: this.uidCounter++,
        itemType: 'PIZZA',
        pizzaId: row.pizzaId,
        sizeId: row.sizeId,
        extraIngredientIds: [],
        quantity: 5,
        label: `${row.pizzaName} (${row.sizeName})`,
        subLabel: `Pizza · Receta x${Number(row.recipeMultiplier).toFixed(2)}`
      });
    }
    this.activeTab = 'simulador';
    this.toastService.showInfo(`${row.pizzaName} (${row.sizeName}) agregada al simulador`);
    this.calcularEstimacionLote();
  }

  agregarExtraDesdeCatalogo(row: ExtraCostDTO): void {
    const existing = this.batchItems.find(
      (b) => b.itemType === 'EXTRA' && b.extraId === row.extraId
    );
    if (existing) {
      existing.quantity += 5;
    } else {
      this.batchItems.push({
        uid: this.uidCounter++,
        itemType: 'EXTRA',
        extraId: row.extraId,
        quantity: 5,
        label: row.name,
        subLabel: `Extra · ${row.category}`
      });
    }
    this.activeTab = 'simulador';
    this.toastService.showInfo(`${row.name} agregado al simulador`);
    this.calcularEstimacionLote();
  }

  toggleToppingBuilder(ingId?: number): void {
    if (!ingId) return;
    const idx = this.builderForm.selectedToppingIds.indexOf(ingId);
    if (idx >= 0) {
      this.builderForm.selectedToppingIds.splice(idx, 1);
    } else {
      this.builderForm.selectedToppingIds.push(ingId);
    }
  }

  agregarItemDesdeFormulario(): void {
    const qty = Math.round(Number(this.builderForm.quantity ?? 0));
    if (Number.isNaN(qty) || qty <= 0) {
      this.toastService.showWarning('La cantidad del lote debe ser al menos 1 unidad.');
      return;
    }

    if (this.builderForm.itemType === 'PIZZA') {
      const piz = this.pizzas.find((p) => p.id === Number(this.builderForm.pizzaId));
      const sz = this.sizes.find((s) => s.id === Number(this.builderForm.sizeId));
      if (!piz?.id || !sz?.id) {
        this.toastService.showWarning('Selecciona una pizza y un tamaño.');
        return;
      }
      const toppingIds = [...this.builderForm.selectedToppingIds].sort((a, b) => a - b);
      const toppingNames = toppingIds
        .map((id) => this.extraToppings.find((t) => t.id === id)?.name)
        .filter(Boolean);

      this.batchItems.push({
        uid: this.uidCounter++,
        itemType: 'PIZZA',
        pizzaId: piz.id,
        sizeId: sz.id,
        extraIngredientIds: toppingIds,
        quantity: qty,
        label: `${piz.name} (${sz.name})`,
        subLabel:
          toppingNames.length > 0
            ? `Toppings extra: ${toppingNames.join(', ')}`
            : `Pizza estándar · Receta x${Number(sz.recipeMultiplier ?? 1).toFixed(2)}`
      });
      this.builderForm.selectedToppingIds = [];
    } else {
      const ext = this.extras.find((e) => e.id === Number(this.builderForm.extraId));
      if (!ext?.id) {
        this.toastService.showWarning('Selecciona un producto extra.');
        return;
      }
      const existing = this.batchItems.find(
        (b) => b.itemType === 'EXTRA' && b.extraId === ext.id
      );
      if (existing) {
        existing.quantity += qty;
      } else {
        this.batchItems.push({
          uid: this.uidCounter++,
          itemType: 'EXTRA',
          extraId: ext.id,
          quantity: qty,
          label: ext.name,
          subLabel: `Extra · ${ext.category}`
        });
      }
    }

    this.calcularEstimacionLote();
  }

  cargarLoteEjemplo(): void {
    this.batchItems = [];
    const firstPizza = this.pizzas[0];
    const secondPizza = this.pizzas[1] || this.pizzas[0];
    const famSize =
      this.sizes.find((s) => s.name.toLowerCase().includes('familiar')) ||
      this.sizes[this.sizes.length - 1] ||
      this.sizes[0];
    const medSize =
      this.sizes.find((s) => s.name.toLowerCase().includes('mediana')) || this.sizes[0];
    const firstExtra = this.extras[0];

    if (firstPizza?.id && famSize?.id) {
      this.batchItems.push({
        uid: this.uidCounter++,
        itemType: 'PIZZA',
        pizzaId: firstPizza.id,
        sizeId: famSize.id,
        extraIngredientIds: [],
        quantity: 10,
        label: `${firstPizza.name} (${famSize.name})`,
        subLabel: `Pizza · Receta x${Number(famSize.recipeMultiplier ?? 1).toFixed(2)}`
      });
    }
    if (secondPizza?.id && medSize?.id) {
      this.batchItems.push({
        uid: this.uidCounter++,
        itemType: 'PIZZA',
        pizzaId: secondPizza.id,
        sizeId: medSize.id,
        extraIngredientIds: [],
        quantity: 8,
        label: `${secondPizza.name} (${medSize.name})`,
        subLabel: `Pizza · Receta x${Number(medSize.recipeMultiplier ?? 1).toFixed(2)}`
      });
    }
    if (firstExtra?.id) {
      this.batchItems.push({
        uid: this.uidCounter++,
        itemType: 'EXTRA',
        extraId: firstExtra.id,
        quantity: 12,
        label: firstExtra.name,
        subLabel: `Extra · ${firstExtra.category}`
      });
    }

    this.calcularEstimacionLote();
  }

  cambiarCantidadItem(item: BatchBuilderRow, delta: number): void {
    const next = Math.max(1, Math.round(Number(item.quantity ?? 1) + delta));
    item.quantity = next;
    this.debouncedCalcularEstimacionLote();
  }

  onCantidadInputChange(item: BatchBuilderRow): void {
    if (!item.quantity || item.quantity < 1) {
      item.quantity = 1;
    } else {
      item.quantity = Math.round(item.quantity);
    }
    this.debouncedCalcularEstimacionLote();
  }

  debouncedCalcularEstimacionLote(): void {
    if (this.batchDebounceTimer) {
      clearTimeout(this.batchDebounceTimer);
    }
    this.batchDebounceTimer = setTimeout(() => {
      this.calcularEstimacionLote();
    }, 300);
  }

  quitarItemLote(uid: number): void {
    this.batchItems = this.batchItems.filter((i) => i.uid !== uid);
    if (this.batchItems.length === 0) {
      this.estimateResult = null;
    } else {
      this.calcularEstimacionLote();
    }
  }

  limpiarLote(): void {
    this.batchItems = [];
    this.estimateResult = null;
  }

  calcularEstimacionLote(): void {
    if (this.batchItems.length === 0) {
      this.estimateResult = null;
      return;
    }

    const payloadItems: BatchItemRequestDTO[] = this.batchItems.map((b) => ({
      itemType: b.itemType,
      pizzaId: b.pizzaId ?? null,
      sizeId: b.sizeId ?? null,
      extraId: b.extraId ?? null,
      extraIngredientIds: b.extraIngredientIds || [],
      quantity: Math.max(1, Math.round(Number(b.quantity ?? 1)))
    }));

    this.calculatingBatch = true;
    this.productionCostService.estimarLoteProduccion({ items: payloadItems })
      .pipe(finalize(() => { this.calculatingBatch = false; }))
      .subscribe({
        next: (res) => {
          this.estimateResult = res;
        },
        error: (err) => {
          console.error('Error al estimar lote:', err);
          this.toastService.showError('Error al estimar el lote de producción.');
        }
      });
  }

  trackByPizzaCost(_index: number, row: PizzaSizeCostDTO): string {
    return `${row.pizzaId}-${row.sizeId}`;
  }

  trackByExtraCost(_index: number, row: ExtraCostDTO): number {
    return row.extraId;
  }

  trackByBatchItem(_index: number, item: BatchBuilderRow): number {
    return item.uid;
  }

  trackByIngredientReq(_index: number, ing: any): number {
    return ing.ingredientId;
  }
}
