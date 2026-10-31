import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../environments/environment';

export interface UploadImageResponse {
  imageUrl: string;
  thumbnailUrl: string;
}

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {
  private readonly apiUrl = `${environment.apiUrl}/uploads`;

  constructor(private readonly http: HttpClient) {}

  uploadPizzaImage(file: File): Observable<UploadImageResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.apiUrl}/pizzas`, formData).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  uploadExtraImage(file: File): Observable<UploadImageResponse> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.apiUrl}/extras`, formData).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }

  deleteImage(imageUrl: string): Observable<void> {
    const params = new HttpParams().set('url', imageUrl);
    return this.http.delete<any>(this.apiUrl, { params }).pipe(
      map(res => (res.data !== undefined ? res.data : res))
    );
  }
}

