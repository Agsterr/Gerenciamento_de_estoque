import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';
import { Consumer } from '../models/consumer.model';

@Injectable({
  providedIn: 'root',
})
export class ConsumidorService {
  private apiUrl = 'http://localhost:8080/consumidores';

  constructor(private http: HttpClient) {}

  // ✅ Gera headers com o token JWT para autenticação
  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado. Faça login.');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // ✅ Extrai o orgId do token JWT
  public getOrgId(): string {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado.');
    const payload = this.decodeJwt(token);
    if (payload && payload.org_id) return payload.org_id;
    throw new Error('OrgId não encontrado no token.');
  }

  // ✅ Decodifica o JWT para extrair informações do payload
  private decodeJwt(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Token JWT inválido.');
    const payload = atob(parts[1]);
    return JSON.parse(payload);
  }

  /**
   * 🔍 Lista consumidores com paginação.
   * @param page Número da página
   * @param size Quantidade por página
   */
  listarConsumidores(page: number = 0, size: number = 10): Observable<Consumer[]> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<{ content: Consumer[] }>(`${this.apiUrl}`, { headers, params }).pipe(
      map(response => response.content),
      catchError(error => {
        console.error('Erro ao listar consumidores:', error);
        return throwError(() => new Error('Falha ao carregar consumidores.'));
      })
    );
  }

  /**
   * ✅ Cria um novo consumidor.
   * @param consumidor Dados do novo consumidor
   */
  criarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    const headers = this.getAuthHeaders();
    const body = {
      ...consumidor,
      orgId: this.getOrgId()
    };

    return this.http.post<Consumer>(`${this.apiUrl}`, body, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao criar consumidor:', error);
        return throwError(() => new Error('Erro ao criar consumidor.'));
      })
    );
  }

  /**
   * ✏️ Edita um consumidor existente.
   * @param consumidor Objeto com dados a serem atualizados
   */
  editarConsumidor(consumidor: Partial<Consumer>): Observable<Consumer> {
    if (!consumidor.id) {
      return throwError(() => new Error('ID do consumidor ausente.'));
    }

    const headers = this.getAuthHeaders();
    const body = {
      ...consumidor,
      orgId: this.getOrgId()
    };

    return this.http.put<Consumer>(`${this.apiUrl}/${consumidor.id}`, body, { headers }).pipe(
      catchError(error => {
        console.error('Erro ao editar consumidor:', error);
        return throwError(() => new Error('Erro ao editar consumidor.'));
      })
    );
  }

  /**
   * 🗑️ Remove um consumidor por ID.
   * @param id ID do consumidor
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
