import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

type AdminGroupKey = 'operaciones' | 'catalogo' | 'finanzas' | 'comunidad';

const STORAGE_GROUPS_KEY = 'adminSidebarGroups';

const VISTA_TO_GROUP: Record<string, AdminGroupKey> = {
  orders: 'operaciones',
  payments: 'operaciones',
  'metodos-pago': 'operaciones',
  delivery: 'operaciones',
  pizzas: 'catalogo',
  categorias: 'catalogo',
  sizes: 'catalogo',
  ingredientes: 'catalogo',
  extras: 'catalogo',
  promociones: 'catalogo',
  finanzas: 'finanzas',
  inventario: 'finanzas',
  'costos-produccion': 'finanzas',
  perdidas: 'finanzas',
  usuarios: 'comunidad',
  reviews: 'comunidad',
};

@Component({
  selector: 'app-admin-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-sidebar.html',
  styleUrls: ['./admin-sidebar.css'],
})
export class AdminSidebarComponent implements OnInit, OnChanges {
  @Input() vistaActual: string = 'dashboard';
  @Input() sidebarCollapsed: boolean = false;
  @Input() alertasStockTotal: number = 0;
  @Input() deliveryFeeActual: number = 5.0;
  @Input() currentUser: any = null;
  @Input() currentUserInitial: string = 'A';

  @Output() vistaChange = new EventEmitter<string>();
  @Output() toggleSidebar = new EventEmitter<void>();
  @Output() logoutRequest = new EventEmitter<void>();

  gruposAbiertos: Record<AdminGroupKey, boolean> = {
    operaciones: true,
    catalogo: false,
    finanzas: false,
    comunidad: false,
  };

  ngOnInit(): void {
    this.cargarEstadoGrupos();
    this.asegurarGrupoActivoAbierto();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['vistaActual']) {
      this.asegurarGrupoActivoAbierto();
    }
  }

  toggleGrupo(grupo: AdminGroupKey): void {
    this.gruposAbiertos[grupo] = !this.gruposAbiertos[grupo];
    this.guardarEstadoGrupos();
  }

  esGrupoConVistaActiva(grupo: AdminGroupKey): boolean {
    return VISTA_TO_GROUP[this.vistaActual] === grupo;
  }

  onCambiarVista(vista: string): void {
    this.vistaChange.emit(vista);
  }

  onToggleSidebar(): void {
    this.toggleSidebar.emit();
  }

  onCerrarSesion(): void {
    this.logoutRequest.emit();
  }

  private asegurarGrupoActivoAbierto(): void {
    const grupo = VISTA_TO_GROUP[this.vistaActual];
    if (grupo && !this.gruposAbiertos[grupo]) {
      this.gruposAbiertos[grupo] = true;
      this.guardarEstadoGrupos();
    }
  }

  private cargarEstadoGrupos(): void {
    if (typeof window === 'undefined') return;
    try {
      const raw = sessionStorage.getItem(STORAGE_GROUPS_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object') {
        this.gruposAbiertos = {
          operaciones: Boolean(parsed.operaciones ?? true),
          catalogo: Boolean(parsed.catalogo ?? false),
          finanzas: Boolean(parsed.finanzas ?? false),
          comunidad: Boolean(parsed.comunidad ?? false),
        };
      }
    } catch {}
  }

  private guardarEstadoGrupos(): void {
    if (typeof window === 'undefined') return;
    try {
      sessionStorage.setItem(STORAGE_GROUPS_KEY, JSON.stringify(this.gruposAbiertos));
    } catch {}
  }
}
