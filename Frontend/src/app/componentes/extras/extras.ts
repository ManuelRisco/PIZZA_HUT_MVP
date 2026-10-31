import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ExtraService, Extra } from '../../services/extra.service';
import { FileUploadService } from '../../services/file-upload.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-extras',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './extras.html',
  styleUrl: './extras.css'
})
export class ExtrasComponent implements OnInit {
  todosLosExtras: Extra[] = [];
  extras: Extra[] = [];
  extrasPaginados: Extra[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  pageSizeOptions: number[] = [10, 25, 50];

  extraForm: FormGroup;
  editMode = false;
  selectedExtraId?: number;
  showForm = false;
  loading = false;
  guardando = false;
  subiendoImagen = false;
  imagenPreview: string | null = null;
  loadError = '';
  error = '';
  success = '';

  // Filtros
  filtroCategoria: string = 'TODOS';
  terminoBusqueda: string = '';

  // KPIs precalculados
  kpiTotalExtras = 0;
  kpiConStock = 0;
  kpiStockBajo = 0;
  kpiMargenPromedio = 0;

  categorias = [
    { value: 'BEBIDA', label: 'Bebidas', icon: 'bi-cup-straw' },
    { value: 'POSTRE', label: 'Postres', icon: 'bi-cake2' },
    { value: 'ENTRADA', label: 'Entradas', icon: 'bi-egg-fried' },
    { value: 'COMPLEMENTO', label: 'Complementos', icon: 'bi-box-seam' }
  ];

  private readonly extraService = inject(ExtraService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly imageOptimizer = inject(ImageOptimizerService);

  constructor() {
    this.extraForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(120)]],
      description: [''],
      imageUrl: [''],
      price: [0, [Validators.required, Validators.min(0)]],
      unitCost: [0, [Validators.required, Validators.min(0)]],
      stockQuantity: [25, [Validators.required, Validators.min(0)]],
      minStock: [5, [Validators.required, Validators.min(0)]],
      category: ['BEBIDA', Validators.required],
      isAvailable: [true],
      displayOrder: [0, Validators.min(0)]
    });
  }

  ngOnInit(): void {
    this.cargarExtras();
  }

  recargar(): void {
    this.cargarExtras();
  }

  reintentar(): void {
    this.cargarExtras();
  }

  abrirNuevo(): void {
    this.abrirFormulario();
  }

  cargarExtras(): void {
    this.loading = true;
    this.loadError = '';
    this.extraService.listarTodos().subscribe({
      next: (data) => {
        this.todosLosExtras = data || [];
        this.recalcularKpis();
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar extras:', err);
        this.loadError = err?.status === 0
          ? 'No se pudo conectar con el servidor (Backend fuera de línea). Verifica el puerto 8089.'
          : (err?.error?.message || 'Error al cargar el catálogo de extras.');
        this.loading = false;
      }
    });
  }

  private recalcularKpis(): void {
    let conStock = 0;
    let bajo = 0;
    let sumaMargen = 0;

    for (const e of this.todosLosExtras) {
      if ((e.stockQuantity ?? 0) > 0 && !e.outOfStock) {
        conStock++;
      } else {
        bajo++;
      }
      sumaMargen += (e.profitMargin ?? 0);
    }

    this.kpiTotalExtras = this.todosLosExtras.length;
    this.kpiConStock = conStock;
    this.kpiStockBajo = bajo;
    this.kpiMargenPromedio = this.kpiTotalExtras > 0 ? (sumaMargen / this.kpiTotalExtras) : 0;
  }

  aplicarFiltro(): void {
    let filtrados = [...this.todosLosExtras];

    if (this.filtroCategoria === 'STOCK_BAJO') {
      filtrados = filtrados.filter(e => e.lowStock || e.outOfStock);
    } else if (this.filtroCategoria !== 'TODOS') {
      filtrados = filtrados.filter(e => e.category === this.filtroCategoria);
    }

    const term = this.terminoBusqueda.trim().toLowerCase();
    if (term) {
      filtrados = filtrados.filter(
        e =>
          e.name?.toLowerCase().includes(term) ||
          e.description?.toLowerCase().includes(term) ||
          e.category?.toLowerCase().includes(term)
      );
    }

    this.extras = filtrados;
    this.paginar();
  }

  cambiarFiltro(categoria: string): void {
    this.filtroCategoria = categoria;
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  onBusquedaChange(): void {
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  limpiarFiltros(): void {
    this.filtroCategoria = 'TODOS';
    this.terminoBusqueda = '';
    this.currentPage = 1;
    this.aplicarFiltro();
  }

  paginar(): void {
    const totalPages = Math.ceil(this.extras.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const inicio = (this.currentPage - 1) * this.itemsPerPage;
    const fin = inicio + this.itemsPerPage;
    this.extrasPaginados = this.extras.slice(inicio, fin);
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

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.error = 'Selecciona un archivo de imagen válido';
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.imagenPreview = reader.result as string;
    };
    reader.readAsDataURL(file);

    this.subiendoImagen = true;
    this.error = '';

    this.fileUploadService.uploadExtraImage(file).subscribe({
      next: (res) => {
        this.extraForm.patchValue({ imageUrl: res.imageUrl });
        this.imagenPreview = this.imageOptimizer.resolveImageUrl(res.imageUrl, false);
        this.subiendoImagen = false;
        this.success = 'Imagen subida al backend correctamente';
        setTimeout(() => (this.success = ''), 3000);
      },
      error: (err) => {
        this.subiendoImagen = false;
        this.error = err.error?.message || 'Error al subir la imagen del extra';
      }
    });
  }

  quitarImagenSeleccionada(): void {
    this.extraForm.patchValue({ imageUrl: '' });
    this.imagenPreview = null;
  }

  getImageUrl(imageUrl?: string): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'low');
  }

  calcularMargenFormulario(): number {
    const price = Number(this.extraForm.get('price')?.value) || 0;
    const cost = Number(this.extraForm.get('unitCost')?.value) || 0;
    if (price <= 0) return 0;
    return ((price - cost) / price) * 100;
  }

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    this.imagenPreview = null;
    this.extraForm.reset({
      isAvailable: true,
      category: 'BEBIDA',
      displayOrder: this.todosLosExtras.length + 1,
      imageUrl: '',
      price: 0,
      unitCost: 0,
      stockQuantity: 25,
      minStock: 5
    });
  }

  editarExtra(extra: Extra): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedExtraId = extra.id;
    this.extraForm.patchValue({
      ...extra,
      unitCost: extra.unitCost ?? 0,
      stockQuantity: extra.stockQuantity ?? 0,
      minStock: extra.minStock ?? 5
    });
    this.imagenPreview = extra.imageUrl ? this.imageOptimizer.resolveImageUrl(extra.imageUrl, false) : null;
  }

  guardarExtra(): void {
    if (this.extraForm.invalid || this.guardando) return;

    this.guardando = true;
    const extraData: Extra = this.extraForm.value;

    const observable =
      this.editMode && this.selectedExtraId
        ? this.extraService.actualizar(this.selectedExtraId, extraData)
        : this.extraService.crear(extraData);

    observable.subscribe({
      next: () => {
        this.guardando = false;
        this.success = this.editMode ? 'Extra actualizado correctamente' : 'Extra creado correctamente';
        this.cargarExtras();
        this.cancelar();
        setTimeout(() => (this.success = ''), 3000);
      },
      error: (err) => {
        this.guardando = false;
        this.error = err.error?.message || 'Error al guardar extra';
      }
    });
  }

  eliminarExtra(id: number): void {
    if (!confirm('¿Está seguro de eliminar este extra?')) return;

    this.extraService.eliminar(id).subscribe({
      next: () => {
        this.success = 'Extra eliminado correctamente';
        this.cargarExtras();
        setTimeout(() => (this.success = ''), 3000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al eliminar extra';
      }
    });
  }

  cambiarDisponibilidad(id: number): void {
    this.extraService.cambiarDisponibilidad(id).subscribe({
      next: () => {
        this.success = 'Disponibilidad actualizada';
        this.cargarExtras();
        setTimeout(() => (this.success = ''), 2000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Error al cambiar disponibilidad';
      }
    });
  }

  cancelar(): void {
    this.showForm = false;
    this.editMode = false;
    this.selectedExtraId = undefined;
    this.imagenPreview = null;
    this.extraForm.reset();
  }

  obtenerIconoCategoria(categoria: string): string {
    const cat = this.categorias.find(c => c.value === categoria);
    return cat?.icon || 'bi-box';
  }

  obtenerNombreCategoria(categoria: string): string {
    const cat = this.categorias.find(c => c.value === categoria);
    return cat?.label || categoria;
  }

  trackByExtraId(index: number, item: Extra): number {
    return item.id ?? index;
  }
}
