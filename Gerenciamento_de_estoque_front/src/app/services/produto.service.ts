
import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Produto } from '../models/produto.model'; // Modelo de Produto que será usado no frontend
import { map } from 'rxjs/operators';
import { Categoria } from '../models/categoria.model'; // Adicione esta linha


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
      // Se o token não for encontrado, gera um erro controlado
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }
    // Retorna os headers com o token JWT
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Método para obter o orgId do token JWT
  private getOrgId(): string {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      console.error('Token não encontrado. O usuário precisa estar autenticado.');
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }

    // Decode o token para extrair o orgId
    const payload = this.decodeJwt(token);
    if (payload && payload.org_id) {
      return payload.org_id;  // Retorna o orgId extraído do token
    }

    throw new Error('OrgId não encontrado no token.');
  }

  // Função para decodificar o JWT
  private decodeJwt(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) {
      throw new Error('Token JWT inválido.');
    }

    const payload = atob(parts[1]);
    return JSON.parse(payload);
  }

  // Método para listar produtos (com paginação)
 
  listarProdutos(page: number = 0, size: number = 100): Observable<any> {
    const orgId = this.getOrgId();  // Obtém o orgId do localStorage ou do token
    const headers = this.getAuthHeaders(); // Obtém os headers com o token
  
    return this.http.get<any>(`${this.apiUrl}/${orgId}?page=${page}&size=${size}`, { headers }).pipe(
      map((response: any) => {
        return {
          content: response.content.map((produto: any) => ({
            ...produto,
            categoria: new Categoria(
              produto.categoriaId,     // ID da categoria
              produto.categoriaNome,   // Mapeando a categoriaNome para o campo nome da Categoria
              produto.descricao,       // Descrição
              produto.criadoEm,        // Data de criação
              produto.orgId           // ID da organização
            )
          })),
          totalPages: response.totalPages,
          currentPage: response.number,
        };
      })
    );
  }
  
   
  
  
  

  // Método para obter detalhes de um produto por ID
  getProdutoById(produtoId: number): Observable<Produto> {
    const orgId = this.getOrgId();  // Obtém o orgId do localStorage ou do token
    const headers = this.getAuthHeaders(); // Obtém os headers com o token
    return this.http.get<Produto>(`${this.apiUrl}/${produtoId}/${orgId}`, { headers });
  }

 // ProdutoService
criarProduto(produto: Produto): Observable<Produto> {
  const orgId = this.getOrgId();  // Obtém o orgId do localStorage
  const headers = this.getAuthHeaders(); // Obtém os headers com o token

  // Adicionando o orgId nos cabeçalhos (cabeçalhos de autorização já incluem o token)
  const updatedHeaders = headers.set('orgId', orgId.toString());  // Envia o orgId no cabeçalho

  // Envia o corpo da requisição com o produto
  const body = { ...produto };  // Não precisamos adicionar o orgId aqui, pois já está no cabeçalho

  return this.http.post<Produto>(`${this.apiUrl}/${orgId}`, body, { headers: updatedHeaders });
}


  // Método para atualizar um produto via PUT
  atualizarProduto(produto: Produto, id: number): Observable<Produto> {
    const orgId = this.getOrgId();  // Obtém o orgId do localStorage
    const headers = this.getAuthHeaders();  // Obtém os headers com o token

    // Adicionando o orgId no corpo da requisição
    const body = { ...produto, orgId };

    return this.http.put<Produto>(`${this.apiUrl}/${id}/${orgId}`, body, { headers });
  }

  // Método para atualizar a quantidade do produto
  atualizarProdutoQuantidade(produtoId: number, quantidade: number): Observable<Produto> {
    const orgId = this.getOrgId(); // Obtém o orgId do localStorage
    const headers = this.getAuthHeaders(); // Obtém os headers com o token
    const body = { quantidade }; // Envia a quantidade no corpo da requisição
    return this.http.patch<Produto>(`${this.apiUrl}/${produtoId}/${orgId}/quantidade`, body, { headers });
  }

  // Método para deletar um produto pelo ID
  deletarProduto(produtoId: number): Observable<string> {
    const orgId = this.getOrgId(); // Obtém o orgId do localStorage
    const headers = this.getAuthHeaders(); // Obtém os headers de autenticação

    return this.http.delete<string>(`${this.apiUrl}/${produtoId}/${orgId}`, { headers });
  }
}
