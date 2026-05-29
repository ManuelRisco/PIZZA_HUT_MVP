import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, timer, Subscription } from 'rxjs';
import { tap, switchMap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthResponseDTO, LoginDTO, UsuarioDTO } from '../models/usuario.interface';

// Alias para compatibilidad
export interface AuthResponse extends AuthResponseDTO {}
export interface User extends UsuarioDTO {}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://localhost:8089/api';
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private refreshTokenSubscription?: Subscription;
  private userCheckSubscription?: Subscription;

  constructor(private http: HttpClient, private router: Router) {
    // Verificar si hay un token almacenado al inicializar
    this.loadUserFromToken();
    this.startTokenRefresh();
    this.startUserStatusCheck();
  }

  login(email: string, password: string): Observable<any> {
    // Limpiar cualquier sesión anterior antes de iniciar sesión
    this.clearSession();

    const loginData: LoginDTO = { email, password };
    return this.http.post<any>(`${this.apiUrl}/ingresar`, loginData).pipe(
      tap(response => {
        if (response.token) {
          this.setSession(response);
          this.startTokenRefresh();
          this.startUserStatusCheck();
        }
      })
    );
  }

  private setSession(authResponse: any): void {
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('refreshToken', authResponse.refreshToken);

    // Extraer información del token para validar
    const tokenData = this.getTokenData();

    // Verificar que los datos del token coincidan con la respuesta
    if (tokenData && authResponse.usuario &&
        tokenData.email === authResponse.usuario.email &&
        tokenData.role === authResponse.usuario.role &&
        tokenData.name === authResponse.usuario.name) {

      // Solo almacenar si los datos coinciden (seguridad adicional)
      this.currentUserSubject.next({
        id: authResponse.usuario.id,
        email: authResponse.usuario.email,
        name: authResponse.usuario.name,
        role: authResponse.usuario.role,
        phone: authResponse.usuario.phone
      });
    } else {
      // Si los datos no coinciden, limpiar todo
      this.logout();
      throw new Error('Token data mismatch - potential security issue');
    }
  }

  logout(): void {
    // Limpiar TODAS las claves relacionadas con autenticación
    const keysToRemove = ['token', 'refreshToken', 'currentUser', 'userRole', 'userId', 'userEmail', 'userName'];
    keysToRemove.forEach(key => {
      localStorage.removeItem(key);
      sessionStorage.removeItem(key);
    });

    // Limpiar cualquier clave que empiece con 'auth', 'user' o 'token'
    const localStorageKeys = Object.keys(localStorage);
    localStorageKeys.forEach(key => {
      if (key.startsWith('auth') || key.startsWith('user') || key.startsWith('token')) {
        localStorage.removeItem(key);
      }
    });

    // Limpiar estado del usuario
    this.currentUserSubject.next(null);

    // Detener procesos de refresh y verificación
    this.stopTokenRefresh();
    this.stopUserStatusCheck();

    // Redirigir a login
    this.router.navigate(['/join']);
  }

  // Método privado para limpiar sesión sin redirigir (usado en login)
  private clearSession(): void {
    // Limpiar TODAS las claves relacionadas con autenticación
    const keysToRemove = ['token', 'refreshToken', 'currentUser', 'userRole', 'userId', 'userEmail', 'userName'];
    keysToRemove.forEach(key => {
      localStorage.removeItem(key);
      sessionStorage.removeItem(key);
    });

    // También limpiar cualquier clave que empiece con 'auth' o 'user'
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && (key.startsWith('auth') || key.startsWith('user') || key.startsWith('token'))) {
        localStorage.removeItem(key);
      }
    }

    this.currentUserSubject.next(null);
    this.stopTokenRefresh();
    this.stopUserStatusCheck();
  }

  // Método público para limpiar sesión sin redirigir (usado por interceptor)
  clearSessionSilently(): void {
    this.clearSession();
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    const tokenData = this.getTokenData();
    return tokenData?.role === 'ADMIN';
  }

  getUserRole(): string | null {
    const tokenData = this.getTokenData();
    return tokenData?.role || null;
  }

  obtenerUsuarioId(): number | null {
    const tokenData = this.getTokenData();
    return tokenData?.id || null;
  }

  // Alias para compatibilidad
  estaAutenticado(): boolean {
    return this.isLoggedIn();
  }

  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  // Método para actualizar la información del usuario actual
  updateCurrentUser(usuario: UsuarioDTO): void {
    const currentUser = this.getCurrentUser();
    if (currentUser && usuario.id === currentUser.id) {
      this.currentUserSubject.next({
        id: usuario.id,
        email: usuario.email,
        name: usuario.name,
        role: usuario.role,
        phone: usuario.phone
      });
    }
  }

  // Método para obtener información completa del usuario desde el token
  getUserInfo(): { email: string, name: string, role: string } | null {
    const tokenData = this.getTokenData();
    if (!tokenData) return null;

    return {
      email: tokenData.email,
      name: tokenData.name,
      role: tokenData.role
    };
  }

  // Método para verificar si el usuario actual es el mismo que está en el token
  validateUserIntegrity(): boolean {
    const currentUser = this.getCurrentUser();
    const tokenData = this.getTokenData();

    if (!currentUser || !tokenData) return false;

    return currentUser.email === tokenData.email &&
           currentUser.name === tokenData.name &&
           currentUser.role === tokenData.role;
  }

  // Método para obtener datos del token de manera segura
  private getTokenData(): any {
    const token = this.getToken();
    if (!token) return null;

    return this.decodeToken(token);
  }

  private loadUserFromToken(): void {
    const token = this.getToken();
    if (!token) {
      return;
    }

    // Si el token está expirado, limpiar la sesión inmediatamente
    if (this.isTokenExpired()) {
      console.warn('Token expirado al cargar. Limpiando sesión...');
      this.clearSession();
      return;
    }

    const tokenData = this.getTokenData();
    if (tokenData && tokenData.email && tokenData.name && tokenData.role) {
      this.currentUserSubject.next({
        id: tokenData.id,
        email: tokenData.email,
        name: tokenData.name,
        role: tokenData.role,
        phone: tokenData.phone
      });
    } else {
      this.clearSession();
    }
  }

  // Método para decodificar JWT (básico, sin validación de firma)
  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      return JSON.parse(atob(payload));
    } catch (error) {
      return null;
    }
  }

  // Verificar si el token ha expirado
  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    const decoded = this.decodeToken(token);
    if (!decoded || !decoded.exp) return true;

    const currentTime = Math.floor(Date.now() / 1000);
    return decoded.exp < currentTime;
  }

  // Método para debugging - mostrar contenido del token (solo en desarrollo)
  // NOTA: Solo usar en desarrollo, no en producción
  debugTokenInfo(): void {
    const tokenData = this.getTokenData();
    if (tokenData) {
      console.log('Token Info:', {
        email: tokenData.email,
        name: tokenData.name,
        role: tokenData.role,
        iat: new Date(tokenData.iat * 1000),
        exp: new Date(tokenData.exp * 1000),
        isExpired: this.isTokenExpired()
      });
    } else {
      console.log('No token found or invalid token');
    }
  }

  // Refresh Token - Renovar token automáticamente
  refreshToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    return this.http.post<any>(`${this.apiUrl}/refresh-token`, { refreshToken }).pipe(
      tap(response => {
        if (response.token) {
          localStorage.setItem('token', response.token);
          localStorage.setItem('refreshToken', response.refreshToken);

          // Actualizar el usuario en memoria
          if (response.usuario) {
            this.currentUserSubject.next(response.usuario);
          }
        }
      })
    );
  }

  // Iniciar refresh automático del token cada 15 minutos
  private startTokenRefresh(): void {
    this.stopTokenRefresh(); // Detener cualquier subscripción previa

    // Renovar token cada 15 minutos (900000 ms)
    this.refreshTokenSubscription = timer(900000, 900000).pipe(
      switchMap(() => {
        const token = this.getToken();
        if (token && !this.isTokenExpired()) {
          return this.refreshToken();
        }
        return [];
      })
    ).subscribe({
      next: (response) => {
        console.log('Token renovado automáticamente');
      },
      error: (error) => {
        console.error('Error al renovar token:', error);
        if (error.error?.userInactive) {
          alert('Su cuenta ha sido desactivada. Será redirigido al inicio de sesión.');
          this.logout();
        }
      }
    });
  }

  private stopTokenRefresh(): void {
    if (this.refreshTokenSubscription) {
      this.refreshTokenSubscription.unsubscribe();
    }
  }

  // Verificar estado del usuario cada 5 minutos
  private startUserStatusCheck(): void {
    this.stopUserStatusCheck(); // Detener cualquier subscripción previa

    // Verificar cada 5 minutos (300000 ms)
    this.userCheckSubscription = timer(300000, 300000).pipe(
      switchMap(() => {
        const token = this.getToken();
        if (token && this.isLoggedIn()) {
          return this.http.get<any>(`${this.apiUrl}/verificar-estado-usuario`, {
            headers: { 'Authorization': `Bearer ${token}` }
          });
        }
        return [];
      })
    ).subscribe({
      next: (response) => {
        if (response && response.activo === false) {
          alert('Su cuenta ha sido desactivada. Será redirigido al inicio de sesión.');
          this.logout();
        }
      },
      error: (error) => {
        if (error.status === 403 && error.error?.userInactive) {
          alert('Su cuenta ha sido desactivada. Será redirigido al inicio de sesión.');
          this.logout();
        }
      }
    });
  }

  private stopUserStatusCheck(): void {
    if (this.userCheckSubscription) {
      this.userCheckSubscription.unsubscribe();
    }
  }

  // Método público para forzar verificación de estado
  checkUserStatus(): Observable<any> {
    const token = this.getToken();
    if (!token) {
      throw new Error('No token available');
    }

    return this.http.get<any>(`${this.apiUrl}/verificar-estado-usuario`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
  }
}
