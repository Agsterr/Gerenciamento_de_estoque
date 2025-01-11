
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root', // Garantindo que o serviço esteja disponível globalmente
})
export class ProdutoService {
  private apiUrl = 'http://localhost:8080/api/produtos';  // URL da sua API

  constructor(private http: HttpClient) {}

  // Método para buscar todos os produtos
  getProdutos(): Observable<any[]> {
    return this.http.get<any[]>(this.apiUrl);
  }

  // Método para buscar detalhes de um produto
  getProdutoDetalhado(produtoId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/${produtoId}`);
  }
}
