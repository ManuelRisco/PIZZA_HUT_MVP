import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { UsuarioDTO } from '../models/usuario.interface';
import { UsuarioService } from './usuario.service';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export interface CrearClienteDTO {
  email: string;
  password: string;
  name: string;
  phone?: string;
  dni?: string | null;
  ageVerified?: boolean;
}

export interface CrearAdminDTO {
  email: string;
  password: string;
  name: string;
}

export interface PatronesInfo {
  patrones_implementados: string[];
  endpoints_disponibles: { [key: string]: string };
  documentacion: string;
}

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
      documentacion: 'Información sobre los patrones de diseño simulada en el frontend.'
    });
  }

  crearCliente(datos: CrearClienteDTO): Observable<{ message: string }> {
    return this.http.post<any>(
      `${this.apiUrl}/registro`,
      { ...datos, role: 'CUSTOMER' },
      this.httpOptions
    ).pipe(map(res => ({ message: res.message || 'Cliente creado con éxito' })));
  }

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
    ).pipe(map(res => ({ message: res.message || 'Administrador creado con éxito' })));
  }

  listarAdministradores(): Observable<UsuarioDTO[]> {
    return this.usuarioService.listarUsuarios().pipe(
      map(usuarios => usuarios.filter((u: UsuarioDTO) => u.role === 'ADMIN'))
    );
  }

  listarUsuariosActivos(): Observable<UsuarioDTO[]> {
    return this.usuarioService.listarUsuarios().pipe(
      map(usuarios => usuarios.filter((u: UsuarioDTO) => u.deletedAt === null))
    );
  }

  listarAdministradoresActivos(): Observable<UsuarioDTO[]> {
    return this.usuarioService.listarUsuarios().pipe(
      map(usuarios => usuarios.filter((u: UsuarioDTO) => u.role === 'ADMIN' && u.deletedAt === null))
    );
  }
}

