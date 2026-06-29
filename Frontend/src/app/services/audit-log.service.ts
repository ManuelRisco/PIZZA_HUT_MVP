import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { AuditLog } from '../models/audit-log.model';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class AuditLogService {
  private apiUrl = `${environment.apiUrl}/audit-logs`;

  constructor(private http: HttpClient, private authService: AuthService) { }

  private get httpOptions() {
    return {
      headers: new HttpHeaders({
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.authService.getToken()}`
      })
    };
  }

  listarTodos(): Observable<AuditLog[]> {
    return this.http.get<any>(this.apiUrl, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }

  listarRecientes(horas: number = 24): Observable<AuditLog[]> {
    return this.http.get<any>(`${this.apiUrl}/recientes?horas=${horas}`, this.httpOptions).pipe(
      map(res => res.data !== undefined ? res.data : res)
    );
  }
}
