import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ExtraService, Extra } from '../../services/extra.service';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-extras',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './extras.html',
  styleUrl: './extras.css'
})
export class ExtrasComponent implements OnInit {
  extras: Extra[] = [];
  extrasPaginados: Extra[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  extraForm: FormGroup;
  editMode = false;
  selectedExtraId?: number;
  showForm = false;
  loading = false;
  error = '';
  success = '';
  filtroCategoria: string = 'TODOS';

  categorias = [
    { value: 'BEBIDA', label: 'Bebidas', icon: 'bi-cup-straw' },
    { value: 'POSTRE', label: 'Postres', icon: 'bi-cake2' },
    { value: 'ENTRADA', label: 'Entradas', icon: 'bi-egg-fried' },
    { value: 'COMPLEMENTO', label: 'Complementos', icon: 'bi-box-seam' }
  ];

  constructor(
    private readonly extraService: ExtraService,
    private readonly formBuilder: FormBuilder
  ) {
    this.extraForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(2)]],
      description: [''],
      price: ['', [Validators.required, Validators.min(0)]],
      category: ['BEBIDA', Validators.required],
      isAvailable: [true],
      displayOrder: [0, Validators.min(0)]
    });
  }

  ngOnInit(): void {
    this.cargarExtras();
  }

  cargarExtras(): void {
    this.loading = true;
    this.extraService.listarTodos().subscribe({
      next: (data) => {
        this.extras = data;
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar extras';
        this.loading = false;
      }
    });
  }

  aplicarFiltro(): void {
    let filtrados = [...this.extras];
    
    if (this.filtroCategoria !== 'TODOS') {
      filtrados = filtrados.filter(e => e.category === this.filtroCategoria);
    }
    
    this.extras = filtrados;
    this.paginar();
  }

  cambiarFiltro(categoria: string): void {
    this.filtroCategoria = categoria;
    this.currentPage = 1;
    this.cargarExtras();
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

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    this.extraForm.reset({ isAvailable: true, category: 'BEBIDA', displayOrder: 0 });
  }

  editarExtra(extra: Extra): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedExtraId = extra.id;
    this.extraForm.patchValue(extra);
  }

  guardarExtra(): void {
    if (this.extraForm.invalid) return;

    this.loading = true;
    const extraData: Extra = this.extraForm.value;

    const observable = this.editMode && this.selectedExtraId
      ? this.extraService.actualizar(this.selectedExtraId, extraData)
      : this.extraService.crear(extraData);

    observable.subscribe({
      next: () => {
        this.success = this.editMode ? 'Extra actualizado correctamente' : 'Extra creado correctamente';
        this.cargarExtras();
        this.cancelar();
        this.loading = false;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = err.error?.message || 'Error al guardar extra';
        this.loading = false;
      }
    });
  }

  eliminarExtra(id: number): void {
    if (!confirm('¿Está seguro de eliminar este extra?')) return;

    this.loading = true;
    this.extraService.eliminar(id).subscribe({
      next: () => {
        this.success = 'Extra eliminado correctamente';
        this.cargarExtras();
        this.loading = false;
        setTimeout(() => this.success = '', 3000);
      },
      error: (err) => {
        this.error = 'Error al eliminar extra';
        this.loading = false;
      }
    });
  }

  cambiarDisponibilidad(id: number): void {
    this.extraService.cambiarDisponibilidad(id).subscribe({
      next: () => {
        this.success = 'Disponibilidad actualizada';
        this.cargarExtras();
        setTimeout(() => this.success = '', 2000);
      },
      error: (err) => {
        this.error = 'Error al cambiar disponibilidad';
      }
    });
  }

  cancelar(): void {
    this.showForm = false;
    this.editMode = false;
    this.selectedExtraId = undefined;
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
}

