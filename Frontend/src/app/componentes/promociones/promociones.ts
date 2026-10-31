import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { PromocionService, Promotion } from '../../services/promocion.service';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-promociones',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './promociones.html',
  styleUrl: './promociones.css'
})
export class Promociones implements OnInit {
  todasLasPromociones: Promotion[] = [];
  promociones: Promotion[] = [];
  promocionesPaginadas: Promotion[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  pageSizeOptions: number[] = [10, 25, 50];

  promocionForm: FormGroup;
  editMode = false;
  selectedPromocionId?: number;
  showForm = false;
  loading = false;
  guardando = false;
  loadError = '';
  error = '';
  success = '';

  // Filtros
  filtroEstado: 'TODOS' | 'ACTIVAS' | 'INACTIVAS' = 'TODOS';
  terminoBusqueda = '';

  // KPIs
  kpiTotalPromociones = 0;
  kpiActivas = 0;
  kpiInactivas = 0;
  kpiConLimiteUso = 0;

  // Precomputed maps to eliminate new Date() allocations per row per render
  vigenciaMap = new Map<number, boolean>();
  fechasMap = new Map<number, { start: string; end: string }>();

  tiposDescuento = [
    { value: 'PERCENTAGE', label: 'Porcentaje (%)' },
    { value: 'FIXED_AMOUNT', label: 'Monto Fijo (S/)' },
    { value: 'BUNDLE', label: 'Precio Final' }
  ];

  aplicableTo = [
    { value: 'ALL', label: 'Todo el menú' },
    { value: 'PIZZAS', label: 'Solo Pizzas' },
    { value: 'EXTRAS', label: 'Solo Extras' },
    { value: 'SPECIFIC_PRODUCTS', label: 'Productos Específicos' }
  ];

  constructor(
    private readonly promocionService: PromocionService,
    private readonly formBuilder: FormBuilder
  ) {
    const today = new Date().toISOString().split('T')[0];
    this.promocionForm = this.formBuilder.group({
      code: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(30)]],
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: [''],
      discountType: ['PERCENTAGE', Validators.required],
      discountValue: [0, Validators.min(0)],
      finalPrice: [0, Validators.min(0)],
      minPurchase: [0, Validators.min(0)],
      maxDiscount: [0, Validators.min(0)],
      isActive: [true],
      startDate: [today, Validators.required],
      endDate: ['', Validators.required],
      usageLimit: [null],
      applicableTo: ['ALL', Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargarPromociones();
  }

  recargar(): void {
    this.cargarPromociones();
  }

  reintentar(): void {
    this.cargarPromociones();
  }

  abrirNuevo(): void {
    this.abrirFormulario();
  }

  cargarPromociones(): void {
    this.loading = true;
    this.loadError = '';
    this.promocionService.listarTodas(true).subscribe({
      next: (data) => {
        this.todasLasPromociones = data || [];
        this.precomputarMetricasYFechas();
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar promociones:', err);
        this.loadError = err?.status === 0
          ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
          : (err?.error?.message || 'Error al cargar las promociones comerciales.');
        this.loading = false;
      }
    });
  }

  private precomputarMetricasYFechas(): void {
    this.vigenciaMap.clear();
    this.fechasMap.clear();

    const ahora = new Date();
    let activas = 0;
    let inactivas = 0;
    let conLimite = 0;

    for (const promo of this.todasLasPromociones) {
      const id = promo.id ?? 0;
      const inicio = new Date(promo.startDate);
      const fin = new Date(promo.endDate);
      const esVig = ahora >= inicio && ahora <= fin && !!promo.isActive;
      this.vigenciaMap.set(id, esVig);

      this.fechasMap.set(id, {
        start: inicio.toLocaleDateString('es-ES'),
        end: fin.toLocaleDateString('es-ES')
      });

      if (esVig) {
        activas++;
      } else {
        inactivas++;
      }

      if (promo.usageLimit && promo.usageLimit > 0) {
        conLimite++;
      }
    }

    this.kpiTotalPromociones = this.todasLasPromociones.length;
    this.kpiActivas = activas;
    this.kpiInactivas = inactivas;
    this.kpiConLimiteUso = conLimite;
  }

  aplicarFiltro(): void {
    let filtradas = [...this.todasLasPromociones];

    if (this.filtroEstado === 'ACTIVAS') {
      filtradas = filtradas.filter(p => this.vigenciaMap.get(p.id ?? 0) === true);
    } else if (this.filtroEstado === 'INACTIVAS') {
      filtradas = filtradas.filter(p => this.vigenciaMap.get(p.id ?? 0) !== true);
    }

    const term = this.terminoBusqueda.trim().toLowerCase();
    if (term) {
      filtradas = filtradas.filter(
        p =>
          p.code?.toLowerCase().includes(term) ||
          p.name?.toLowerCase().includes(term) ||
          p.description?.toLowerCase().includes(term)
      );
    }

    this.promociones = filtradas;
    this.paginar();
  }

  cambiarFiltro(estado: 'TODOS' | 'ACTIVAS' | 'INACTIVAS'): void {
    this.filtroEstado = estado;
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  onBusquedaChange(): void {
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  limpiarFiltros(): void {
    this.filtroEstado = 'TODOS';
    this.terminoBusqueda = '';
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  paginar(): void {
    const totalPages = Math.ceil(this.promociones.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const inicio = (this.currentPage - 1) * this.itemsPerPage;
    const fin = inicio + this.itemsPerPage;
    this.promocionesPaginadas = this.promociones.slice(inicio, fin);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.paginar();
  }

  onItemsPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.paginar();
  }

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    const today = new Date().toISOString().split('T')[0];
    this.promocionForm.reset({
      isActive: true,
      discountType: 'PERCENTAGE',
      applicableTo: 'ALL',
      startDate: today,
      endDate: '',
      discountValue: 0,
      finalPrice: 0,
      minPurchase: 0,
      maxDiscount: 0,
      usageLimit: null
    });
  }

  editarPromocion(promocion: Promotion): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedPromocionId = promocion.id;
    this.promocionForm.patchValue({
      ...promocion,
      startDate: promocion.startDate ? promocion.startDate.split('T')[0] : '',
      endDate: promocion.endDate ? promocion.endDate.split('T')[0] : ''
    });
  }

  guardarPromocion(): void {
    if (this.promocionForm.invalid || this.guardando) return;

    this.guardando = true;
    const promocionData: Promotion = {
      ...this.promocionForm.value,
      startDate: this.promocionForm.value.startDate + 'T00:00:00',
      endDate: this.promocionForm.value.endDate + 'T23:59:59'
    };

    const observable = this.editMode && this.selectedPromocionId
      ? this.promocionService.actualizar(this.selectedPromocionId, promocionData)
      : this.promocionService.crear(promocionData);

    observable.subscribe({
      next: () => {
        this.guardando = false;
        this.success = this.editMode ? 'Promoción actualizada correctamente' : 'Promoción creada correctamente';
        this.cargarPromociones();
        this.cancelar();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.guardando = false;
        this.error = err.error?.message || 'Error al guardar promoción';
      }
    });
  }


  restaurarPromocion(id: number, nombre?: string): void {
    if (!confirm('¿Desea restaurar la promoción' + (nombre ? ` "${nombre}"` : '') + '?')) return;

    this.promocionService.restaurar(id).subscribe({
      next: () => {
        this.success = 'Promoción' + (nombre ? ` "${nombre}"` : '') + ' restaurada con éxito';
        this.cargarPromociones();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al restaurar promoción';
        setTimeout(() => this.error = '', 3000);
      }
    });
  }

  eliminarPromocion(id: number): void {
    if (!confirm('¿Está seguro de eliminar esta promoción?')) return;

    this.promocionService.eliminar(id).subscribe({
      next: () => {
        this.success = 'Promoción eliminada correctamente';
        this.cargarPromociones();
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al eliminar promoción';
      }
    });
  }

  toggleActivar(promocion: Promotion): void {
    const id = promocion.id!;
    const observable = promocion.isActive
      ? this.promocionService.desactivar(id)
      : this.promocionService.activar(id);

    observable.subscribe({
      next: () => {
        this.success = promocion.isActive ? 'Promoción desactivada' : 'Promoción activada';
        this.cargarPromociones();
        setTimeout(() => this.success = '', 2000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al cambiar estado de la promoción';
      }
    });
  }

  cancelar(): void {
    this.showForm = false;
    this.editMode = false;
    this.selectedPromocionId = undefined;
    this.promocionForm.reset();
  }

  formatearFecha(fecha: string): string {
    return new Date(fecha).toLocaleDateString('es-ES');
  }

  obtenerFechasPromo(id?: number): { start: string; end: string } {
    if (!id || !this.fechasMap.has(id)) {
      return { start: '-', end: '-' };
    }
    return this.fechasMap.get(id)!;
  }

  esVigente(promocion: Promotion): boolean {
    if (!promocion.id) return false;
    return this.vigenciaMap.get(promocion.id) ?? false;
  }

  trackByPromoId(index: number, item: Promotion): number {
    return item.id ?? index;
  }
}
