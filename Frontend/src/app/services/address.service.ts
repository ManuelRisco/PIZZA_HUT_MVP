import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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
  private readonly apiUrl = 'http://localhost:8089/api/addresses';

  constructor(private http: HttpClient) {}

  listarTodos(): Observable<AddressDTO[]> {
    return this.http.get<AddressDTO[]>(this.apiUrl);
  }

  obtenerPorId(id: number): Observable<AddressDTO> {
    return this.http.get<AddressDTO>(`${this.apiUrl}/${id}`);
  }

  obtenerPorUserId(userId: number): Observable<AddressDTO[]> {
    return this.http.get<AddressDTO[]>(`${this.apiUrl}/user/${userId}`);
  }

  crear(address: AddressDTO): Observable<AddressDTO> {
    return this.http.post<AddressDTO>(this.apiUrl, address);
  }

  actualizar(id: number, address: AddressDTO): Observable<AddressDTO> {
    return this.http.put<AddressDTO>(`${this.apiUrl}/${id}`, address);
  }

  eliminar(id: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }
}
