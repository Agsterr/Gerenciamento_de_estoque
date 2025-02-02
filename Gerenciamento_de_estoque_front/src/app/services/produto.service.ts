



import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Produto } from '../models/produto.model';

@Injectable({
  providedIn: 'root',
})
export class ProdutoService {
  private apiUrl = 'http://localhost:8080/produtos'; // URL da API

  constructor(private http: HttpClient) {}

  // Gera os headers com o token JWT
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken'); // Obtém o token do localStorage
    if (!token) {
      // Retorna um erro controlado caso o token não esteja presente
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Método para listar produtos (com paginação)
  listarProdutos(page: number = 0, size: number = 100): Observable<any> { // Aumentado para 100
    const headers = this.getAuthHeaders();
    return this.http.get<any>(`${this.apiUrl}?page=${page}&size=${size}`, { headers });
  }

  // Método para obter detalhes de um produto por ID
  getProdutoById(produtoId: number): Observable<Produto> {
    const headers = this.getAuthHeaders();
    return this.http.get<Produto>(`${this.apiUrl}/${produtoId}`, { headers });
  }

  // Método para criar um novo produto
  criarProduto(produto: Partial<Produto>): Observable<Produto> {
    const headers = this.getAuthHeaders();
    return this.http.post<Produto>(this.apiUrl, produto, { headers });
  }

  // Método para atualizar a quantidade do produto via PATCH
  atualizarProdutoQuantidade(produtoId: number, quantidade: number): Observable<Produto> {
    const headers = this.getAuthHeaders();
    return this.http.patch<Produto>(`${this.apiUrl}/${produtoId}/quantidade`, { quantidade }, { headers });
  }

  // Método para deletar um produto pelo ID
  deletarProduto(produtoId: number): Observable<string> {
    const headers = this.getAuthHeaders();
    return this.http.delete<string>(`${this.apiUrl}/${produtoId}`, { headers });
  }
}
