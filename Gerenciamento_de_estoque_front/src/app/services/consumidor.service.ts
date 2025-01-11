// src/app/services/consumer.service.ts

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Defina a interface para representar o consumidor recebido da API
export interface Consumer {
  id: number;
  nome: string;
  cpf: string;
}

@Injectable({
  providedIn: 'root'
})
export class ConsumerService {
  // URL base para o endpoint de consumidores (ajuste conforme sua configuração)
  private baseUrl = 'http://localhost:8080/consumidores';

  constructor(private http: HttpClient) { }

  // Método para listar todos os consumidores
  getConsumers(): Observable<Consumer[]> {
    return this.http.get<Consumer[]>(this.baseUrl);
  }

  // Método para criar um novo consumidor (caso seja necessário futuramente)
  createConsumer(consumer: Consumer): Observable<Consumer> {
    return this.http.post<Consumer>(this.baseUrl, consumer);
  }

  // Método para deletar um consumidor pelo id
  deleteConsumer(id: number): Observable<any> {
    return this.http.delete(`${this.baseUrl}/${id}`);
  }
}

