
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { catchError, throwError } from 'rxjs';
import { Categoria } from '../models/categoria.model';
import { ApiResponse } from '../models/api-response.model';

@Injectable({
  providedIn: 'root',
})
export class CategoriaService {
  private apiUrl = 'http://localhost:8080/categorias'; // URL da API

  constructor(private http: HttpClient) {}

  // Gera os headers com o token JWT
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken'); // Obtém o token do localStorage
    if (!token) {
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Método para listar categorias
  listarCategorias(): Observable<Categoria[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<Categoria[]>(this.apiUrl, { headers }).pipe(
      catchError((error) => {
        console.error('Erro ao listar categorias:', error);
        return throwError('Falha ao carregar categorias, por favor, tente novamente.');
      })
    );
  }

  // Método para criar uma nova categoria
  criarCategoria(nome: string): Observable<ApiResponse> {
    const headers = this.getAuthHeaders();
    const body = { nome };

    return this.http.post<ApiResponse>(this.apiUrl, body, { headers }).pipe(
      catchError((error) => {
        console.error('Erro ao criar categoria:', error);
        return throwError('Falha ao criar categoria, tente novamente.');
      })
    );
  }

  // Método para deletar uma categoria (agora retorna a categoria deletada)
  deletarCategoria(id: number): Observable<Categoria> {
    const headers = this.getAuthHeaders();
    return this.http.delete<Categoria>(`${this.apiUrl}/${id}`, { headers }).pipe(
      catchError((error) => {
        console.error('Erro ao deletar categoria:', error);
        if (error.status === 404) {
          return throwError('Categoria não encontrada.');
        }
        return throwError('Falha ao deletar categoria, tente novamente.');
      })
    );
  }
}
