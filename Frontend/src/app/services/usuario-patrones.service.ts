import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioDTO } from '../models/usuario.interface';

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

// Interface para la respuesta de información de patrones
export interface PatronesInfo {
  patrones_implementados: string[];
  endpoints_disponibles: { [key: string]: string };
  documentacion: string;
}

@Injectable({
  providedIn: 'root'
})
export class UsuarioPatronesService {
  private apiUrl = 'http://localhost:8089/api/usuarios/patrones';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) { }

  /**
   * Obtiene información sobre los patrones implementados
   * Endpoint público
   */
  obtenerInformacionPatrones(): Observable<PatronesInfo> {
    return this.http.get<PatronesInfo>(`${this.apiUrl}/info`);
  }

  /**
   * Crea un cliente usando Factory Pattern
   * Endpoint público - permite registro de clientes
   */
  crearCliente(datos: CrearClienteDTO): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/crear-cliente`,
      datos,
      this.httpOptions
    );
  }

  /**
   * Crea un administrador usando Factory Pattern
   * Requiere autenticación como ADMIN
   */
  crearAdministrador(datos: CrearAdminDTO): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(
      `${this.apiUrl}/crear-admin`,
      datos,
      this.httpOptions
    );
  }

  /**
   * Lista solo usuarios administradores usando Specification Pattern
   * Requiere autenticación como ADMIN
   */
  listarAdministradores(): Observable<UsuarioDTO[]> {
    return this.http.get<UsuarioDTO[]>(`${this.apiUrl}/admins`);
  }

  /**
   * Lista solo usuarios activos (no eliminados) usando Specification Pattern
   * Requiere autenticación como ADMIN
   */
  listarUsuariosActivos(): Observable<UsuarioDTO[]> {
    return this.http.get<UsuarioDTO[]>(`${this.apiUrl}/activos`);
  }

  /**
   * Lista administradores activos usando Composite Specification Pattern
   * Combina dos especificaciones: AdminSpec + ActiveSpec
   * Requiere autenticación como ADMIN
   */
  listarAdministradoresActivos(): Observable<UsuarioDTO[]> {
    return this.http.get<UsuarioDTO[]>(`${this.apiUrl}/admins-activos`);
  }
}
