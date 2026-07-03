import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface AddressDTO {
  id?: number;
  userId: number;
  line1: string;
  city: string;
  district: string;
  reference?: string;
  isDefault?: boolean;
  createdAt?: string;
}

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly apiUrl = `${environment.apiUrl}/addresses`;

  constructor(private readonly http: HttpClient) {}

  listarTodos(): Observable<AddressDTO[]> {
    return this.http.get<any>(this.apiUrl).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorId(id: number): Observable<AddressDTO> {
    return this.http.get<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  obtenerPorUserId(userId: number): Observable<AddressDTO[]> {
    return this.http.get<any>(`${this.apiUrl}/user/${userId}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  crear(address: AddressDTO): Observable<AddressDTO> {
    return this.http.post<any>(this.apiUrl, address).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  actualizar(id: number, address: AddressDTO): Observable<AddressDTO> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, address).pipe(map(res => res.data !== undefined ? res.data : res));
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`).pipe(map(res => res.data !== undefined ? res.data : res));
  }
}

