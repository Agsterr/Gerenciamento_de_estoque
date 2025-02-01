import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Entrega } from '../models/entrega.model';
import { PageEntregaResponse } from '../models/PageEntregaResponse.model';
import { EntregaRequest } from '../models/EntregaRequest.model';
import { ApiResponse } from '../models/api-response.model';  // Certifique-se de que a interface ApiResponse está correta
import { AuthService } from './auth.service'; // Serviço de autenticação

@Injectable({
  providedIn: 'root',
})
export class EntregasService {
  private apiUrl = 'http://localhost:8080/entregas'; // URL da API

  constructor(private http: HttpClient, private authService: AuthService) {}

  /**
   * Gera os headers com o token JWT
   * @returns HttpHeaders
   */
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken'); // Obtém o token do localStorage
    if (!token) {
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }
    return new HttpHeaders()
      .set('Authorization', `Bearer ${token}`)
      .set('Content-Type', 'application/json'); // Define o Content-Type como application/json
  }

  /**
   * Lista entregas com paginação.
   * @param page Número da página.
   * @param size Quantidade de itens por página.
   * @returns Observable<PageEntregaResponse>
   */
  listarEntregas(page: number, size: number): Observable<PageEntregaResponse> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageEntregaResponse>(this.apiUrl, {
      headers: this.getAuthHeaders(),
      params,
    });
  }

  /**
   * Cria uma nova entrega.
   * @param entrega Dados da nova entrega.
   * @returns Observable<ApiResponse> - Agora retornando ApiResponse
   */
  criarEntrega(entrega: Partial<EntregaRequest>): Observable<ApiResponse> {
    return this.http.post<ApiResponse>(this.apiUrl, entrega, {
      headers: this.getAuthHeaders(),
      observe: 'body', // Para observar o corpo da resposta
    });
  }

  /**
   * Deleta uma entrega pelo ID.
   * @param id ID da entrega a ser deletada.
   * @returns Observable<ApiResponse> - Agora retornando ApiResponse
   */
  deletarEntrega(id: number): Observable<ApiResponse> {
    return this.http.delete<ApiResponse>(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders(),
      observe: 'body', // Para observar o corpo da resposta
    });
  }
}
