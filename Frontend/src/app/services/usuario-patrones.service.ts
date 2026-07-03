import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { UsuarioDTO } from '../models/usuario.interface';
import { UsuarioService } from './usuario.service';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

// Interface para los datos al crear cliente con Factory Pattern
export interface CrearClienteDTO {
  email: string;
  password: string;
  name: string;
  phone?: string;
}

// Interface para los datos al crear admin con Factory Pattern
export interface CrearAdminDTO {
  email: string;
  password: string;
  name: string;
}

// Interface para la respuesta de informaciÃ³n de patrones
export interface PatronesInfo {
  patrones_implementados: string[];
  endpoints_disponibles: { [key: string]: string };
  documentacion: string;
}

/**
 * Servicio que simulaba los patrones de diseÃ±o del backend para usuarios.
 * Ahora utiliza los endpoints estÃ¡ndar y filtra en el cliente para mantener la interfaz.
 */
@Injectable({
  providedIn: 'root'
})
export class UsuarioPatronesService {
  private readonly apiUrl= environment.apiUrl;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(
    private readonly http: HttpClient,
    private readonly usuarioService: UsuarioService,
    private readonly authService: AuthService
  ) { }

  /**
   * Obtiene informaciÃ³n sobre los patrones implementados (Simulado localmente)
   */
  obtenerInformacionPatrones(): Observable<PatronesInfo> {
    return of({
      patrones_implementados: ['Factory Method', 'Specification', 'Composite'],
      endpoints_disponibles: {
        'informacion': 'GET /api/usuarios/patrones/info',
        'crearCliente': 'POST /api/usuarios/patrones/crear-cliente',
        'crearAdmin': 'POST /api/usuarios/patrones/crear-admin',
        'admins': 'GET /api/usuarios/patrones/admins',
        'activos': 'GET /api/usuarios/patrones/activos',
        'adminsActivos': 'GET /api/usuarios/patrones/admins-activos'
      },
      documentacion: 'InformaciÃ³n sobre los patrones de diseÃ±o simulada en el frontend.'
    });
  }

  /**
   * Crea un cliente usando Factory Pattern (Mapeado a /api/registro)
   */
  crearCliente(datos: CrearClienteDTO): Observable<{ message: string }> {
    return this.http.post<any>(
      `${this.apiUrl}/registro`,
      { ...datos, role: 'CUSTOMER' },
      this.httpOptions
    ).pipe(map(res => ({ message: res.message || 'Cliente creado con Ã©xito' })));
  }

  /**
   * Crea un administrador usando Factory Pattern (Mapeado a /api/usuarios)
   * Requiere autenticaciÃ³n como ADMIN
   */
  crearAdministrador(datos: CrearAdminDTO): Observable<{ message: string }> {
    const token = this.authService.getToken();
    const headers = new HttpHeaders({
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    });
    return this.http.post<any>(
      `${this.apiUrl}/usuarios`,
      { ...datos, role: 'ADMIN' },
      { headers }
    ).pipe(map(res => ({ message: res.message || 'Administrador creado con Ã©xito' })));
  }

  /**
   * Lista solo usuarios administradores usando Specification Pattern (Simulado)
   */
  listarAdministradores(): Observable<UsuarioDTO[]> {
    return this.usuarioService.listarUsuarios().pipe(
      map(usuarios => usuarios.filter((u: UsuarioDTO) => u.role === 'ADMIN'))
    );
  }

  /**
   * Lista solo usuarios activos (no eliminados) usando Specification Pattern (Simulado)
   */
  listarUsuariosActivos(): Observable<UsuarioDTO[]> {
    return this.usuarioService.listarUsuarios().pipe(
      map(usuarios => usuarios.filter((u: UsuarioDTO) => u.deletedAt === null))
    );
  }

  /**
   * Lista administradores activos usando Composite Specification Pattern (Simulado)
   */
  listarAdministradoresActivos(): Observable<UsuarioDTO[]> {
    return this.usuarioService.listarUsuarios().pipe(
      map(usuarios => usuarios.filter((u: UsuarioDTO) => u.role === 'ADMIN' && u.deletedAt === null))
    );
  }
}

