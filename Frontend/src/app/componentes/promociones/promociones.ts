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
  promociones: Promotion[] = [];
  promocionesPaginadas: Promotion[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  promocionForm: FormGroup;
  editMode = false;
  selectedPromocionId?: number;
  showForm = false;
  loading = false;
  error = '';
  success = '';
  filtroEstado: string = 'TODOS';

  tiposDescuento = [
    { value: 'PERCENTAGE', label: 'Porcentaje (%)' },
    { value: 'FIXED_AMOUNT', label: 'Monto Fijo (S/)' },
    { value: 'BUNDLE', label: 'Precio Final' }
  ];

  aplicableTo = [
    { value: 'ALL', label: 'Todo' },
    { value: 'PIZZAS', label: 'Solo Pizzas' },
    { value: 'EXTRAS', label: 'Solo Extras' },
    { value: 'SPECIFIC_PRODUCTS', label: 'Productos EspecÃ­ficos' }
  ];

  constructor(
    private readonly promocionService: PromocionService,
    private readonly formBuilder: FormBuilder
  ) {
    const today = new Date().toISOString().split('T')[0];
    this.promocionForm = this.formBuilder.group({
      code: ['', [Validators.required, Validators.minLength(3)]],
      name: ['', [Validators.required, Validators.minLength(3)]],
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

  cargarPromociones(): void {
    this.loading = true;
    this.promocionService.listarTodas().subscribe({
      next: (data) => {
        this.promociones = data;
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar promociones';
        this.loading = false;
      }
    });
  }

  aplicarFiltro(): void {
    let filtradas = [...this.promociones];
    
    if (this.filtroEstado === 'ACTIVAS') {
      filtradas = filtradas.filter(p => p.isActive && p.currentlyActive);
    } else if (this.filtroEstado === 'INACTIVAS') {
      filtradas = filtradas.filter(p => !p.isActive || !p.currentlyActive);
    }
    
    this.promociones = filtradas;
    this.paginar();
  }

  cambiarFiltro(estado: string): void {
    this.filtroEstado = estado;
    this.currentPage = 1;
    this.cargarPromociones();
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

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    const today = new Date().toISOString().split('T')[0];
    this.promocionForm.reset({ 
      isActive: true, 
      discountType: 'PERCENTAGE', 
      applicableTo: 'ALL',
      startDate: today,
      discountValue: 0,
      finalPrice: 0,
      minPurchase: 0,
      maxDiscount: 0
    });
  }

  editarPromocion(promocion: Promotion): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedPromocionId = promocion.id;
    this.promocionForm.patchValue({
      ...promocion,
      startDate: promocion.startDate.split('T')[0],
      endDate: promocion.endDate.split('T')[0]
    });
  }

  guardarPromocion(): void {
    if (this.promocionForm.invalid) return;

    this.loading = true;
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
        this.success = this.editMode ? 'PromociÃ³n actualizada correctamente' : 'PromociÃ³n creada correctamente';
        this.cargarPromociones();
        this.cancelar();
        this.loading = false;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al guardar promociÃ³n';
        this.loading = false;
      }
    });
  }

  eliminarPromocion(id: number): void {
    if (!confirm('Â¿EstÃ¡ seguro de eliminar esta promociÃ³n?')) return;

    this.loading = true;
    this.promocionService.eliminar(id).subscribe({
      next: () => {
        this.success = 'PromociÃ³n eliminada correctamente';
        this.cargarPromociones();
        this.loading = false;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = 'Error al eliminar promociÃ³n';
        this.loading = false;
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
        this.success = promocion.isActive ? 'PromociÃ³n desactivada' : 'PromociÃ³n activada';
        this.cargarPromociones();
        setTimeout(() => this.success = '', 2000);
      },
      error: (err) => {
        this.error = 'Error al cambiar estado';
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

  esVigente(promocion: Promotion): boolean {
    const ahora = new Date();
    const inicio = new Date(promocion.startDate);
    const fin = new Date(promocion.endDate);
    return ahora >= inicio && ahora <= fin && promocion.isActive;
  }
}

