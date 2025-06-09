// src/app/services/consumidor.service.ts

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Consumer } from '../models/consumer.model';

/**
 * Tipagem para resposta paginada, contendo conteúdo e metadados.
 */
export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // página atual
}

@Injectable({
  providedIn: 'root',
})
export class ConsumidorService {
  private apiUrl = 'http://localhost:8080/consumidores';

  constructor(private http: HttpClient) {}

  /**
   * Gera headers de autenticação com o token JWT presente no localStorage.
   */
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado. Faça login.');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  /**
   * Decodifica o token JWT e extrai o orgId do payload.
   */
  public getOrgId(): string {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado.');
    const payload = this.decodeJwt(token);
    if (payload && payload.org_id) return payload.org_id;
    throw new Error('OrgId não encontrado no token.');
  }

  /**
   * Faz o parse do JWT para JSON e retorna o payload.
   */
  private decodeJwt(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Token JWT inválido.');
    const payload = atob(parts[1]);
    return JSON.parse(payload);
  }

  /**
   * Lista consumidores com paginação, retornando apenas o array de Consumer.
   * @param page Número da página (padrão 0)
   * @param size Quantidade por página (padrão 10)
   */
  listarConsumidores(page: number = 0, size: number = 10): Observable<Consumer[]> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<{ content: Consumer[] }>(`${this.apiUrl}`, { headers, params })
      .pipe(
        catchError(error => {
          console.error('Erro ao listar consumidores:', error);
          return throwError(() => new Error('Falha ao carregar consumidores.'));
        }),
        map(response => response.content)
      );
  }

  /**
   * Lista consumidores com paginação e metadados completos.
   * @param page Número da página
   * @param size Quantidade por página
   */
  listarConsumidoresPaged(page: number = 0, size: number = 10): Observable<PagedResponse<Consumer>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PagedResponse<Consumer>>(this.apiUrl, { headers, params })
      .pipe(
        catchError(error => {
          console.error('Erro ao listar consumidores paginados:', error);
          return throwError(() => new Error('Falha ao carregar consumidores.'));
        })
      );
  }

  /**
   * Busca um consumidor específico pelo seu ID.
   * @param id ID do consumidor
   */
  getById(id: number): Observable<Consumer> {
    const headers = this.getAuthHeaders();
    return this.http.get<Consumer>(`${this.apiUrl}/${id}`, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao buscar consumidor por ID:', error);
        return throwError(() => new Error('Falha ao carregar consumidor.'));
      })
    );
  }

  /**
   * Cria um novo consumidor no backend.
   * @param consumidor Dados do novo consumidor
   */
  criarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    const headers = this.getAuthHeaders();
    const body = { ...consumidor, orgId: this.getOrgId() };
    return this.http.post<Consumer>(`${this.apiUrl}`, body, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao criar consumidor:', error);
        return throwError(() => new Error('Erro ao criar consumidor.'));
      })
    );
  }

  /**
   * Atualiza os dados de um consumidor existente.
   * @param consumidor Objeto parcial contendo ID e campos a atualizar
   */
  editarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    if (!consumidor.id) {
      return throwError(() => new Error('ID do consumidor ausente.'));
    }
    const headers = this.getAuthHeaders();
    const body = { ...consumidor, orgId: this.getOrgId() };
    return this.http.put<Consumer>(`${this.apiUrl}/${consumidor.id}`, body, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao editar consumidor:', error);
        return throwError(() => new Error('Erro ao editar consumidor.'));
      })
    );
  }

  /**
   * Exclui um consumidor pelo ID.
   * @param id ID do consumidor que será removido
   */
  deletarConsumidor(id: number): Observable<void> {
    const headers = this.getAuthHeaders();
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao deletar consumidor:', error);
        return throwError(() => new Error('Erro ao deletar consumidor.'));
      })
    );
  }
}
