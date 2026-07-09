import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { UsuarioDTO, UsuarioCreateDTO, LoginDTO, AuthResponseDTO } from '../models/usuario.interface';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class UsuarioService {
  private readonly apiUrl= environment.apiUrl;
  private readonly httpOptions= {
    headers: new HttpHeaders({ 'Content-Type': 'application/json' })
  };

  constructor(private readonly http: HttpClient) { }

  // Operaciones CRUD para Usuarios
  listarUsuarios(): Observable<UsuarioDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/usuarios`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerUsuarioPorId(id: number): Observable<UsuarioDTO> {
    return this.http.get<any>(`${this.apiUrl}/usuarios/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Obtener información del usuario autenticado actual
  obtenerUsuarioActual(): Observable<UsuarioDTO> {
    return this.http.get<any>(`${this.apiUrl}/usuarios/me`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crearUsuario(usuario: UsuarioCreateDTO): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/registro`, usuario, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizarUsuario(id: number, usuario: UsuarioCreateDTO): Observable<UsuarioDTO> {
    return this.http.put<any>(`${this.apiUrl}/usuarios/${id}`, usuario, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminarUsuario(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/usuarios/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Soft Delete - Inactivar/Reactivar
  inactivarUsuario(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/usuarios/${id}/inactivar`, {}).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  reactivarUsuario(id: number): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/usuarios/${id}/reactivar`, {}).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  verificarUsuarioActivo(id: number): Observable<{activo: boolean}> {
    return this.http.get<any>(`${this.apiUrl}/usuarios/${id}/activo`).pipe(map(res => typeof res.data === 'boolean' ? {activo: res.data} : res));
  }

  // Actualizar perfil del usuario actual
  actualizarPerfil(id: number, datos: Partial<UsuarioCreateDTO>): Observable<UsuarioDTO> {
    return this.http.put<any>(`${this.apiUrl}/usuarios/me`, datos, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Cambiar contraseña del usuario actual
  cambiarPassword(id: number, passwordData: { password: string }): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/usuarios/${id}/cambiar-password`, passwordData, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Operaciones de autenticación
  login(loginData: LoginDTO): Observable<AuthResponseDTO> {
    return this.http.post<any>(`${this.apiUrl}/ingresar`, loginData, this.httpOptions).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  // Endpoints públicos para validación de duplicados
  verificarEmail(email: string): Observable<{existe: boolean}> {
    return this.http.get<any>(`${this.apiUrl}/verificar-email?email=${encodeURIComponent(email)}`).pipe(map(res => typeof res.data === 'boolean' ? {existe: res.data} : res));
  }

  verificarNombre(nombre: string): Observable<{existe: boolean}> {
    return this.http.get<any>(`${this.apiUrl}/verificar-nombre?nombre=${encodeURIComponent(nombre)}`).pipe(map(res => typeof res.data === 'boolean' ? {existe: res.data} : res));
  }
}
