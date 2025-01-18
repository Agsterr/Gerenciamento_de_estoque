// src/app/services/consumer.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Consumer } from '../models/consumer.model';
import { AuthService } from './auth.service'; // Importa o AuthService

@Injectable({
  providedIn: 'root',
})
export class ConsumidorService {
  private apiUrl = 'http://localhost:8080/consumidores'; // URL da API

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Gera os headers com o token JWT
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken'); // Obtém o token do localStorage
    if (!token) {
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Método para listar consumidores
  listarConsumidores(): Observable<Consumer[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<Consumer[]>(this.apiUrl, { headers });
  }

  // Método para criar um novo consumidor
  criarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    const headers = this.getAuthHeaders();
    return this.http.post<Consumer>(this.apiUrl, consumidor, { headers });
  }

  // Método para deletar um consumidor pelo ID
  deletarConsumidor(id: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.delete<string>(`${this.apiUrl}/${id}`, { headers });
  }
}
