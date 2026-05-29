import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UsuarioDTO, UsuarioCreateDTO, LoginDTO, AuthResponseDTO } from '../models/usuario.interface';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private apiUrl = 'http://localhost:8089/api';
  private httpOptions = {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private http: HttpClient) { }

  // Operaciones CRUD para Usuarios
  listarUsuarios(): Observable<UsuarioDTO[]> {
    return this.http.get<UsuarioDTO[]>(`${this.apiUrl}/usuarios`);
  }

  obtenerUsuarioPorId(id: number): Observable<UsuarioDTO> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/usuarios/${id}`);
  }

  // Obtener información del usuario autenticado actual
  obtenerUsuarioActual(): Observable<UsuarioDTO> {
    return this.http.get<UsuarioDTO>(`${this.apiUrl}/usuarios/me`);
  }

  crearUsuario(usuario: UsuarioCreateDTO): Observable<any> {
    return this.http.post(`${this.apiUrl}/registro`, usuario, this.httpOptions);
  }

  actualizarUsuario(id: number, usuario: UsuarioCreateDTO): Observable<UsuarioDTO> {
    return this.http.put<UsuarioDTO>(`${this.apiUrl}/usuarios/${id}`, usuario, this.httpOptions);
  }

  eliminarUsuario(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/usuarios/${id}`);
  }

  // Soft Delete - Inactivar/Reactivar
  inactivarUsuario(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/usuarios/${id}/inactivar`, {});
  }

  reactivarUsuario(id: number): Observable<any> {
    return this.http.patch(`${this.apiUrl}/usuarios/${id}/reactivar`, {});
  }

  verificarUsuarioActivo(id: number): Observable<{activo: boolean}> {
    return this.http.get<{activo: boolean}>(`${this.apiUrl}/usuarios/${id}/activo`);
  }

  // Actualizar perfil del usuario actual
  actualizarPerfil(id: number, datos: Partial<UsuarioCreateDTO>): Observable<UsuarioDTO> {
    return this.http.put<UsuarioDTO>(`${this.apiUrl}/usuarios/me`, datos, this.httpOptions);
  }

  // Cambiar contraseña del usuario actual
  cambiarPassword(id: number, passwordData: { password: string }): Observable<any> {
    return this.http.patch(`${this.apiUrl}/usuarios/${id}/cambiar-password`, passwordData, this.httpOptions);
  }

  // Operaciones de autenticación
  login(loginData: LoginDTO): Observable<AuthResponseDTO> {
    return this.http.post<AuthResponseDTO>(`${this.apiUrl}/ingresar`, loginData, this.httpOptions);
  }

  // Endpoints públicos para validación de duplicados
  verificarEmail(email: string): Observable<{existe: boolean}> {
    return this.http.get<{existe: boolean}>(`${this.apiUrl}/verificar-email?email=${encodeURIComponent(email)}`);
  }

  verificarNombre(nombre: string): Observable<{existe: boolean}> {
    return this.http.get<{existe: boolean}>(`${this.apiUrl}/verificar-nombre?nombre=${encodeURIComponent(nombre)}`);
  }
}