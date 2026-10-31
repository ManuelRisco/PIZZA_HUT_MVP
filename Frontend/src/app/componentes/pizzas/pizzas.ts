import { Component, OnInit, OnDestroy, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { PizzaService } from '../../services/pizza.service';
import { PizzaPatronesService } from '../../services/pizza-patrones.service';
import { IngredientService } from '../../services/ingredient.service';
import { Size } from '../../services/size.service';
import { ImageOptimizerService } from '../../services/image-optimizer.service';
import { FileUploadService } from '../../services/file-upload.service';
import { PizzaDTO, PizzaCreateDTO, PizzaIngredientDTO, CategoryDTO } from '../../models/pizza.interface';
import { IngredientDTO, SizeDTO } from '../../models/admin.interface';
import { PaginationComponent } from '../pagination/pagination';

@Component({
  selector: 'app-pizzas',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PaginationComponent],
  templateUrl: './pizzas.html',
  styleUrls: ['./pizzas.css']
})
export class Pizzas implements OnInit, OnDestroy {
  todasLasPizzas: PizzaDTO[] = [];
  pizzas: PizzaDTO[] = [];
  pizzasPaginadas: PizzaDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 8;
  pageSizeOptions: number[] = [8, 16, 32];
  categorias: CategoryDTO[] = [];
  todosIngredientes: IngredientDTO[] = [];
  pizzaForm: FormGroup;
  editMode = false;
  selectedPizzaId?: number;
  showForm = false;
  loading = false;
  guardando = false;
  subiendoImagen = false;
  imagenPreview: string | null = null;
  loadError = '';
  error = '';
  success = '';
  filtroActivo: 'todas' | 'disponibles' | 'populares' | 'stock-bajo' = 'todas';
  terminoBusqueda = '';
  categoriaFiltro = 'TODAS';

  // KPIs precalculados
  kpiTotalPizzas = 0;
  kpiDisponibles = 0;
  kpiAgotadas = 0;
  kpiPopulares = 0;

  // Editor de Receta (en formulario y en modal rápido)
  recetaFormulario: PizzaIngredientDTO[] = [];
  nuevoIngredienteId: number | null = null;
  nuevaCantidadReceta: number = 80;
  costoRecetaForm = 0;
  margenRecetaForm = 0;
  stockProducibleForm = 0;

  mostrarModalReceta = false;
  pizzaRecetaModal?: PizzaDTO;
  recetaModalItems: PizzaIngredientDTO[] = [];
  guardandoReceta = false;
  costoRecetaModal = 0;
  margenRecetaModal = 0;
  stockProducibleModal = 0;

  // Simulador de Insumos y Costos por Tamaño
  tamanoSimuladorForm: SizeDTO | null = null;
  tamanoSimuladorModal: SizeDTO | null = null;

  // Variables para Decorator Pattern (extras)
  pizzaSeleccionadaExtras?: PizzaDTO;
  extrasSeleccionados: string[] = [];
  precioConExtras: number = 0;
  descripcionConExtras: string = '';
  mostrarExtras: boolean = false;
  ingredientesDisponibles: IngredientDTO[] = [];
  tamanosDisponibles: SizeDTO[] = [];
  tamanoSeleccionado?: SizeDTO;

  private priceSub?: Subscription;

  private readonly pizzaService = inject(PizzaService);
  private readonly pizzaPatronesService = inject(PizzaPatronesService);
  private readonly ingredientService = inject(IngredientService);
  private readonly sizeService = inject(Size);
  private readonly formBuilder = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly router = inject(Router);
  private readonly imageOptimizer = inject(ImageOptimizerService);
  private readonly fileUploadService = inject(FileUploadService);

