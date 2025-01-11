import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ConsumidorService {
  private apiUrl = 'https://seu-backend.com/api/consumidores';

  constructor(private http: HttpClient) {}

  getConsumidores(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }
}
