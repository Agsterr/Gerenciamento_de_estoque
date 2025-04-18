import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Consumer } from '../models/consumer.model';
import { AuthService } from '../services/auth.service'; // Importa o AuthService
import { catchError } from 'rxjs/operators'; // Para capturar erros
import { throwError } from 'rxjs'; // Para criar o erro

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
    return this.http
      .get<Consumer[]>(this.apiUrl, { headers })
      .pipe(
        catchError(this.handleError) // Aqui tratamos qualquer erro ocorrido
      );
  }

  // Método para criar um novo consumidor
  criarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    const headers = this.getAuthHeaders();
    return this.http
      .post<Consumer>(this.apiUrl, consumidor, { headers })
      .pipe(
        catchError(this.handleError)
      );
  }

  // Método para editar um consumidor existente
  editarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    const headers = this.getAuthHeaders();
    return this.http
      .put<Consumer>(`${this.apiUrl}/${consumidor.id}`, consumidor, { headers })
      .pipe(
        catchError(this.handleError)
      );
  }

  // Método para deletar um consumidor pelo ID
  deletarConsumidor(id: number): Observable<void> {
    const headers = this.getAuthHeaders();
    return this.http
      .delete<void>(`${this.apiUrl}/${id}`, { headers })
      .pipe(
        catchError(this.handleError)
      );
  }

  // Função centralizada para tratamento de erros
  private handleError(error: any): Observable<never> {
    console.error('Ocorreu um erro:', error);
    return throwError(() => new Error('Erro ao realizar a requisição. Tente novamente.'));
  }
}

