import { Component, OnInit, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidatorFn } from '@angular/forms';
import { HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioPatronesService } from '../../services/usuario-patrones.service';
import { AuthService } from '../../services/auth.service';
import { UsuarioDTO, UsuarioCreateDTO } from '../../models/usuario.interface';
import { Subscription } from 'rxjs';
import { PaginationComponent } from '../pagination/pagination';
import { SessionLog } from '../../models/session-log.model';
import { AuditLog } from '../../models/audit-log.model';
import { SessionLogService } from '../../services/session-log.service';
import { AuditLogService } from '../../services/audit-log.service';

export type FiltroUsuario = 'todos' | 'admins' | 'clientes' | 'activos' | 'inactivos' | 'admins-activos';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    HttpClientModule,
    PaginationComponent
  ],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class Usuarios implements OnInit, OnDestroy {
  todosLosUsuarios: UsuarioDTO[] = [];
  usuarios: UsuarioDTO[] = [];
  usuariosPaginados: UsuarioDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;

  // Búsqueda y filtros locales (0ms)
  busqueda: string = '';
  filtroActivo: FiltroUsuario = 'todos';

  // KPIs precomputados
  kpiTotal: number = 0;
  kpiClientes: number = 0;
  kpiAdmins: number = 0;
  kpiActivos: number = 0;

  usuarioForm: FormGroup;
  editMode = false;
  selectedUsuarioId?: number;
  showForm = false;
  loading: boolean = true;
  guardando: boolean = false;
  procesandoId: number | null = null;
  loadError: string = '';
  error = '';
  success = '';

  // Variables para mostrar/ocultar contraseñas
  showPassword = false;
  showConfirmPassword = false;

  // Variables para la modal de creación/edición
  mostrarModal: boolean = false;

  // Variables para Logs del Sistema
  showLogsModal: boolean = false;
  activeLogTab: 'sesiones' | 'auditoria' = 'sesiones';
  sesionesActivas: SessionLog[] = [];
  sesionesPaginadas: SessionLog[] = [];
  sesionesPage: number = 1;
  sesionesPerPage: number = 10;

  auditLogs: AuditLog[] = [];
  auditLogsFiltrados: AuditLog[] = [];
  auditLogsPaginados: AuditLog[] = [];
  logsBusqueda: string = '';
  logsPage: number = 1;
  logsPerPage: number = 10;

  loadingLogs: boolean = false;
  logsError: string = '';
  cerrandoSesionToken: string | null = null;

  private subscriptions: Subscription[] = [];

  constructor(
    private readonly usuarioService: UsuarioService,
    private readonly usuarioPatronesService: UsuarioPatronesService,
    private readonly formBuilder: FormBuilder,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly sessionLogService: SessionLogService,
    private readonly auditLogService: AuditLogService
  ) {
    this.usuarioForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      name: ['', [
        Validators.required,
        Validators.minLength(4),
        Validators.maxLength(50),
        Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)
      ]],
      phone: ['', [
        Validators.pattern(/^[0-9]{9}$/)
      ]],
      role: ['CUSTOMER', Validators.required],
      password: ['', [
        Validators.required,
        Validators.minLength(6),
        Validators.maxLength(20),
        Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/)
      ]],
      confirmPassword: ['', Validators.required]
    }, { validators: this.passwordsMatchValidator });
  }

  ngOnInit(): void {
    this.cargarUsuarios();
    this.setupFormValidationListeners();
  }

  ngOnDestroy(): void {
    // Limpiar todas las suscripciones
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  setupFormValidationListeners(): void {
    // Limpiar suscripciones anteriores
    this.subscriptions.forEach(sub => sub.unsubscribe());
    this.subscriptions = [];

    // Validar nombre en tiempo real
    const nameSub = this.usuarioForm.get('name')?.valueChanges.subscribe(value => {
      if (value?.trim()) {
        setTimeout(() => {
          if (this.verificarNombreDuplicado()) {
            this.usuarioForm.get('name')?.setErrors({ 'duplicado': true });
          } else {
            // Limpiar error de duplicado si ya no existe
            const currentErrors = this.usuarioForm.get('name')?.errors;
            if (currentErrors?.['duplicado']) {
              delete currentErrors['duplicado'];
              const hasOtherErrors = Object.keys(currentErrors).length > 0;
              this.usuarioForm.get('name')?.setErrors(hasOtherErrors ? currentErrors : null);
            }
          }
        }, 300); // Debounce de 300ms
      }
    });
    if (nameSub) this.subscriptions.push(nameSub);

    // Validar email en tiempo real
    const emailSub = this.usuarioForm.get('email')?.valueChanges.subscribe(value => {
      if (value?.trim()) {
        setTimeout(() => {
          if (this.verificarEmailDuplicado()) {
            this.usuarioForm.get('email')?.setErrors({ 'duplicado': true });
          } else {
            // Limpiar error de duplicado si ya no existe
            const currentErrors = this.usuarioForm.get('email')?.errors;
            if (currentErrors?.['duplicado']) {
              delete currentErrors['duplicado'];
              const hasOtherErrors = Object.keys(currentErrors).length > 0;
              this.usuarioForm.get('email')?.setErrors(hasOtherErrors ? currentErrors : null);
            }
          }
        }, 300); // Debounce de 300ms
      }
    });
    if (emailSub) this.subscriptions.push(emailSub);
  }

  // Validador personalizado para comparar contraseñas
  passwordsMatchValidator: ValidatorFn = (form: AbstractControl) => {
    const password = form.get('password')?.value;
    const confirm = form.get('confirmPassword')?.value;
    return password === confirm ? null : { passwordMismatch: true };
  };

  recargar(): void {
    this.cargarUsuarios();
  }

  reintentar(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this.loading = true;
    this.loadError = '';
    this.limpiarMensajes();

    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.todosLosUsuarios = [...(usuarios || [])];
        this.recalcularKPIs();
        this.aplicarFiltroLocal();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al cargar usuarios:', err);
        this.loadError = err?.status === 0
          ? 'No se pudo conectar con el servidor. Verifique que el backend esté en ejecución.'
          : (err?.error?.message || 'No se pudo cargar la lista de usuarios.');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private recalcularKPIs(): void {
    const lista = this.todosLosUsuarios;
    this.kpiTotal = lista.length;
    let clientes = 0;
    let admins = 0;
    let activos = 0;

    for (const u of lista) {
      if (u.role === 'ADMIN') {
        admins++;
      } else {
        clientes++;
      }
      if (this.estaActivo(u)) {
        activos++;
      }
    }

    this.kpiClientes = clientes;
    this.kpiAdmins = admins;
    this.kpiActivos = activos;
  }

  /**
   * Aplica filtro de rol/estado y búsqueda de texto localmente en 0ms sin peticiones HTTP
   */
  aplicarFiltroLocal(): void {
    let resultado = this.todosLosUsuarios;

    switch (this.filtroActivo) {
      case 'admins':
        resultado = resultado.filter(u => u.role === 'ADMIN');
        break;
      case 'clientes':
        resultado = resultado.filter(u => u.role !== 'ADMIN');
        break;
      case 'activos':
        resultado = resultado.filter(u => this.estaActivo(u));
        break;
      case 'inactivos':
        resultado = resultado.filter(u => !this.estaActivo(u));
        break;
      case 'admins-activos':
        resultado = resultado.filter(u => u.role === 'ADMIN' && this.estaActivo(u));
        break;
      default:
        break;
    }

    const term = this.busqueda.trim().toLowerCase();
    if (term) {
      resultado = resultado.filter(u =>
        (u.name || '').toLowerCase().includes(term) ||
        (u.email || '').toLowerCase().includes(term) ||
        (u.phone || '').toLowerCase().includes(term) ||
        String(u.id ?? '').includes(term)
      );
    }

    this.usuarios = resultado;
    this.aplicarPaginacion();
  }

  /**
   * Aplica el filtro seleccionado (compatibilidad local inmediata)
   */
  aplicarFiltro(): void {
    if (this.todosLosUsuarios.length === 0 && !this.loadError) {
      this.cargarUsuarios();
      return;
    }
    this.aplicarFiltroLocal();
  }

  cargarTodosLosUsuarios(): void {
    this.filtroActivo = 'todos';
    this.cargarUsuarios();
  }

  cargarAdministradores(): void {
    this.filtroActivo = 'admins';
    this.aplicarFiltro();
  }

  cargarUsuariosActivos(): void {
    this.filtroActivo = 'activos';
    this.aplicarFiltro();
  }

  cargarAdministradoresActivos(): void {
    this.filtroActivo = 'admins-activos';
    this.aplicarFiltro();
  }

  /**
   * Cambia el filtro activo localmente en 0ms
   */
  cambiarFiltro(filtro: FiltroUsuario): void {
    this.filtroActivo = filtro;
    this.currentPage = 1;
    this.aplicarFiltroLocal();
  }

  onBusquedaChange(): void {
    this.currentPage = 1;
    this.aplicarFiltroLocal();
  }

  limpiarFiltros(): void {
    this.busqueda = '';
    this.filtroActivo = 'todos';
    this.currentPage = 1;
    this.aplicarFiltroLocal();
  }

  /**
   * Aplica paginación a los usuarios filtrados
   */
  aplicarPaginacion(): void {
    const totalPages = Math.ceil(this.usuarios.length / this.itemsPerPage);
    if (this.currentPage > totalPages && totalPages > 0) {
      this.currentPage = totalPages;
    } else if (this.currentPage < 1) {
      this.currentPage = 1;
    }

    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.usuariosPaginados = this.usuarios.slice(startIndex, endIndex);
  }

  /**
   * Cambia de página
   */
  onPageChange(page: number): void {
    this.currentPage = page;
    this.aplicarPaginacion();
  }

  onItemsPerPageChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.aplicarPaginacion();
  }

  recargarListaSinLoading(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.todosLosUsuarios = [...(usuarios || [])];
        this.loadError = '';
        this.recalcularKPIs();
        this.aplicarFiltroLocal();
        this.loading = false;
        this.procesandoId = null;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error al recargar usuarios:', err);
        this.mostrarMensajeError('Error al sincronizar la lista de usuarios');
        this.loading = false;
        this.procesandoId = null;
        this.cdr.detectChanges();
      }
    });
  }

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
    this.showPassword = false;
    this.showConfirmPassword = false;
    this.usuarioForm.reset();
    this.usuarioForm.patchValue({ role: 'CUSTOMER' });
    this.selectedUsuarioId = undefined;

    // Asegurar que todos los campos estén habilitados
    this.usuarioForm.get('email')?.enable();
    this.usuarioForm.get('name')?.enable();
    this.usuarioForm.get('phone')?.enable();
    this.usuarioForm.get('role')?.enable();
    this.usuarioForm.get('password')?.enable();
    this.usuarioForm.get('confirmPassword')?.enable();

    this.setupFormValidators();
    this.limpiarMensajes();
  }

  editarUsuario(usuario: UsuarioDTO): void {
    this.showForm = true;
    this.editMode = true;
    this.selectedUsuarioId = usuario.id;
    this.usuarioForm.patchValue({
      email: usuario.email,
      name: usuario.name,
      phone: usuario.phone,
      role: usuario.role
    });

    // Asegurar que todos los campos estén habilitados para edición
    this.usuarioForm.get('email')?.enable();
    this.usuarioForm.get('name')?.enable();
    this.usuarioForm.get('phone')?.enable();
    this.usuarioForm.get('role')?.enable();

    // Deshabilitar el campo de rol si es el usuario actual
    if (this.esUsuarioActual(usuario.email)) {
      this.usuarioForm.get('role')?.disable();
    }

    this.setupFormValidatorsForEdit();
    this.limpiarMensajes();
  }

  cancelar(): void {
    this.showForm = false;
    this.editMode = false;
    this.guardando = false;
    this.selectedUsuarioId = undefined;
    this.usuarioForm.reset();

    // Habilitar todos los campos al cancelar
    this.usuarioForm.get('email')?.enable();
    this.usuarioForm.get('name')?.enable();
    this.usuarioForm.get('phone')?.enable();
    this.usuarioForm.get('role')?.enable();
    this.usuarioForm.get('password')?.enable();
    this.usuarioForm.get('confirmPassword')?.enable();

    this.limpiarMensajes();
  }

  volverAlPanel(): void {
    this.router.navigate(['/panel-admin']);
  }

  guardarUsuario(): void {
    if (this.guardando) return;

    const isValidForCreation = !this.editMode && this.usuarioForm.valid;

    let isValidForEdit = false;
    if (this.editMode) {
      const emailValid = this.usuarioForm.get('email')?.valid ?? false;
      const nameValid = this.usuarioForm.get('name')?.valid ?? false;
      const phoneValid = this.usuarioForm.get('phone')?.valid ?? true;
      const roleDisabled = this.usuarioForm.get('role')?.disabled ?? false;
      const roleValid = this.usuarioForm.get('role')?.valid ?? false;

      isValidForEdit = emailValid && nameValid && phoneValid && (roleDisabled || roleValid);
    }

    if (!(isValidForCreation || isValidForEdit)) {
      this.mostrarMensajeError('Por favor, complete todos los campos requeridos correctamente');
      this.usuarioForm.markAllAsTouched();
      return;
    }

    if (this.verificarNombreDuplicado()) {
      this.mostrarMensajeError('Ya existe un usuario con ese nombre');
      return;
    }

    if (this.verificarEmailDuplicado()) {
      this.mostrarMensajeError('Ya existe un usuario con ese email');
      return;
    }

    const fuente = this.todosLosUsuarios.length > 0 ? this.todosLosUsuarios : this.usuarios;

    if (this.editMode && this.selectedUsuarioId && this.esUsuarioActualPorId(this.selectedUsuarioId)) {
      const formValue = this.usuarioForm.getRawValue();
      const usuarioOriginal = fuente.find(u => u.id === this.selectedUsuarioId);

      if (usuarioOriginal && formValue.role !== usuarioOriginal.role) {
        this.mostrarMensajeError('Por seguridad, no puedes cambiar tu propio rol mientras estás logueado');
        return;
      }
    }

    this.guardando = true;
    this.limpiarMensajes();

    if (this.editMode && this.selectedUsuarioId) {
      this.actualizarUsuarioExistente();
    } else {
      this.crearNuevoUsuario();
    }
  }

  private actualizarUsuarioExistente(): void {
    const formValue = this.usuarioForm.getRawValue();
    const fuente = this.todosLosUsuarios.length > 0 ? this.todosLosUsuarios : this.usuarios;
    const usuarioOriginal = fuente.find(u => u.id === this.selectedUsuarioId);

    const usuarioActualizar: UsuarioCreateDTO = {
      email: formValue.email.trim(),
      name: formValue.name.trim(),
      phone: formValue.phone ? formValue.phone.trim() : '',
      password: formValue.password || '',
      role: this.usuarioForm.get('role')?.disabled
        ? (usuarioOriginal?.role || 'CUSTOMER')
        : formValue.role
    };

    const editedId = this.selectedUsuarioId as number;

    this.usuarioService.actualizarUsuario(editedId, usuarioActualizar).subscribe({
      next: (usuarioActualizado) => {
        this.guardando = false;
        this.cancelar();
        this.mostrarMensajeExito('Usuario actualizado correctamente');

        const currentUserId = this.authService.obtenerUsuarioId();
        if (currentUserId && currentUserId === editedId) {
          this.authService.updateCurrentUser(usuarioActualizado);
        }

        this.recargarListaSinLoading();
      },
      error: (err) => {
        console.error('Error al actualizar usuario:', err);
        this.mostrarMensajeError(err?.error?.message || 'Error al actualizar el usuario');
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  private crearNuevoUsuario(): void {
    const formValue = this.usuarioForm.getRawValue();
    const usuarioCrear: UsuarioCreateDTO = {
      email: formValue.email.trim(),
      name: formValue.name.trim(),
      phone: formValue.phone ? formValue.phone.trim() : '',
      role: formValue.role,
      password: formValue.password
    };

    this.usuarioService.crearUsuario(usuarioCrear).subscribe({
      next: () => {
        this.guardando = false;
        this.cancelar();
        this.mostrarMensajeExito('Usuario creado correctamente');
        this.recargarListaSinLoading();
      },
      error: (err) => {
        console.error('Error al crear usuario:', err);
        this.mostrarMensajeError(err?.error?.message || 'Error al crear el usuario');
        this.guardando = false;
        this.cdr.detectChanges();
      }
    });
  }

  eliminarUsuario(id: number, email: string): void {
    if (this.procesandoId === id) return;

    // Verificar si es el usuario actual usando el token
    const userInfo = this.authService.getUserInfo();
    if (userInfo?.email === email) {
      this.mostrarMensajeError('Por seguridad, no puedes eliminar tu propio usuario mientras estás logueado como administrador');
      return;
    }

    if (confirm(`¿Está seguro de que desea eliminar el usuario "${email}"?`)) {
      this.procesandoId = id;
      this.limpiarMensajes();

      this.usuarioService.eliminarUsuario(id).subscribe({
        next: () => {
          this.mostrarMensajeExito('Usuario eliminado correctamente');
          this.recargarListaSinLoading();
        },
        error: (err) => {
          console.error('Error al eliminar usuario:', err);
          this.mostrarMensajeError(err?.error?.message || 'Error al eliminar el usuario');
          this.procesandoId = null;
          this.cdr.detectChanges();
        }
      });
    }
  }

  /**
   * Inactiva un usuario (Soft Delete)
   */
  inactivarUsuario(id: number, email: string): void {
    if (this.procesandoId === id) return;

    const userInfo = this.authService.getUserInfo();
    if (userInfo?.email === email) {
      this.mostrarMensajeError('No puedes inactivar tu propio usuario');
      return;
    }

    if (confirm(`¿Desea inactivar el usuario "${email}"?\n\nEl usuario no será eliminado, solo marcado como inactivo.`)) {
      this.procesandoId = id;
      this.limpiarMensajes();

      this.usuarioService.inactivarUsuario(id).subscribe({
        next: () => {
          this.mostrarMensajeExito('Usuario inactivado correctamente');
          this.recargarListaSinLoading();
        },
        error: (err) => {
          console.error('Error al inactivar usuario:', err);
          this.mostrarMensajeError(err?.error?.message || 'Error al inactivar el usuario');
          this.procesandoId = null;
          this.cdr.detectChanges();
        }
      });
    }
  }

  /**
   * Reactiva un usuario previamente inactivado
   */
  reactivarUsuario(id: number, email: string): void {
    if (this.procesandoId === id) return;

    if (confirm(`¿Desea reactivar el usuario "${email}"?`)) {
      this.procesandoId = id;
      this.limpiarMensajes();

      this.usuarioService.reactivarUsuario(id).subscribe({
        next: () => {
          this.mostrarMensajeExito('Usuario reactivado correctamente');
          this.recargarListaSinLoading();
        },
        error: (err) => {
          console.error('Error al reactivar usuario:', err);
          this.mostrarMensajeError(err?.error?.message || 'Error al reactivar el usuario');
          this.procesandoId = null;
          this.cdr.detectChanges();
        }
      });
    }
  }

  /**
   * Verifica si un usuario está activo
   */
  estaActivo(usuario: UsuarioDTO): boolean {
    return usuario.deletedAt === null || usuario.deletedAt === undefined;
  }

  private setupFormValidators(): void {
    // Para crear nuevo usuario, contraseña es requerida con patrón
    this.usuarioForm.get('password')?.setValidators([
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(20),
      Validators.pattern(/^(?=.*[A-Z])(?=.*\d).+$/)
    ]);
    this.usuarioForm.get('confirmPassword')?.setValidators(Validators.required);
    this.usuarioForm.setValidators(this.passwordsMatchValidator);
    this.usuarioForm.updateValueAndValidity();
  }

  private setupFormValidatorsForEdit(): void {
    // Para editar usuario, contraseña es opcional
    this.usuarioForm.get('password')?.clearValidators();
    this.usuarioForm.get('confirmPassword')?.clearValidators();
    this.usuarioForm.clearValidators();

    // Mantener validaciones básicas
    this.usuarioForm.get('email')?.setValidators([Validators.required, Validators.email]);
    this.usuarioForm.get('name')?.setValidators([
      Validators.required,
      Validators.minLength(4),
      Validators.maxLength(50),
      Validators.pattern(/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/)
    ]);
    this.usuarioForm.get('phone')?.setValidators([
      Validators.pattern(/^[0-9]{9}$/)
    ]);
    this.usuarioForm.get('role')?.setValidators(Validators.required);

    this.usuarioForm.updateValueAndValidity();
  }

  limpiarMensajes(): void {
    this.error = '';
    this.success = '';
  }

  mostrarMensajeExito(mensaje: string): void {
    this.success = mensaje;
    this.error = '';

    setTimeout(() => {
      this.success = '';
      this.cdr.markForCheck();
    }, 3500);
  }

  mostrarMensajeError(mensaje: string): void {
    this.error = mensaje;
    this.success = '';
  }

  obtenerRolTexto(role: string): string {
    return role === 'ADMIN' ? 'Administrador' : 'Cliente';
  }

  esUsuarioActual(email: string): boolean {
    const userInfo = this.authService.getUserInfo();
    return userInfo ? email === userInfo.email : false;
  }

  esUsuarioActualPorId(id?: number): boolean {
    if (!id) return false;
    const fuente = this.todosLosUsuarios.length > 0 ? this.todosLosUsuarios : this.usuarios;
    const usuario = fuente.find(u => u.id === id);
    if (!usuario) return false;

    const userInfo = this.authService.getUserInfo();
    return userInfo ? usuario.email === userInfo.email : false;
  }

  verificarNombreDuplicado(): boolean {
    const nombreFormulario = this.usuarioForm.get('name')?.value?.trim().toLowerCase();
    if (!nombreFormulario) {
      return false;
    }

    const fuente = this.todosLosUsuarios.length > 0 ? this.todosLosUsuarios : this.usuarios;
    return fuente.some(usuario => {
      if (this.editMode && usuario.id === this.selectedUsuarioId) {
        return false;
      }
      return usuario.name?.trim().toLowerCase() === nombreFormulario;
    });
  }

  verificarEmailDuplicado(): boolean {
    const emailFormulario = this.usuarioForm.get('email')?.value?.trim().toLowerCase();
    if (!emailFormulario) {
      return false;
    }

    const fuente = this.todosLosUsuarios.length > 0 ? this.todosLosUsuarios : this.usuarios;
    return fuente.some(usuario => {
      if (this.editMode && usuario.id === this.selectedUsuarioId) {
        return false;
      }
      return usuario.email?.trim().toLowerCase() === emailFormulario;
    });
  }

  esCampoProtegido(campo: string): boolean {
    if (this.editMode && this.selectedUsuarioId && this.esUsuarioActualPorId(this.selectedUsuarioId)) {
      return campo === 'role';
    }
    return false;
  }

  obtenerMensajeProteccion(campo: string): string {
    if (this.esCampoProtegido(campo)) {
      switch (campo) {
        case 'role':
          return 'Por seguridad, no puedes cambiar tu propio rol';
        default:
          return 'Este campo está protegido para tu seguridad';
      }
    }
    return '';
  }

  // Métodos para formateo de fechas
  formatearFechaRelativa(fecha: string | null | undefined): string {
    if (!fecha) return '-';

    const fechaDate = new Date(fecha);
    const ahora = new Date();
    const diffMs = ahora.getTime() - fechaDate.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHoras = Math.floor(diffMs / 3600000);
    const diffDias = Math.floor(diffMs / 86400000);

    if (diffMins < 1) return 'Hace unos segundos';
    if (diffMins < 60) return `Hace ${diffMins} min`;
    if (diffHoras < 24) return `Hace ${diffHoras}h`;
    if (diffDias < 7) return `Hace ${diffDias} día${diffDias > 1 ? 's' : ''}`;

    return fechaDate.toLocaleDateString('es-ES', { day: '2-digit', month: 'short' });
  }

  formatearFechaCompleta(fecha: string | null | undefined): string {
    if (!fecha) return '-';

    const fechaDate = new Date(fecha);
    return fechaDate.toLocaleString('es-ES', {
      day: '2-digit',
      month: 'long',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatearFechaHora(fecha: string | null | undefined): string {
    if (!fecha) return '-';

    const fechaDate = new Date(fecha);
    return fechaDate.toLocaleString('es-ES', {
      day: '2-digit',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  esCuentaBloqueada(usuario: UsuarioDTO): boolean {
    if (!usuario.lockedUntil) return false;

    const lockedUntil = new Date(usuario.lockedUntil);
    const ahora = new Date();

    return lockedUntil > ahora;
  }

  // ==========================================
  // LÓGICA DE LOGS DE SISTEMA
  // ==========================================

  abrirModalLogs(): void {
    this.showLogsModal = true;
    this.sesionesPage = 1;
    this.logsPage = 1;
    this.logsBusqueda = '';
    this.cargarLogs();
  }

  cerrarModalLogs(): void {
    this.showLogsModal = false;
    this.logsError = '';
  }

  cambiarLogTab(tab: 'sesiones' | 'auditoria'): void {
    this.activeLogTab = tab;
    this.logsError = '';
    this.cargarLogs();
  }

  cargarLogs(): void {
    this.loadingLogs = true;
    this.logsError = '';

    if (this.activeLogTab === 'sesiones') {
      this.subscriptions.push(
        this.sessionLogService.listarSesionesActivas().subscribe({
          next: (data) => {
            this.sesionesActivas = data || [];
            this.aplicarPaginacionSesiones();
            this.loadingLogs = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error al cargar sesiones activas:', err);
            this.logsError = err?.status === 0
              ? 'No se pudo conectar con el servidor para obtener las sesiones activas.'
              : (err?.error?.message || 'Error al cargar las sesiones activas.');
            this.loadingLogs = false;
            this.cdr.detectChanges();
          }
        })
      );
    } else {
      this.subscriptions.push(
        this.auditLogService.listarRecientes(24).subscribe({
          next: (data) => {
            this.auditLogs = data || [];
            this.filtrarAuditLogs();
            this.loadingLogs = false;
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('Error al cargar auditoría:', err);
            this.logsError = err?.status === 0
              ? 'No se pudo conectar con el servidor para obtener el registro de auditoría.'
              : (err?.error?.message || 'Error al cargar el registro de auditoría.');
            this.loadingLogs = false;
            this.cdr.detectChanges();
          }
        })
      );
    }
  }

  reintentarLogs(): void {
    this.cargarLogs();
  }

  aplicarPaginacionSesiones(): void {
    const totalPages = Math.ceil(this.sesionesActivas.length / this.sesionesPerPage);
    if (this.sesionesPage > totalPages && totalPages > 0) {
      this.sesionesPage = totalPages;
    } else if (this.sesionesPage < 1) {
      this.sesionesPage = 1;
    }
    const start = (this.sesionesPage - 1) * this.sesionesPerPage;
    this.sesionesPaginadas = this.sesionesActivas.slice(start, start + this.sesionesPerPage);
  }

  onSesionesPageChange(page: number): void {
    this.sesionesPage = page;
    this.aplicarPaginacionSesiones();
  }

  onSesionesPerPageChange(size: number): void {
    this.sesionesPerPage = size;
    this.sesionesPage = 1;
    this.aplicarPaginacionSesiones();
  }

  filtrarAuditLogs(): void {
    const term = this.logsBusqueda.trim().toLowerCase();
    if (!term) {
      this.auditLogsFiltrados = this.auditLogs;
    } else {
      this.auditLogsFiltrados = this.auditLogs.filter(log => {
        const extraEndpoint = String((log as any).endpoint ?? (log as any).ipAddress ?? '').toLowerCase();
        return (
          (log.actionType || '').toLowerCase().includes(term) ||
          (log.userName || '').toLowerCase().includes(term) ||
          (log.entityType || '').toLowerCase().includes(term) ||
          (log.description || '').toLowerCase().includes(term) ||
          String(log.userId ?? '').includes(term) ||
          String(log.entityId ?? '').includes(term) ||
          extraEndpoint.includes(term)
        );
      });
    }
    this.aplicarPaginacionLogs();
  }

  onLogsBusquedaChange(): void {
    this.logsPage = 1;
    this.filtrarAuditLogs();
  }

  aplicarPaginacionLogs(): void {
    const totalPages = Math.ceil(this.auditLogsFiltrados.length / this.logsPerPage);
    if (this.logsPage > totalPages && totalPages > 0) {
      this.logsPage = totalPages;
    } else if (this.logsPage < 1) {
      this.logsPage = 1;
    }
    const start = (this.logsPage - 1) * this.logsPerPage;
    this.auditLogsPaginados = this.auditLogsFiltrados.slice(start, start + this.logsPerPage);
  }

  onLogsPageChange(page: number): void {
    this.logsPage = page;
    this.aplicarPaginacionLogs();
  }

  onLogsPerPageChange(size: number): void {
    this.logsPerPage = size;
    this.logsPage = 1;
    this.aplicarPaginacionLogs();
  }

  forzarCierreSesion(token: string | undefined): void {
    if (!token || this.cerrandoSesionToken === token) return;
    if (confirm('¿Está seguro de forzar el cierre de esta sesión? El usuario será desconectado inmediatamente.')) {
      this.cerrandoSesionToken = token;
      this.subscriptions.push(
        this.sessionLogService.cerrarSesionExterna(token).subscribe({
          next: () => {
            this.cerrandoSesionToken = null;
            this.mostrarMensajeExito('Sesión cerrada remotamente.');
            this.cargarLogs();

            // Si el admin cierra su propia sesión actual, desconectarlo inmediatamente
            const currentToken = this.authService.getToken();
            if (token === currentToken) {
              this.authService.logout();
            }
          },
          error: () => {
            this.cerrandoSesionToken = null;
            this.logsError = 'No se pudo cerrar la sesión seleccionada.';
            this.cdr.detectChanges();
          }
        })
      );
    }
  }

  trackByUsuario(index: number, usuario: UsuarioDTO): number | string {
    return usuario.id ?? usuario.email ?? index;
  }

  trackBySession(index: number, session: SessionLog): number | string {
    return session.sessionToken ?? (session as any).id ?? index;
  }

  trackByAuditLog(index: number, log: AuditLog): number | string {
    return (log as any).id ?? `${log.createdAt}-${log.actionType}-${index}`;
  }
}
