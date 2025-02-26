

import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Categoria } from '../models/categoria.model';
import { ApiResponse } from '../models/api-response.model';
import { catchError, map } from 'rxjs/operators'; // Importando 'catchError' e 'map' corretamente
import { throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class CategoriaService {
  private apiUrl = 'http://localhost:8080/categorias'; // URL da API

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) {
      throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    }
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Método para obter o orgId
  public getOrgId(): string {
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

  listarCategorias(): Observable<Categoria[]> {
  const orgId = this.getOrgId();  // Obtém o orgId do localStorage ou do token
  const headers = this.getAuthHeaders();

  // A URL agora inclui o 'orgId' como parte do caminho da URL
  return this.http.get<Categoria[]>(`${this.apiUrl}/${orgId}`, { headers }).pipe(
    catchError((error) => {
      console.error('Erro ao listar categorias:', error);
      return throwError(() => new Error('Falha ao carregar categorias, tente novamente.'));
    })
  );
}

criarCategoria(nome: string): Observable<Categoria> {
  const orgId = this.getOrgId();  // Obtém o orgId do localStorage
  const headers = this.getAuthHeaders();

  const body = { 
    nome,
    orgId
  };

  return this.http.post<Categoria>(`${this.apiUrl}`, body, { headers }).pipe(
    catchError((error) => {
      console.error('Erro ao criar categoria:', error);
      return throwError(() => new Error('Falha ao criar categoria, tente novamente.'));
    }),
    map((response: Categoria) => {
      // Agora, a resposta é diretamente um objeto Categoria
      return new Categoria(
        response.id, 
        response.nome, 
        response.descricao,  // Certifique-se de que `descricao` está presente na resposta
        response.criadoEm,   // Se a resposta incluir isso, ou modifique para sua estrutura
        response.orgId       // Certifique-se de que `orgId` está na resposta ou remova
      );
    })
  );
}


 
deletarCategoria(id: number): Observable<void> {
  const orgId = this.getOrgId(); // Obtém o orgId do localStorage ou de onde você estiver armazenando
  const headers = this.getAuthHeaders(); // Obtém os headers de autenticação

  // Passando o orgId como parte da URL no caminho, conforme definido no backend
  return this.http.delete<void>(`${this.apiUrl}/${id}/${orgId}`, { headers }).pipe(
    catchError((error) => {
      console.error('Erro ao deletar categoria:', error);
      return throwError(() => new Error('Falha ao deletar categoria, tente novamente.'));
    })
  );
}

 
}