  constructor() {
    this.pizzaForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      imageUrl: ['', Validators.maxLength(500)],
      price: ['', [Validators.required, Validators.min(0.01)]],
      categoryId: ['', Validators.required],
      isAvailable: [true],
      isPopular: [false]
    });

    this.priceSub = this.pizzaForm.get('price')?.valueChanges.subscribe(() => {
      this.recalcularMetricasRecetaForm();
    });
  }

  ngOnInit(): void {
    this.cargarPizzas();
    this.cargarCategorias();
    this.cargarTodosIngredientes();
    this.cargarTamanosDisponibles();
  }

  ngOnDestroy(): void {
    this.priceSub?.unsubscribe();
  }

  recargar(): void {
    this.cargarPizzas();
    this.cargarCategorias();
    this.cargarTodosIngredientes();
    this.cargarTamanosDisponibles();
  }

  reintentar(): void {
    this.recargar();
  }

  abrirNuevo(): void {
    this.abrirFormulario();
  }

  cargarTodosIngredientes(): void {
    this.ingredientService.obtenerTodos().subscribe({
      next: ingredientes => {
        this.todosIngredientes = ingredientes.toSorted((a, b) => a.name.localeCompare(b.name));
        this.recalcularMetricasRecetaForm();
        this.recalcularMetricasRecetaModal();
      },
      error: err => console.error('Error al cargar catálogo de ingredientes:', err)
    });
  }

  cargarPizzas(): void {
    this.loading = true;
    this.loadError = '';

    this.pizzaService.listarPizzas(true).subscribe({
      next: pizzas => {
        this.todasLasPizzas = pizzas || [];
        this.recalcularKpis();
        this.filtrarLocalmente();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: error => {
        console.error('Error al cargar pizzas:', error);
        this.loadError = this.obtenerMensajeErrorCarga(
          error,
          'No se pudo cargar el catálogo de pizzas. Intente nuevamente.'
        );
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private recalcularKpis(): void {
    let disponibles = 0;
    let agotadas = 0;
    let populares = 0;

    for (const p of this.todasLasPizzas) {
      if (p.isAvailable && !p.outOfStock) {
        disponibles++;
      } else {
        agotadas++;
      }
      if (p.isPopular) {
        populares++;
      }
    }

    this.kpiTotalPizzas = this.todasLasPizzas.length;
    this.kpiDisponibles = disponibles;
    this.kpiAgotadas = agotadas;
    this.kpiPopulares = populares;
  }

  aplicarFiltro(filtro: 'todas' | 'disponibles' | 'populares' | 'stock-bajo'): void {
    this.filtroActivo = filtro;
    this.currentPage = 1;
    this.limpiarMensajes();
    this.filtrarLocalmente();
  }

  onFiltroBusquedaChange(): void {
    this.currentPage = 1;
    this.filtrarLocalmente();
  }

  limpiarFiltros(): void {
    this.terminoBusqueda = '';
    this.categoriaFiltro = 'TODAS';
    this.filtroActivo = 'todas';
    this.currentPage = 1;
    this.filtrarLocalmente();
  }

  private filtrarLocalmente(): void {
    let resultado = [...this.todasLasPizzas];

    switch (this.filtroActivo) {
      case 'disponibles':
        resultado = resultado.filter(p => p.isAvailable && !p.outOfStock);
        break;
      case 'populares':
        resultado = resultado.filter(p => p.isPopular);
        break;
      case 'stock-bajo':
        resultado = resultado.filter(p => p.lowStock || p.outOfStock || !p.isAvailable);
        break;
      case 'todas':
      default:
        break;
    }

    if (this.categoriaFiltro !== 'TODAS') {
      const catId = Number(this.categoriaFiltro);
      const catObj = this.categorias.find(c => c.id === catId);
      resultado = resultado.filter(
        p => p.categoryId === catId || (!!catObj && p.categoryName === catObj.name)
      );
    }

    const q = this.terminoBusqueda.trim().toLowerCase();
    if (q) {
      resultado = resultado.filter(
        p =>
          p.name?.toLowerCase().includes(q) ||
          p.description?.toLowerCase().includes(q) ||
          p.categoryName?.toLowerCase().includes(q)
      );
    }

    this.pizzas = resultado;
    this.aplicarPaginacion();
  }

  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.pizzas.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.pizzasPaginadas = this.pizzas.slice(startIndex, endIndex);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  onItemsPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.aplicarPaginacion();
  }

  private recargarSinSpinner(): void {
    this.pizzaService.listarPizzas(true).subscribe({
      next: pizzas => {
        this.todasLasPizzas = pizzas || [];
        this.loadError = '';
        this.recalcularKpis();
        this.filtrarLocalmente();
        this.cdr.detectChanges();
      },
      error: error => console.error('Error al recargar pizzas:', error)
    });
  }

  /**
   * Subida de archivo de imagen al backend (/api/uploads/pizzas)
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const file = input.files[0];
    if (!file.type.startsWith('image/')) {
      this.mostrarMensajeError('Selecciona un archivo de imagen válido (JPG, PNG, WEBP)');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      this.imagenPreview = reader.result as string;
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);

    this.subiendoImagen = true;
    this.limpiarMensajes();

    this.fileUploadService.uploadPizzaImage(file).subscribe({
      next: res => {
        this.pizzaForm.patchValue({ imageUrl: res.imageUrl });
        this.imagenPreview = this.imageOptimizer.resolveImageUrl(res.imageUrl, false);
        this.subiendoImagen = false;
        this.mostrarMensajeExito('Imagen cargada y almacenada en el servidor');
        this.cdr.detectChanges();
      },
      error: err => {
        console.error('Error al subir imagen de pizza:', err);
        this.subiendoImagen = false;
        this.mostrarMensajeError(err.error?.message || 'Error al subir la imagen al servidor');
        this.cdr.detectChanges();
      }
    });
  }

  quitarImagenSeleccionada(): void {
    this.pizzaForm.patchValue({ imageUrl: '' });
    this.imagenPreview = null;
  }

  // ==================== GESTIÓN DE RECETA ====================

  /**
   * Genera una descripción comercial atractiva para el menú público (ej: http://localhost:4200/menu)
   * a partir de los insumos configurados en la receta base actual.
   * El usuario puede editar libremente el texto generado antes de guardar la pizza.
   */
  generarDescripcionDesdeReceta(): void {
    if (!this.recetaFormulario || this.recetaFormulario.length === 0) {
      this.mostrarMensajeError('Agrega al menos un insumo a la receta base para generar la descripción automáticamente.');
      return;
    }

    const nombres: string[] = [];
    for (const item of this.recetaFormulario) {
      let nombre = item.ingredientName?.trim();
      if (!nombre) {
        const ing = this.todosIngredientes.find(i => i.id === item.ingredientId);
        nombre = ing?.name?.trim();
      }
      if (nombre && !nombres.some(n => n.toLowerCase() === nombre!.toLowerCase())) {
        nombres.push(nombre);
      }
    }

    if (nombres.length === 0) {
      this.mostrarMensajeError('No se encontraron ingredientes válidos en la receta.');
      return;
    }

    const esMasa = (n: string) => n.toLowerCase().includes('masa');
    const ingredientesComida = nombres.filter(n => !esMasa(n)).map(n => n.toLowerCase());
    const tieneMasa = nombres.some(esMasa);

    let listaTexto = '';
    if (ingredientesComida.length === 0) {
      listaTexto = 'masa artesanal fresca';
    } else if (ingredientesComida.length === 1) {
      listaTexto = ingredientesComida[0];
    } else if (ingredientesComida.length === 2) {
      listaTexto = `${ingredientesComida[0]} y ${ingredientesComida[1]}`;
    } else {
      const ult = ingredientesComida[ingredientesComida.length - 1];
      const resto = ingredientesComida.slice(0, -1).join(', ');
      listaTexto = `${resto} y ${ult}`;
    }

    let descripcionGenerada = `Deliciosa pizza preparada con ${listaTexto}`;
    if (tieneMasa && ingredientesComida.length > 0) {
      descripcionGenerada += ` sobre masa fresca tradicional.`;
    } else {
      descripcionGenerada += `.`;
    }

    this.pizzaForm.patchValue({ description: descripcionGenerada });
    this.pizzaForm.get('description')?.markAsDirty();
    this.pizzaForm.get('description')?.markAsTouched();
    this.mostrarMensajeExito('Descripción generada desde la receta. Puedes editarla antes de guardar.');
    this.cdr.detectChanges();
  }

  private recalcularMetricasRecetaForm(): void {
    const price = Number(this.pizzaForm?.get('price')?.value) || 0;
    this.costoRecetaForm = this.calcularCostoReceta(this.recetaFormulario);
    this.margenRecetaForm = price > 0 ? ((price - this.costoRecetaForm) / price) * 100 : 0;
    const personalSize = this.tamanosDisponibles.find(t => (t.recipeMultiplier || 1) === 1.0);
    this.stockProducibleForm = this.calcularStockProducibleReceta(this.recetaFormulario, 1.0, personalSize?.stockQuantity);
  }

  private recalcularMetricasRecetaModal(): void {
    const price = Number(this.pizzaRecetaModal?.price) || 0;
    this.costoRecetaModal = this.calcularCostoReceta(this.recetaModalItems);
    this.margenRecetaModal = price > 0 ? ((price - this.costoRecetaModal) / price) * 100 : 0;
    const personalSize = this.tamanosDisponibles.find(t => (t.recipeMultiplier || 1) === 1.0);
    this.stockProducibleModal = this.calcularStockProducibleReceta(this.recetaModalItems, 1.0, personalSize?.stockQuantity);
  }

  // ==================== SIMULADOR DE TAMAÑOS EN FORMULARIO ====================

  seleccionarTamanoSimuladorForm(tamano: SizeDTO | null): void {
    this.tamanoSimuladorForm = tamano;
  }

  getMultiplicadorForm(): number {
    return this.tamanoSimuladorForm?.recipeMultiplier || 1.0;
  }

  getPrecioSimuladoForm(): number {
    const basePrice = Number(this.pizzaForm?.get('price')?.value) || 0;
    const extraCost = this.tamanoSimuladorForm?.extraCost || 0;
    return basePrice + extraCost;
  }

  getCostoSimuladoForm(): number {
    return Number((this.costoRecetaForm * this.getMultiplicadorForm()).toFixed(2));
  }

  getMargenSimuladoForm(): number {
    const precio = this.getPrecioSimuladoForm();
    if (precio <= 0) return 0;
    const costo = this.getCostoSimuladoForm();
    return ((precio - costo) / precio) * 100;
  }

  getStockProducibleSimuladoForm(): number {
    const mult = this.getMultiplicadorForm();
    const masaStock = this.getStockMasaForm();
    return this.calcularStockProducibleReceta(this.recetaFormulario, mult, masaStock ?? undefined);
  }

  getStockMasaForm(): number | null {
    if (this.tamanoSimuladorForm && this.tamanoSimuladorForm.stockQuantity !== undefined) {
      return this.tamanoSimuladorForm.stockQuantity;
    }
    const personal = this.tamanosDisponibles.find(t => (t.recipeMultiplier || 1) === 1.0);
    return personal?.stockQuantity ?? null;
  }

  // ==================== SIMULADOR DE TAMAÑOS EN MODAL RÁPIDO ====================

  seleccionarTamanoSimuladorModal(tamano: SizeDTO | null): void {
    this.tamanoSimuladorModal = tamano;
  }

  getMultiplicadorModal(): number {
    return this.tamanoSimuladorModal?.recipeMultiplier || 1.0;
  }

  getPrecioSimuladoModal(): number {
    const basePrice = Number(this.pizzaRecetaModal?.price) || 0;
    const extraCost = this.tamanoSimuladorModal?.extraCost || 0;
    return basePrice + extraCost;
  }

  getCostoSimuladoModal(): number {
    return Number((this.costoRecetaModal * this.getMultiplicadorModal()).toFixed(2));
  }

  getMargenSimuladoModal(): number {
    const precio = this.getPrecioSimuladoModal();
    if (precio <= 0) return 0;
    const costo = this.getCostoSimuladoModal();
    return ((precio - costo) / precio) * 100;
  }

  getStockProducibleSimuladoModal(): number {
    const mult = this.getMultiplicadorModal();
    const masaStock = this.getStockMasaModal();
    return this.calcularStockProducibleReceta(this.recetaModalItems, mult, masaStock ?? undefined);
  }

  getStockMasaModal(): number | null {
    if (this.tamanoSimuladorModal && this.tamanoSimuladorModal.stockQuantity !== undefined) {
      return this.tamanoSimuladorModal.stockQuantity;
    }
    const personal = this.tamanosDisponibles.find(t => (t.recipeMultiplier || 1) === 1.0);
    return personal?.stockQuantity ?? null;
  }

  getCantidadEscalada(baseQty: number | string | undefined, mult: number): number {
    return Number(((Number(baseQty) || 0) * mult).toFixed(1));
  }

  getCostoEscalado(baseLineCost: number | string | undefined, mult: number): number {
    return Number(((Number(baseLineCost) || 0) * mult).toFixed(2));
  }

  agregarIngredienteAReceta(targetList: PizzaIngredientDTO[]): void {
    if (!this.nuevoIngredienteId || this.nuevaCantidadReceta <= 0) {
      return;
    }
    const ingId = Number(this.nuevoIngredienteId);
    const ing = this.todosIngredientes.find(i => i.id === ingId);
    if (!ing) return;

    const existente = targetList.find(r => r.ingredientId === ingId);
    if (existente) {
      existente.quantity = Number((Number(existente.quantity) + Number(this.nuevaCantidadReceta)).toFixed(2));
      existente.lineCost = Number((existente.quantity * (ing.unitCost || 0)).toFixed(2));
    } else {
      targetList.push({
        ingredientId: ingId,
        ingredientName: ing.name,
        unit: ing.unit || 'g',
        unitCost: ing.unitCost || 0,
        stockQuantity: ing.stockQuantity || 0,
        isDefault: true,
        quantity: Number(this.nuevaCantidadReceta),
        lineCost: Number((Number(this.nuevaCantidadReceta) * (ing.unitCost || 0)).toFixed(2))
      });
    }

    this.nuevoIngredienteId = null;
    this.nuevaCantidadReceta = 80;
    if (targetList === this.recetaFormulario) {
      this.recalcularMetricasRecetaForm();
    } else {
      this.recalcularMetricasRecetaModal();
    }
  }

  actualizarCantidadItemReceta(item: PizzaIngredientDTO, isModal = false): void {
    const qty = Number(item.quantity) || 0;
    item.lineCost = Number((qty * (item.unitCost || 0)).toFixed(2));
    if (isModal) {
      this.recalcularMetricasRecetaModal();
    } else {
      this.recalcularMetricasRecetaForm();
    }
  }

  quitarIngredienteDeReceta(targetList: PizzaIngredientDTO[], index: number): void {
    targetList.splice(index, 1);
    if (targetList === this.recetaFormulario) {
      this.recalcularMetricasRecetaForm();
    } else {
      this.recalcularMetricasRecetaModal();
    }
  }

  calcularCostoReceta(items: PizzaIngredientDTO[]): number {
    return items.reduce((sum, item) => sum + (Number(item.quantity) || 0) * (item.unitCost || 0), 0);
  }

  calcularMargenReceta(items: PizzaIngredientDTO[], precioVenta: number): number {
    if (!precioVenta || precioVenta <= 0) return 0;
    const costo = this.calcularCostoReceta(items);
    return ((precioVenta - costo) / precioVenta) * 100;
  }

  calcularStockProducibleReceta(items: PizzaIngredientDTO[], multiplicador = 1.0, masaStock?: number): number {
    if (!items || items.length === 0) return 0;
    let minUnits = Number.MAX_SAFE_INTEGER;
    for (const item of items) {
      const baseQty = Number(item.quantity) || 0;
      const qtyRequerida = baseQty * multiplicador;
      if (qtyRequerida <= 0) return 0;
      const ing = this.todosIngredientes.find(i => i.id === item.ingredientId);
      const stock = ing ? (ing.stockQuantity ?? 0) : (item.stockQuantity ?? 0);
      minUnits = Math.min(minUnits, Math.max(0, Math.floor(stock / qtyRequerida)));
    }
    let resultado = minUnits === Number.MAX_SAFE_INTEGER ? 0 : minUnits;
    if (masaStock !== undefined && masaStock !== null) {
      resultado = Math.min(resultado, masaStock);
    }
    return resultado;
  }

  abrirModalRecetaRapida(pizza: PizzaDTO): void {
    this.pizzaRecetaModal = pizza;
    this.tamanoSimuladorModal = null;
    this.recetaModalItems = (pizza.recipe || []).map(r => ({
      ...r,
      lineCost: Number(((Number(r.quantity) || 0) * (r.unitCost || 0)).toFixed(2))
    }));
    this.nuevoIngredienteId = null;
    this.nuevaCantidadReceta = 80;
    this.recalcularMetricasRecetaModal();
    this.mostrarModalReceta = true;
  }

  cerrarModalRecetaRapida(): void {
    this.mostrarModalReceta = false;
    this.pizzaRecetaModal = undefined;
    this.recetaModalItems = [];
    this.tamanoSimuladorModal = null;
  }

  guardarRecetaRapida(): void {
    if (!this.pizzaRecetaModal?.id || this.guardandoReceta) return;
    this.guardandoReceta = true;

    this.pizzaService.actualizarRecetaPizza(this.pizzaRecetaModal.id, this.recetaModalItems).subscribe({
      next: () => {
        this.guardandoReceta = false;
        this.mostrarMensajeExito(`Receta de "${this.pizzaRecetaModal?.name}" actualizada correctamente`);
        this.cerrarModalRecetaRapida();
        this.recargarSinSpinner();
      },
      error: err => {
        this.guardandoReceta = false;
        this.mostrarMensajeError(err.error?.message || 'Error al guardar la receta');
      }
    });
  }

  // ==================== DECORATOR PATTERN (EXTRAS SIMULADOR) ====================

  mostrarOpcionesExtras(pizza: PizzaDTO): void {
    this.pizzaSeleccionadaExtras = pizza;
    this.extrasSeleccionados = [];
    this.precioConExtras = pizza.price;
    this.descripcionConExtras = pizza.name;
    this.mostrarExtras = true;
    this.tamanoSeleccionado = undefined;

    if (this.ingredientesDisponibles.length === 0) {
      this.cargarIngredientesDisponibles();
    }
    if (this.tamanosDisponibles.length === 0) {
      this.cargarTamanosDisponibles();
    }
  }

  cargarIngredientesDisponibles(): void {
    this.ingredientService.obtenerDisponibles().subscribe({
      next: ingredientes => {
        this.ingredientesDisponibles = ingredientes;
      },
      error: () => {
        this.ingredientesDisponibles = [];
      }
    });
  }

  cargarTamanosDisponibles(): void {
    this.sizeService.obtenerTodos().subscribe({
      next: tamanos => {
        this.tamanosDisponibles = tamanos.toSorted((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.recalcularMetricasRecetaForm();
        this.recalcularMetricasRecetaModal();
        this.cdr.detectChanges();
      },
      error: () => {
        this.tamanosDisponibles = [];
      }
    });
  }

  seleccionarTamano(tamano: SizeDTO): void {
    this.tamanoSeleccionado = tamano;
    this.calcularPrecioConExtras();
  }

  toggleExtra(extra: string): void {
    const index = this.extrasSeleccionados.indexOf(extra);
    if (index > -1) {
      this.extrasSeleccionados.splice(index, 1);
    } else {
      this.extrasSeleccionados.push(extra);
    }
    this.calcularPrecioConExtras();
  }

  calcularPrecioConExtras(): void {
    if (!this.pizzaSeleccionadaExtras) return;

    const basePrice = this.pizzaSeleccionadaExtras.price || 0;
    const sizeCost = this.tamanoSeleccionado?.extraCost || 0;
    const extrasCost = this.extrasSeleccionados.reduce((acc, name) => {
      const ing = this.ingredientesDisponibles.find(i => i.name === name);
      return acc + (ing?.extraCost || 0);
    }, 0);

    let desc = this.pizzaSeleccionadaExtras.name;
    if (this.tamanoSeleccionado) {
      desc += ` - Tamaño: ${this.tamanoSeleccionado.name}`;
    }
    if (this.extrasSeleccionados.length > 0) {
      desc += ` + ${this.extrasSeleccionados.join(', ')}`;
    }

    this.precioConExtras = basePrice + sizeCost + extrasCost;
    this.descripcionConExtras = desc;
  }

  cerrarExtras(): void {
    this.mostrarExtras = false;
    this.pizzaSeleccionadaExtras = undefined;
    this.extrasSeleccionados = [];
  }

  cargarCategorias(): void {
    this.pizzaService.listarCategorias().subscribe({
      next: categorias => {
        this.categorias = categorias || [];
      },
      error: error => {
        console.error('Error al cargar categorías:', error);
      }
    });
  }

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    this.tamanoSimuladorForm = null;
    this.pizzaForm.reset();
    this.pizzaForm.patchValue({ isAvailable: true, isPopular: false, imageUrl: '' });
    this.imagenPreview = null;
    this.selectedPizzaId = undefined;
    this.recetaFormulario = [];

    // Pre-cargar Masa Fresca si existe en el catálogo para facilitar nueva receta
    const masa = this.todosIngredientes.find(i => i.name.toLowerCase().includes('masa'));
    if (masa?.id) {
      this.recetaFormulario.push({
        ingredientId: masa.id,
        ingredientName: masa.name,
        unit: masa.unit || 'g',
        unitCost: masa.unitCost || 0,
        stockQuantity: masa.stockQuantity || 0,
        isDefault: true,
        quantity: 220,
        lineCost: Number((220 * (masa.unitCost || 0)).toFixed(2))
      });
    }

    this.recalcularMetricasRecetaForm();
    this.limpiarMensajes();
  }

  editarPizza(pizza: PizzaDTO): void {
    this.showForm = true;
    this.editMode = true;
    this.tamanoSimuladorForm = null;
    this.selectedPizzaId = pizza.id;

    const categoria = this.categorias.find(cat => cat.id === pizza.categoryId || cat.name === pizza.categoryName);

    this.pizzaForm.patchValue({
      name: pizza.name,
      description: pizza.description,
      imageUrl: pizza.imageUrl || '',
      price: pizza.price,
      categoryId: pizza.categoryId || categoria?.id || '',
      isAvailable: pizza.isAvailable,
      isPopular: pizza.isPopular
    });

    this.imagenPreview = pizza.imageUrl ? this.imageOptimizer.resolveImageUrl(pizza.imageUrl, false) : null;
    this.recetaFormulario = (pizza.recipe || []).map(r => ({
      ...r,
      lineCost: Number(((Number(r.quantity) || 0) * (r.unitCost || 0)).toFixed(2))
    }));
    this.recalcularMetricasRecetaForm();
    this.limpiarMensajes();
  }

  cancelar(): void {
    this.showForm = false;
    this.editMode = false;
    this.tamanoSimuladorForm = null;
    this.selectedPizzaId = undefined;
    this.imagenPreview = null;
    this.recetaFormulario = [];
    this.pizzaForm.reset();
    this.limpiarMensajes();
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-admin']);
  }

  guardarPizza(): void {
    if (this.guardando) return;

    if (this.pizzaForm.valid) {
      this.guardando = true;
      const formValue = this.pizzaForm.value;

      const pizzaData: PizzaCreateDTO = {
        name: formValue.name,
        description: formValue.description,
        imageUrl: formValue.imageUrl,
        price: Number.parseFloat(formValue.price),
        isAvailable: formValue.isAvailable,
        isPopular: formValue.isPopular,
        categoryId: Number.parseInt(formValue.categoryId, 10),
        recipe: this.recetaFormulario
      };

      if (this.editMode && this.selectedPizzaId) {
        this.pizzaService.actualizarPizza(this.selectedPizzaId, pizzaData).subscribe({
          next: () => {
            this.guardando = false;
            this.cancelar();
            this.mostrarMensajeExito('Pizza y receta actualizadas correctamente');
            this.recargarSinSpinner();
          },
          error: error => {
            console.error('Error al actualizar pizza:', error);
            this.mostrarMensajeError(error.error?.message || 'Error al actualizar la pizza');
            this.guardando = false;
          }
        });
      } else {
        this.pizzaService.crearPizza(pizzaData).subscribe({
          next: () => {
            this.guardando = false;
            this.cancelar();
            this.mostrarMensajeExito('Pizza creada correctamente');
            this.recargarSinSpinner();
          },
          error: error => {
            console.error('Error al crear pizza:', error);
            this.mostrarMensajeError(error.error?.message || 'Error al crear la pizza');
            this.guardando = false;
          }
        });
      }
    } else {
      this.pizzaForm.markAllAsTouched();
      this.mostrarMensajeError('Por favor, complete todos los campos requeridos correctamente');
    }
  }


  restaurarPizza(id: number, nombre: string): void {
    if (!confirm(`¿Desea restaurar la pizza "${nombre}"?`)) {
      return;
    }
    this.pizzaService.restaurarPizza(id).subscribe({
      next: () => {
        this.mostrarMensajeExito(`Pizza "${nombre}" restaurada con éxito`);
        this.recargarSinSpinner();
      },
      error: (error) => {
        this.mostrarMensajeError(error.error?.message || 'Error al restaurar la pizza');
      }
    });
  }

  eliminarPizza(id: number, nombre: string): void {
    if (confirm(`¿Está seguro de que desea eliminar la pizza "${nombre}"?`)) {
      this.pizzaService.eliminarPizza(id).subscribe({
        next: () => {
          this.mostrarMensajeExito('Pizza eliminada correctamente');
          this.recargarSinSpinner();
          this.cdr.detectChanges();
        },
        error: error => {
          console.error('Error al eliminar pizza:', error);
          this.mostrarMensajeError(error.error?.message || 'Error al eliminar la pizza');
        }
      });
    }
  }

  limpiarMensajes(): void {
    this.error = '';
    this.success = '';
  }

  private obtenerMensajeErrorCarga(err: any, fallback: string): string {
    if (err?.status === 0) {
      return 'No se pudo conectar con el servidor (Backend sin conexión). Verifique que el servicio esté activo e intente nuevamente.';
    }
    return err?.error?.message || fallback;
  }

  private mostrarMensajeExito(mensaje: string): void {
    this.success = mensaje;
    this.error = '';
    setTimeout(() => {
      this.success = '';
    }, 3500);
  }

  private mostrarMensajeError(mensaje: string): void {
    this.error = mensaje;
    this.success = '';
  }

  obtenerNombreCategoria(pizza: PizzaDTO): string {
    return pizza.categoryName || 'Sin categoría';
  }

  formatearPrecio(precio: number | undefined | null): string {
    if (precio === undefined || precio === null || Number.isNaN(precio)) {
      return 'S/ 0.00';
    }
    return `S/ ${Number(precio).toFixed(2)}`;
  }

  trackByPizzaId(index: number, pizza: PizzaDTO): number {
    return pizza.id || index;
  }

  trackByCategoriaId(index: number, cat: CategoryDTO): number {
    return cat.id || index;
  }

  trackByIngredienteId(index: number, ing: IngredientDTO): number {
    return ing.id || index;
  }

  trackByRecetaItem(index: number, item: PizzaIngredientDTO): number {
    return item.ingredientId || index;
  }

  trackBySizeId(index: number, size: SizeDTO): number {
    return size.id || index;
  }

  onImageError(pizza: PizzaDTO): void {
    pizza.imageError = true;
  }

  getOptimizedImageUrl(imageUrl: string | undefined): string {
    return this.imageOptimizer.optimizeImageUrl(imageUrl, 'low');
  }
}
