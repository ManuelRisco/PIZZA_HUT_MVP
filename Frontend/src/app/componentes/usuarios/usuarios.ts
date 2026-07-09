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
  usuarios: UsuarioDTO[] = [];
  usuariosPaginados: UsuarioDTO[] = [];
  currentPage: number = 1;
  itemsPerPage: number = 10;
  usuarioForm: FormGroup;
  editMode = false;
  selectedUsuarioId?: number;
  showForm = false;
  loading: boolean = true;
  error = '';
  success = '';
  filtroActivo: 'todos' | 'admins' | 'activos' | 'admins-activos' = 'todos';
  
  // Variables para mostrar/ocultar contraseñas
  showPassword = false;
  showConfirmPassword = false;

  // Variables para la modal de creación/edición
  mostrarModal: boolean = false;
  
  // Variables para Logs del Sistema
  showLogsModal: boolean = false;
  activeLogTab: 'sesiones' | 'auditoria' = 'sesiones';
  sesionesActivas: SessionLog[] = [];
  auditLogs: AuditLog[] = [];
  loadingLogs: boolean = false;

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

  cargarUsuarios(): void {
    this.loading = true;
    this.limpiarMensajes();
    
    // Aplicar filtro según el seleccionado (Specification Pattern)
    this.aplicarFiltro();
  }

  /**
   * Aplica el filtro seleccionado usando Specification Pattern
   */
  aplicarFiltro(): void {
    this.loading = true;
    
    switch (this.filtroActivo) {
      case 'admins':
        this.cargarAdministradores();
        break;
      case 'activos':
        this.cargarUsuariosActivos();
        break;
      case 'admins-activos':
        this.cargarAdministradoresActivos();
        break;
      default:
        this.cargarTodosLosUsuarios();
    }
  }

  /**
   * Carga todos los usuarios (sin filtro)
   */
  cargarTodosLosUsuarios(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = [...usuarios];
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar usuarios:', error);
        this.mostrarMensajeError('Error al cargar los usuarios');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Carga solo administradores (filtrado local)
   */
  cargarAdministradores(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = usuarios.filter(u => u.role === 'ADMIN');
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar administradores:', error);
        this.mostrarMensajeError('Error al cargar administradores');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Carga solo usuarios activos (filtrado local)
   */
  cargarUsuariosActivos(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = usuarios.filter(u => this.estaActivo(u));
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar usuarios activos:', error);
        this.mostrarMensajeError('Error al cargar usuarios activos');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Carga administradores activos (filtrado local)
   */
  cargarAdministradoresActivos(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = usuarios.filter(u => u.role === 'ADMIN' && this.estaActivo(u));
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (error) => {
        console.error('Error al cargar administradores activos:', error);
        this.mostrarMensajeError('Error al cargar administradores activos');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Cambia el filtro activo y recarga los usuarios
   */
  cambiarFiltro(filtro: 'todos' | 'admins' | 'activos' | 'admins-activos'): void {
    this.filtroActivo = filtro;
    this.aplicarFiltro();
  }

  /**
   * Aplica paginación a los usuarios
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

  recargarListaSinLoading(): void {
    // Aplicar el filtro activo en lugar de cargar todos los usuarios
    this.aplicarFiltroSinLoading();
  }

  /**
   * Aplica el filtro activo sin mostrar el indicador de carga
   */
  aplicarFiltroSinLoading(): void {
    switch (this.filtroActivo) {
      case 'admins':
        this.cargarAdministradoresSinLoading();
        break;
      case 'activos':
        this.cargarUsuariosActivosSinLoading();
        break;
      case 'admins-activos':
        this.cargarAdministradoresActivosSinLoading();
        break;
      default:
        this.cargarTodosLosUsuariosSinLoading();
    }
  }

  /**
   * Carga todos los usuarios sin indicador de loading
   */
  cargarTodosLosUsuariosSinLoading(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = [...usuarios];
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.cdr.markForCheck();
        }, 10);
      },
      error: (error) => {
        console.error('Error al recargar usuarios:', error);
        this.mostrarMensajeError('Error al recargar los usuarios');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Carga administradores sin indicador de loading (filtrado local)
   */
  cargarAdministradoresSinLoading(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = usuarios.filter(u => u.role === 'ADMIN');
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.cdr.markForCheck();
        }, 10);
      },
      error: (error) => {
        console.error('Error al recargar administradores:', error);
        this.mostrarMensajeError('Error al recargar administradores');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Carga usuarios activos sin indicador de loading (filtrado local)
   */
  cargarUsuariosActivosSinLoading(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = usuarios.filter(u => this.estaActivo(u));
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.cdr.markForCheck();
        }, 10);
      },
      error: (error) => {
        console.error('Error al recargar usuarios activos:', error);
        this.mostrarMensajeError('Error al recargar usuarios activos');
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /**
   * Carga administradores activos sin indicador de loading (filtrado local)
   */
  cargarAdministradoresActivosSinLoading(): void {
    this.usuarioService.listarUsuarios().subscribe({
      next: (usuarios) => {
        this.usuarios = usuarios.filter(u => u.role === 'ADMIN' && this.estaActivo(u));
        this.aplicarPaginacion();
        this.loading = false;
        this.cdr.detectChanges();
        setTimeout(() => {
          this.cdr.markForCheck();
        }, 10);
      },
      error: (error) => {
        console.error('Error al recargar administradores activos:', error);
        this.mostrarMensajeError('Error al recargar administradores activos');
        this.loading = false;
        this.cdr.detectChanges();
      },
      complete: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  abrirFormulario(): void {
    this.showForm = true;
    this.editMode = false;
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
    const isValidForCreation = !this.editMode && this.usuarioForm.valid;
    
    let isValidForEdit = false;
    if (this.editMode) {
      const emailValid = this.usuarioForm.get('email')?.valid ?? false;
      const nameValid = this.usuarioForm.get('name')?.valid ?? false;
      const roleDisabled = this.usuarioForm.get('role')?.disabled ?? false;
      const roleValid = this.usuarioForm.get('role')?.valid ?? false;
      
      isValidForEdit = emailValid && nameValid && (roleDisabled || roleValid);
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

    if (this.editMode && this.selectedUsuarioId && this.esUsuarioActualPorId(this.selectedUsuarioId)) {
      const formValue = this.usuarioForm.getRawValue();
      const usuarioOriginal = this.usuarios.find(u => u.id === this.selectedUsuarioId);
      
      if (usuarioOriginal && formValue.role !== usuarioOriginal.role) {
        this.mostrarMensajeError('ðŸš« Por seguridad, no puedes cambiar tu propio rol mientras estás logueado');
        return;
      }
    }

    this.loading = true;
    this.limpiarMensajes();

    if (this.editMode && this.selectedUsuarioId) {
      this.actualizarUsuarioExistente();
    } else {
      this.crearNuevoUsuario();
    }
  }

  private actualizarUsuarioExistente(): void {
    const formValue = this.usuarioForm.getRawValue();
    const usuarioOriginal = this.usuarios.find(u => u.id === this.selectedUsuarioId);
    
    const usuarioActualizar: UsuarioCreateDTO = {
      email: formValue.email.trim(),
      name: formValue.name.trim(),
      phone: formValue.phone ? formValue.phone.trim() : '',
      password: formValue.password || '',
      role: this.usuarioForm.get('role')?.disabled ? 
            (usuarioOriginal?.role || 'CUSTOMER') : 
            formValue.role
    };
    
    this.usuarioService.actualizarUsuario(this.selectedUsuarioId as number, usuarioActualizar).subscribe({
      next: (usuarioActualizado) => {
        this.mostrarMensajeExito('Usuario actualizado correctamente');
        this.cancelar();
        
        const currentUserId = this.authService.obtenerUsuarioId();
        if (currentUserId && currentUserId === this.selectedUsuarioId) {
          this.authService.updateCurrentUser(usuarioActualizado);
        }
        
        this.recargarListaSinLoading();
      },
      error: (error) => {
        console.error('Error al actualizar usuario:', error);
        this.mostrarMensajeError(error.error?.message || 'Error al actualizar el usuario');
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  private crearNuevoUsuario(): void {
    const formValue = this.usuarioForm.getRawValue();
    const usuarioCrear: UsuarioCreateDTO = {
      email: formValue.email,
      name: formValue.name,
      phone: formValue.phone || '',
      role: formValue.role,
      password: formValue.password
    };
    
    this.usuarioService.crearUsuario(usuarioCrear).subscribe({
      next: (response) => {
        this.mostrarMensajeExito('Usuario creado correctamente');
        this.cancelar();
        this.recargarListaSinLoading();
      },
      error: (error) => {
        console.error('Error al crear usuario:', error);
        this.mostrarMensajeError(error.error?.message || 'Error al crear el usuario');
        this.loading = false;
      },
      complete: () => {
        this.loading = false;
      }
    });
  }

  eliminarUsuario(id: number, email: string): void {
    // Verificar si es el usuario actual usando el token
    const userInfo = this.authService.getUserInfo();
    if (userInfo?.email === email) {
      this.mostrarMensajeError('ðŸš« Por seguridad, no puedes eliminar tu propio usuario mientras estás logueado como administrador');
      return;
    }

    if (confirm(`¿Está seguro de que desea eliminar el usuario "${email}"?`)) {
      this.loading = true;
      this.limpiarMensajes();
      
      this.usuarioService.eliminarUsuario(id).subscribe({
        next: (response) => {
          this.mostrarMensajeExito('Usuario eliminado correctamente');
          
          // Recargar la lista desde el servidor para mantener sincronización
          this.recargarListaSinLoading();
        },
        error: (error) => {
          console.error('Error al eliminar usuario:', error);
          this.mostrarMensajeError(error.error?.message || 'Error al eliminar el usuario');
          this.loading = false;
        },
        complete: () => {
          // Asegurar que siempre se termine el loading
          this.loading = false;
        }
      });
    }
  }

  /**
   * Inactiva un usuario (Soft Delete)
   */
  inactivarUsuario(id: number, email: string): void {
    const userInfo = this.authService.getUserInfo();
    if (userInfo?.email === email) {
      this.mostrarMensajeError('ðŸš« No puedes inactivar tu propio usuario');
      return;
    }

    if (confirm(`¿Desea inactivar el usuario "${email}"?\n\nEl usuario no será eliminado, solo marcado como inactivo.`)) {
      this.loading = true;
      this.limpiarMensajes();
      
      this.usuarioService.inactivarUsuario(id).subscribe({
        next: (response) => {
          this.mostrarMensajeExito('âœ… Usuario inactivado correctamente');
          this.recargarListaSinLoading();
        },
        error: (error) => {
          console.error('Error al inactivar usuario:', error);
          this.mostrarMensajeError(error.error?.message || 'Error al inactivar el usuario');
          this.loading = false;
        }
      });
    }
  }

  /**
   * Reactiva un usuario previamente inactivado
   */
  reactivarUsuario(id: number, email: string): void {
    if (confirm(`¿Desea reactivar el usuario "${email}"?`)) {
      this.loading = true;
      this.limpiarMensajes();
      
      this.usuarioService.reactivarUsuario(id).subscribe({
        next: (response) => {
          this.mostrarMensajeExito('âœ… Usuario reactivado correctamente');
          this.recargarListaSinLoading();
        },
        error: (error) => {
          console.error('Error al reactivar usuario:', error);
          this.mostrarMensajeError(error.error?.message || 'Error al reactivar el usuario');
          this.loading = false;
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
    
    // Limpiar el mensaje después de 3 segundos
    setTimeout(() => {
      this.success = '';
    }, 3000);
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
    const usuario = this.usuarios.find(u => u.id === id);
    if (!usuario) return false;
    
    const userInfo = this.authService.getUserInfo();
    return userInfo ? usuario.email === userInfo.email : false;
  }

  verificarNombreDuplicado(): boolean {
    const nombreFormulario = this.usuarioForm.get('name')?.value?.trim().toLowerCase();
    
    if (!nombreFormulario) {
      return false; // Si no hay nombre, no hay duplicado
    }

    // Filtrar usuarios con el mismo nombre (ignorando mayúsculas/minúsculas)
    const usuariosConMismoNombre = this.usuarios.filter(usuario => {
      // En modo edición, ignorar el usuario actual
      if (this.editMode && usuario.id === this.selectedUsuarioId) {
        return false;
      }
      
      return usuario.name?.trim().toLowerCase() === nombreFormulario;
    });

    return usuariosConMismoNombre.length > 0;
  }

  verificarEmailDuplicado(): boolean {
    const emailFormulario = this.usuarioForm.get('email')?.value?.trim().toLowerCase();
    
    if (!emailFormulario) {
      return false; // Si no hay email, no hay duplicado
    }

    // Filtrar usuarios con el mismo email (ignorando mayúsculas/minúsculas)
    const usuariosConMismoEmail = this.usuarios.filter(usuario => {
      // En modo edición, ignorar el usuario actual
      if (this.editMode && usuario.id === this.selectedUsuarioId) {
        return false;
      }
      
      return usuario.email?.trim().toLowerCase() === emailFormulario;
    });

    return usuariosConMismoEmail.length > 0;
  }

  esCampoProtegido(campo: string): boolean {
    // Verificar si el campo debería estar protegido para el usuario actual
    if (this.editMode && this.selectedUsuarioId && this.esUsuarioActualPorId(this.selectedUsuarioId)) {
      return campo === 'role'; // Solo el rol está protegido para el usuario actual
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
    this.cargarLogs();
  }

  cerrarModalLogs(): void {
    this.showLogsModal = false;
  }

  cambiarLogTab(tab: 'sesiones' | 'auditoria'): void {
    this.activeLogTab = tab;
    this.cargarLogs();
  }

  cargarLogs(): void {
    this.loadingLogs = true;
    if (this.activeLogTab === 'sesiones') {
      this.subscriptions.push(
        this.sessionLogService.listarSesionesActivas().subscribe({
          next: (data) => {
            this.sesionesActivas = data;
            this.loadingLogs = false;
            this.cdr.detectChanges();
          },
          error: (error) => {
            this.mostrarMensajeError('Error al cargar sesiones activas');
            this.loadingLogs = false;
            this.cdr.detectChanges();
          }
        })
      );
    } else {
      this.subscriptions.push(
        this.auditLogService.listarRecientes(24).subscribe({
          next: (data) => {
            this.auditLogs = data;
            this.loadingLogs = false;
            this.cdr.detectChanges();
          },
          error: (error) => {
            this.mostrarMensajeError('Error al cargar registro de auditoría');
            this.loadingLogs = false;
            this.cdr.detectChanges();
          }
        })
      );
    }
  }

  forzarCierreSesion(token: string | undefined): void {
    if (!token) return;
    if (confirm('¿Está seguro de forzar el cierre de esta sesión? El usuario será desconectado inmediatamente.')) {
      this.subscriptions.push(
        this.sessionLogService.cerrarSesionExterna(token).subscribe({
          next: () => {
            this.mostrarMensajeExito('Sesión cerrada remotamente.');
            this.cargarLogs();
            
            // Si el admin cierra su propia sesión actual, desconectarlo inmediatamente
            const currentToken = this.authService.getToken();
            if (token === currentToken) {
              this.authService.logout();
            }
          },
          error: (error) => {
            this.mostrarMensajeError('Error al cerrar sesión remotamente.');
          }
        })
      );
    }
  }
}

