import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Consumer } from '../models/consumer.model';
import { catchError, map } from 'rxjs/operators';
import { throwError } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class ConsumidorService {
  private apiUrl = 'http://localhost:8080/consumidores'; // URL da API

  constructor(private http: HttpClient) {}

  // Gerar os headers com o token JWT
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

  // Método para listar consumidores de uma organização específica
  listarConsumidoresPorOrg(): Observable<{ message: string, consumidores: Consumer[] }> {
    const orgId = this.getOrgId();  // Obtém o orgId do localStorage ou do token
    const headers = this.getAuthHeaders();

    return this.http.get<{ message: string, consumidores: Consumer[] }>(`${this.apiUrl}/${orgId}`, { headers }).pipe(
      catchError((error) => {
        console.error('Erro ao listar consumidores:', error);
        return throwError(() => new Error('Falha ao carregar consumidores, tente novamente.'));
      })
    );
  }

  // Método para criar um novo consumidor
  criarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    const orgId = this.getOrgId();  // Obtém o orgId do localStorage
    const headers = this.getAuthHeaders();

    const body = { 
      ...consumidor,
      orgId  // Adiciona o orgId no corpo da requisição
    };

    return this.http.post<Consumer>(`${this.apiUrl}`, body, { headers }).pipe(
      catchError((error) => {
        console.error('Erro ao criar consumidor:', error);
        return throwError(() => new Error('Falha ao criar consumidor, tente novamente.'));
      })
    );
  }

  editarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    const orgId = this.getOrgId();  // Obtém o orgId do localStorage
    const headers = this.getAuthHeaders();
  
    // Verifique se o id está presente antes de fazer a requisição
    if (!consumidor.id) {
      console.error('ID do consumidor não fornecido:', consumidor);
      return throwError(() => new Error('ID do consumidor não encontrado.'));
    }
  
    // Adicionando o orgId no corpo da requisição
    const body = { 
      ...consumidor,
      orgId // Garantindo que o orgId seja incluído corretamente
    };
  
    console.log('Atualizando consumidor no backend:', body);
  
    // Fazendo a requisição PUT para o backend com a URL que inclui o id e orgId
    return this.http
      .put<Consumer>(`${this.apiUrl}/${consumidor.id}/${orgId}`, body, { headers })
      .pipe(
        catchError((error) => {
          console.error('Erro ao editar consumidor:', error);
          return throwError(() => new Error('Falha ao editar consumidor, tente novamente.'));
        })
      );
  }
   
   
  
  

  // Método para deletar um consumidor pelo ID e orgId
  deletarConsumidor(id: number): Observable<void> {
    const orgId = this.getOrgId(); // Obtém o orgId do localStorage
    const headers = this.getAuthHeaders(); // Obtém os headers de autenticação

    // Passando o orgId como parte da URL no caminho, conforme definido no backend
    return this.http.delete<void>(`${this.apiUrl}/${id}/${orgId}`, { headers }).pipe(
      catchError((error) => {
        console.error('Erro ao deletar consumidor:', error);
        return throwError(() => new Error('Falha ao deletar consumidor, tente novamente.'));
      })
    );
  }
}
