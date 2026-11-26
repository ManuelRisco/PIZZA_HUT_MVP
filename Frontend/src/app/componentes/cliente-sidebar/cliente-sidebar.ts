import { Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

type ClienteGroupKey = 'actividad' | 'cuenta';

const STORAGE_CLIENTE_GROUPS_KEY = 'clienteSidebarGroups';

const CLIENTE_VISTA_TO_GROUP: Record<string, ClienteGroupKey> = {
  pedidos: 'actividad',
  favoritos: 'actividad',
  resenas: 'actividad',
  perfil: 'cuenta',
};

@Component({
  selector: 'app-cliente-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './cliente-sidebar.html',
  styleUrls: ['./cliente-sidebar.css'],
})
export class ClienteSidebarComponent implements OnInit, OnChanges {
  @Input() vistaActual: string = 'dashboard';
  @Input() sidebarCollapsed: boolean = false;
  @Input() pedidosActivos: number = 0;
  @Input() totalFavoritos: number = 0;
  @Input() currentUser: any = null;
  @Input() inicialUsuario: string = 'U';

  @Output() vistaChange = new EventEmitter<string>();
  @Output() toggleSidebar = new EventEmitter<void>();
  @Output() logoutRequest = new EventEmitter<void>();

  gruposAbiertos: Record<ClienteGroupKey, boolean> = {
    actividad: true,
    cuenta: true,
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

  toggleGrupo(grupo: ClienteGroupKey): void {
    this.gruposAbiertos[grupo] = !this.gruposAbiertos[grupo];
    this.guardarEstadoGrupos();
  }

  esGrupoConVistaActiva(grupo: ClienteGroupKey): boolean {
    return CLIENTE_VISTA_TO_GROUP[this.vistaActual] === grupo;
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
    const grupo = CLIENTE_VISTA_TO_GROUP[this.vistaActual];
    if (grupo && !this.gruposAbiertos[grupo]) {
      this.gruposAbiertos[grupo] = true;
      this.guardarEstadoGrupos();
    }
  }

  private cargarEstadoGrupos(): void {
    if (typeof window === 'undefined') return;
    try {
      const raw = sessionStorage.getItem(STORAGE_CLIENTE_GROUPS_KEY);
      if (!raw) return;
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object') {
        this.gruposAbiertos = {
          actividad: Boolean(parsed.actividad ?? true),
          cuenta: Boolean(parsed.cuenta ?? true),
        };
      }
    } catch {}
  }

  private guardarEstadoGrupos(): void {
    if (typeof window === 'undefined') return;
    try {
      sessionStorage.setItem(STORAGE_CLIENTE_GROUPS_KEY, JSON.stringify(this.gruposAbiertos));
    } catch {}
  }
}
