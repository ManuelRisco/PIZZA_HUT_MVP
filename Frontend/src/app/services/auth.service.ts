import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, timer, Subscription } from 'rxjs';
import { tap, switchMap, map } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthResponseDTO, LoginDTO, UsuarioDTO } from '../models/usuario.interface';
import { environment } from '../../environments/environment';

// Alias para compatibilidad
export interface AuthResponse extends AuthResponseDTO {}
export interface User extends UsuarioDTO {}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly apiUrl= environment.apiUrl;
  private readonly currentUserSubject= new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private readonly needsPasswordSetupSubject = new BehaviorSubject<boolean>(false);
  public needsPasswordSetup$ = this.needsPasswordSetupSubject.asObservable();
  private refreshTokenSubscription?: Subscription;
  private userCheckSubscription?: Subscription;

  constructor(private readonly http: HttpClient, private readonly router: Router, private readonly ngZone: NgZone) {
    // Verificar si hay un token almacenado al inicializar
    this.loadUserFromToken();
    this.startTokenRefresh();
    this.startUserStatusCheck();
    this.listenToStorageEvents();
  }

  // Sincronizar login y logout entre pesta\u00f1as
  private listenToStorageEvents(): void {
    window.addEventListener('storage', (event) => {
      // Si el token se elimin\u00f3 en otra pesta\u00f1a (por ej. al hacer logout o cambiar contrase\u00f1a)
      if (event.key === 'token' && !event.newValue) {
        this.ngZone.run(() => {
          this.currentUserSubject.next(null);
          this.stopTokenRefresh();
          this.stopUserStatusCheck();
          this.router.navigate(['/join']);
        });
      }
      
      // Si el token se A\u00d1ADI\u00d3 en otra pesta\u00f1a (por ej. al hacer login)
      if (event.key === 'token' && event.newValue) {
        this.ngZone.run(() => {
          // Recargar el usuario desde el nuevo token
          this.loadUserFromToken();
          
          // Si estamos en la p\u00e1gina de login (/join), redirigir al panel correspondiente
          if (this.router.url.includes('/join') || this.router.url === '/') {
             const currentUser = this.currentUserSubject.value;
             if (currentUser) {
               if (currentUser.role === 'ADMIN') {
                 this.router.navigate(['/panel-admin']);
               } else {
                 this.router.navigate(['/menu']);
               }
             }
          }
        });
      }
    });
  }

  login(email: string, password: string): Observable<any> {
    // Limpiar cualquier sesi\u00f3n anterior antes de iniciar sesi\u00f3n
    this.clearSession();

    const loginData: LoginDTO = { email, password };
    return this.http.post<any>(`${this.apiUrl}/ingresar`, loginData).pipe(
      map(res => res.data !== undefined ? res.data : res),
      tap(response => {
        if (response.token) {
          this.setSession(response, false);
          this.startTokenRefresh();
          this.startUserStatusCheck();
        }
      })
    );
  }

  obtenerConfiguracionGoogle(): Observable<{ clientId: string; googleCloudConfigured: boolean; allowQuickGoogleSignIn: boolean }> {
    return this.http.get<any>(`${this.apiUrl}/auth/google/config`).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  loginConGoogle(payload: { credential?: string; accessToken?: string; email?: string; name?: string }): Observable<any> {
    this.clearSession();
    return this.http.post<any>(`${this.apiUrl}/auth/google`, payload).pipe(
      map(res => res.data !== undefined ? res.data : res),
      tap(response => {
        if (response.token) {
          this.setSession(response, true);
          this.startTokenRefresh();
          this.startUserStatusCheck();
        }
      })
    );
  }

  vincularCuentaConGoogle(payload: { credential?: string; accessToken?: string } = {}): Observable<UsuarioDTO> {
    return this.http.post<any>(`${this.apiUrl}/usuarios/me/vincular-google`, payload).pipe(
      map(res => res.data !== undefined ? res.data : res),
      tap((usuarioActualizado: UsuarioDTO) => {
        localStorage.setItem('authProvider', 'GOOGLE');
        localStorage.setItem('userGoogleLinked', 'true');
        if (usuarioActualizado) {
          this.updateCurrentUser({
            ...usuarioActualizado,
            googleLinked: true,
            authProvider: 'GOOGLE'
          });
        }
      })
    );
  }

  isGoogleLinked(usuario?: Partial<UsuarioDTO> | null): boolean {
    if (usuario?.googleLinked === true || usuario?.authProvider === 'GOOGLE') {
      return true;
    }
    const current = this.currentUserSubject.value;
    if (current?.googleLinked === true || current?.authProvider === 'GOOGLE') {
      return true;
    }
    return localStorage.getItem('userGoogleLinked') === 'true' || localStorage.getItem('authProvider') === 'GOOGLE';
  }

  hasPasswordDefined(usuario?: Partial<UsuarioDTO> | null): boolean {
    if (usuario && typeof usuario.passwordSet === 'boolean') {
      return usuario.passwordSet;
    }
    const current = this.currentUserSubject.value;
    if (current && typeof current.passwordSet === 'boolean') {
      return current.passwordSet;
    }
    return localStorage.getItem('userPasswordSet') !== 'false';
  }

  openPasswordSetupModal(): void {
    this.needsPasswordSetupSubject.next(true);
  }

  dismissPasswordSetupModal(): void {
    sessionStorage.setItem('googlePasswordPromptDismissed', 'true');
    this.needsPasswordSetupSubject.next(false);
  }

  definirPasswordCuentaGoogle(password: string): Observable<UsuarioDTO> {
    return this.http.post<any>(`${this.apiUrl}/usuarios/me/definir-password`, { password }).pipe(
      map(res => res.data !== undefined ? res.data : res),
      tap((usuarioActualizado: UsuarioDTO) => {
        localStorage.setItem('userPasswordSet', 'true');
        this.needsPasswordSetupSubject.next(false);
        if (usuarioActualizado) {
          this.updateCurrentUser({
            ...usuarioActualizado,
            passwordSet: true,
            googleLinked: true,
            authProvider: 'GOOGLE'
          });
        }
      })
    );
  }

  solicitarRecuperacionPassword(email: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/recuperar-password/solicitar`, { email }).pipe(
      map(res => ({
        ...(res.data !== undefined ? res.data : res),
        message: res.message
      }))
    );
  }

  verificarCodigoRecuperacion(email: string, code: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/recuperar-password/verificar-codigo`, { email, code }).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  confirmarRecuperacionPassword(email: string, code: string, newPassword: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/recuperar-password/confirmar`, { email, code, newPassword }).pipe(
      map(res => ({
        ...(res.data !== undefined ? res.data : res),
        message: res.message
      }))
    );
  }

  private setSession(authResponse: any, forceGoogle: boolean = false): void {
    localStorage.setItem('token', authResponse.token);
    localStorage.setItem('refreshToken', authResponse.refreshToken);

    const usuario = authResponse.usuario;
    if (!usuario) {
      this.logout();
      throw new Error('Respuesta de login sin usuario');
    }

    const isGoogle = forceGoogle || usuario.googleLinked === true || usuario.authProvider === 'GOOGLE';
    const hasPassword = usuario.passwordSet !== false && authResponse.requiresPasswordSetup !== true;
    localStorage.setItem('authProvider', isGoogle ? 'GOOGLE' : 'LOCAL');
    localStorage.setItem('userGoogleLinked', isGoogle ? 'true' : 'false');
    localStorage.setItem('userPasswordSet', hasPassword ? 'true' : 'false');

    this.currentUserSubject.next({
      id: usuario.id,
      email: usuario.email,
      name: usuario.name,
      role: usuario.role,
      phone: usuario.phone,
      googleLinked: isGoogle,
      authProvider: isGoogle ? 'GOOGLE' : (usuario.authProvider || 'LOCAL'),
      passwordSet: hasPassword
    });

    if (isGoogle && !hasPassword && sessionStorage.getItem('googlePasswordPromptDismissed') !== 'true') {
      this.needsPasswordSetupSubject.next(true);
    } else {
      this.needsPasswordSetupSubject.next(false);
    }
  }

  logout(): void {
    // Limpiar TODAS las claves relacionadas con autenticaci\u00f3n
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
    sessionStorage.removeItem('googlePasswordPromptDismissed');
    this.needsPasswordSetupSubject.next(false);
    this.currentUserSubject.next(null);

    // Detener procesos de refresh y verificaci\u00f3n
    this.stopTokenRefresh();
    this.stopUserStatusCheck();

    // Redirigir a login
    this.router.navigate(['/join']);
  }

  // M\u00e9todo privado para limpiar sesi\u00f3n sin redirigir (usado en login)
  private clearSession(): void {
    // Limpiar TODAS las claves relacionadas con autenticaci\u00f3n
    const keysToRemove = ['token', 'refreshToken', 'currentUser', 'userRole', 'userId', 'userEmail', 'userName'];
    keysToRemove.forEach(key => {
      localStorage.removeItem(key);
      sessionStorage.removeItem(key);
    });

    // Tambi\u00e9n limpiar cualquier clave que empiece con 'auth' o 'user'
    for (let i = 0; i < localStorage.length; i++) {
      const key = localStorage.key(i);
      if (key && (key.startsWith('auth') || key.startsWith('user') || key.startsWith('token'))) {
        localStorage.removeItem(key);
      }
    }

    sessionStorage.removeItem('googlePasswordPromptDismissed');
    this.needsPasswordSetupSubject.next(false);
    this.currentUserSubject.next(null);
    this.stopTokenRefresh();
    this.stopUserStatusCheck();
  }

  // M\u00e9todo p\u00fablico para limpiar sesi\u00f3n sin redirigir (usado por interceptor)
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

  // M\u00e9todo para actualizar la informaci\u00f3n del usuario actual
  updateCurrentUser(usuario: UsuarioDTO): void {
    const currentUser = this.getCurrentUser();
    if (currentUser && usuario.id === currentUser.id) {
      const isGoogle = Boolean(
        usuario.googleLinked === true ||
        usuario.authProvider === 'GOOGLE' ||
        currentUser.googleLinked === true ||
        currentUser.authProvider === 'GOOGLE' ||
        localStorage.getItem('userGoogleLinked') === 'true'
      );
      if (isGoogle) {
        localStorage.setItem('authProvider', 'GOOGLE');
        localStorage.setItem('userGoogleLinked', 'true');
      }
      const hasPassword = typeof usuario.passwordSet === 'boolean'
        ? usuario.passwordSet
        : (typeof currentUser.passwordSet === 'boolean' ? currentUser.passwordSet : localStorage.getItem('userPasswordSet') !== 'false');
      localStorage.setItem('userPasswordSet', hasPassword ? 'true' : 'false');

      this.currentUserSubject.next({
        id: usuario.id,
        email: usuario.email,
        name: usuario.name,
        role: usuario.role,
        phone: usuario.phone,
        googleLinked: isGoogle,
        authProvider: isGoogle ? 'GOOGLE' : (usuario.authProvider || 'LOCAL'),
        passwordSet: hasPassword
      });

      if (isGoogle && !hasPassword && sessionStorage.getItem('googlePasswordPromptDismissed') !== 'true') {
        this.needsPasswordSetupSubject.next(true);
      }
    }
  }

  // M\u00e9todo para obtener informaci\u00f3n completa del usuario desde el token
  getUserInfo(): { email: string, name: string, role: string } | null {
    const tokenData = this.getTokenData();
    if (!tokenData) return null;

    return {
      email: tokenData.email,
      name: tokenData.name,
      role: tokenData.role
    };
  }

  // M\u00e9todo para verificar si el usuario actual es el mismo que est\u00e1 en el token
  validateUserIntegrity(): boolean {
    const currentUser = this.getCurrentUser();
    const tokenData = this.getTokenData();

    if (!currentUser || !tokenData) return false;

    return currentUser.email === tokenData.email &&
           currentUser.name === tokenData.name &&
           currentUser.role === tokenData.role;
  }

  // M\u00e9todo para obtener datos del token de manera segura
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

    // Si el token est\u00e1 expirado, limpiar la sesi\u00f3n inmediatamente
    if (this.isTokenExpired()) {
      this.clearSession();
      return;
    }

    const tokenData = this.getTokenData();
    if (tokenData?.email && tokenData.name && tokenData.role) {
      const isGoogle = localStorage.getItem('userGoogleLinked') === 'true' || localStorage.getItem('authProvider') === 'GOOGLE';
      const hasPassword = localStorage.getItem('userPasswordSet') !== 'false';
      this.currentUserSubject.next({
        id: tokenData.id,
        email: tokenData.email,
        name: tokenData.name,
        role: tokenData.role,
        phone: tokenData.phone,
        googleLinked: isGoogle,
        authProvider: isGoogle ? 'GOOGLE' : 'LOCAL',
        passwordSet: hasPassword
      });
    } else {
      this.clearSession();
    }
  }

  // M\u00e9todo para decodificar JWT (b\u00e1sico, sin validaci\u00f3n de firma)
  private decodeToken(token: string): any {
    try {
      const payload = token.split('.')[1];
      const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=');
      return JSON.parse(atob(padded));
    } catch (error) {
      return null;
    }
  }

  // Verificar si el token ha expirado
  isTokenExpired(): boolean {
    const token = this.getToken();
    if (!token) return true;

    const decoded = this.decodeToken(token);
    if (!decoded?.exp) return true;

    const currentTime = Math.floor(Date.now() / 1000);
    return decoded.exp < currentTime;
  }

  // M\u00e9todo para debugging - mostrar contenido del token (solo en desarrollo)
  // NOTA: Solo usar en desarrollo, no en producci\u00f3n
  debugTokenInfo(): void {
    const tokenData = this.getTokenData();
    if (!tokenData) {
      // No token found
    }
  }

  // Refresh Token - Renovar token autom\u00e1ticamente
  refreshToken(): Observable<any> {
    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    return this.http.post<any>(`${this.apiUrl}/refresh-token`, { refreshToken }).pipe(
      map(res => res.data !== undefined ? res.data : res),
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

  // Iniciar refresh autom\u00e1tico del token cada 15 minutos
  private startTokenRefresh(): void {
    this.stopTokenRefresh(); // Detener cualquier subscripci\u00f3n previa

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
        // Token renovado
      },
      error: (error) => {
        console.error('Error al renovar token:', error);
        if (error.error?.userInactive) {
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

  // Verificar estado del usuario y token expirado cada 60 segundos
  private startUserStatusCheck(): void {
    this.stopUserStatusCheck(); // Detener cualquier subscripci\u00f3n previa

    // Verificar cada 60 segundos (60000 ms)
    this.userCheckSubscription = timer(60000, 60000).pipe(
      switchMap(() => {
        const token = this.getToken();
        if (!token) {
          return [];
        }
        
        if (this.isTokenExpired()) {
          this.logout();
          return [];
        }

        if (this.isLoggedIn()) {
          return this.http.get<any>(`${this.apiUrl}/verificar-estado-usuario`, {
            headers: { 'Authorization': `Bearer ${token}` }
          }).pipe(map(res => res.data !== undefined ? res.data : res));
        }
        return [];
      })
    ).subscribe({
      next: (response) => {
        // If it unwrapped a boolean or an object with activo
        const isInactive = (typeof response === 'boolean') ? !response : (response?.activo === false);
        if (isInactive) {
          this.logout();
        }
      },
      error: (error) => {
        if (error.status === 401 || error.status === 403) {
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

  // M\u00e9todo p\u00fablico para forzar verificaci\u00f3n de estado
  checkUserStatus(): Observable<any> {
    const token = this.getToken();
    if (!token) {
      throw new Error('No token available');
    }

    return this.http.get<any>(`${this.apiUrl}/verificar-estado-usuario`, {
      headers: { 'Authorization': `Bearer ${token}` }
    }).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}


