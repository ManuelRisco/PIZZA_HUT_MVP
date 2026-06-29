import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { SessionLog } from '../models/session-log.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class SessionLogService {
  private apiUrl = `${environment.apiUrl}/sessions`;

  constructor(private http: HttpClient, private authService: AuthService) { }

  private get httpOptions() {
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.authService.getToken()}`
      })
    };
  }

  listarSesionesActivas(): Observable<SessionLog[]> {
    return this.http.get<any>(`${this.apiUrl}/activas`, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  listarSesionesLargas(horas: number = 12): Observable<SessionLog[]> {
    return this.http.get<any>(`${this.apiUrl}/largas?horas=${horas}`, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  cerrarSesionExterna(sessionToken: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/cerrar/${sessionToken}?reason=CLOSED_BY_ADMIN`, {}, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }
}
