import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Entrega } from '../models/entrega.model';
import { PageEntregaResponse } from '../models/PageEntregaResponse.model';
import { EntregaRequest } from '../models/EntregaRequest.model';
import { AuthService } from './auth.service'; // Serviço de autenticação

@Injectable({
  providedIn: 'root',
})
export class EntregasService {
  private apiUrl = 'http://localhost:8080/entregas'; // URL da API

  constructor(private http: HttpClient, private authService: AuthService) {}

  // Gera os headers com o token JWT
  private getAuthHeaders() {
    const token = localStorage.getItem('jwtToken'); // Obtém o token do localStorage
    if (!token) {
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
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
   * @returns Observable<Entrega>
   */
  criarEntrega(entrega: Partial<EntregaRequest>): Observable<Entrega> {
    return this.http.post<Entrega>(this.apiUrl, entrega, {
      headers: this.getAuthHeaders(),
      observe: 'body', // Para observar o corpo da resposta
    });
  }

  /**
   * Deleta uma entrega pelo ID.
   * @param id ID da entrega a ser deletada.
   * @returns Observable<any>
   */
  deletarEntrega(id: number): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}`, {
      headers: this.getAuthHeaders(),
      observe: 'body', // Para observar o corpo da resposta
    });
  }
}
