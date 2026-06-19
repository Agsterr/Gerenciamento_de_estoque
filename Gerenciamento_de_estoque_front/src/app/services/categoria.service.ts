import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Categoria } from '../models/categoria.model';
import { PageCategoriaResponse } from '../models/page-categoria-response.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class CategoriaService {
  private apiUrl = `${environment.apiUrl}/categorias`;

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  public getOrgId(): string {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado.');
    const payload = this.decodeJwt(token);
    if (payload && payload.org_id) return payload.org_id;
    throw new Error('OrgId não encontrado no token.');
  }

  private decodeJwt(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Token JWT inválido.');
    const payload = atob(parts[1]);
    return JSON.parse(payload);
  }

  listarCategorias(page: number = 0, size: number = 10): Observable<PageCategoriaResponse> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageCategoriaResponse>(this.apiUrl, { headers, params }).pipe(
      catchError(error => {
        console.error('Erro ao listar categorias paginadas:', error);
        return throwError(() => new Error('Falha ao carregar categorias paginadas.'));
      })
    );
  }

  criarCategoria(nome: string, descricao: string = ''): Observable<Categoria> {
    const headers = this.getAuthHeaders();
    const body = { nome, descricao };

    return this.http.post<Categoria>(this.apiUrl, body, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao criar categoria:', error);
        return throwError(() => new Error('Falha ao criar categoria.'));
      })
    );
  }

  atualizarCategoria(id: number, nome: string, descricao: string = ''): Observable<Categoria> {
    const headers = this.getAuthHeaders();
    const body = { nome, descricao };

    return this.http.put<Categoria>(`${this.apiUrl}/${id}`, body, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao atualizar categoria:', error);
        return throwError(() => new Error('Falha ao atualizar categoria.'));
      })
    );
  }

  deletarCategoria(id: number): Observable<void> {
    const headers = this.getAuthHeaders();

    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao deletar categoria:', error);
        return throwError(() => new Error('Falha ao deletar categoria.'));
      })
    );
  }
}
