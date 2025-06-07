import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { Categoria } from '../models/categoria.model';
import { catchError, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class CategoriaService {
  private apiUrl = 'http://localhost:8080/categorias';

  constructor(private http: HttpClient) {}

  /** Recupera o token e define os headers de autenticação */
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  /** Extrai o orgId do JWT armazenado */
  public getOrgId(): string {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado.');
    const payload = this.decodeJwt(token);
    if (payload && payload.org_id) return payload.org_id;
    throw new Error('OrgId não encontrado no token.');
  }

  /** Decodifica o payload do JWT */
  private decodeJwt(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Token JWT inválido.');
    const payload = atob(parts[1]);
    return JSON.parse(payload);
  }

  /** Lista categorias da organização atual */
  listarCategorias(): Observable<Categoria[]> {
    const orgId = this.getOrgId();
    const headers = this.getAuthHeaders();

    return this.http.get<any>(`${this.apiUrl}/${orgId}`, { headers }).pipe(
      map(response => response.content as Categoria[]),
      catchError(error => {
        console.error('Erro ao listar categorias:', error);
        return throwError(() => new Error('Falha ao carregar categorias.'));
      })
    );
  }

  /** Cria uma nova categoria com nome e descrição */
  criarCategoria(nome: string, descricao: string = ''): Observable<Categoria> {
    const orgId = this.getOrgId();
    const headers = this.getAuthHeaders();

    const body = { nome, descricao };

    return this.http.post<Categoria>(`${this.apiUrl}/${orgId}`, body, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao criar categoria:', error);
        return throwError(() => new Error('Falha ao criar categoria.'));
      })
    );
  }

  /** Deleta uma categoria pelo ID e orgId */
  deletarCategoria(id: number): Observable<void> {
    const orgId = this.getOrgId();
    const headers = this.getAuthHeaders();

    return this.http.delete<void>(`${this.apiUrl}/${orgId}/${id}`, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao deletar categoria:', error);
        return throwError(() => new Error('Falha ao deletar categoria.'));
      })
    );
  }
}
