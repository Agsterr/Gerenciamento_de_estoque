// src/app/services/consumer.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Consumer } from '../models/consumer.model'; // Importe a interface Consumer

@Injectable({
  providedIn: 'root',
})
export class ConsumidorService {
  private apiUrl = 'http://localhost:8080/consumidores'; // URL da API

  constructor(private http: HttpClient) {}

  // Método para listar consumidores
  listarConsumidores(): Observable<Consumer[]> {
    return this.http.get<Consumer[]>(this.apiUrl);
  }

  // Método para criar um novo consumidor
  criarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    return this.http.post<Consumer>(this.apiUrl, consumidor);
  }

  // Método para deletar um consumidor pelo ID
  deletarConsumidor(id: number): Observable<string> {
    return this.http.delete<string>(`${this.apiUrl}/${id}`);
  }
}


