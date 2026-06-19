import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Deposito {
  id: number; nome: string; endereco?: string; padrao: boolean; ativo: boolean; orgId: number;
}

export interface EstoqueDeposito {
  id: number; produtoId: number; produtoNome: string; sku?: string;
  depositoId: number; depositoNome: string; quantidade: number;
  custoMedio?: number; valorTotal?: number;
}

@Injectable({ providedIn: 'root' })
export class DepositoService {
  private url = `${environment.apiUrl}/depositos`;
  constructor(private http: HttpClient) {}
  listar(): Observable<Deposito[]> { return this.http.get<Deposito[]>(this.url); }
  criar(data: Partial<Deposito>): Observable<Deposito> { return this.http.post<Deposito>(this.url, data); }
  atualizar(id: number, data: Partial<Deposito>): Observable<Deposito> { return this.http.put<Deposito>(`${this.url}/${id}`, data); }
  excluir(id: number): Observable<{ message: string }> { return this.http.delete<{ message: string }>(`${this.url}/${id}`); }
  estoque(depositoId: number): Observable<EstoqueDeposito[]> { return this.http.get<EstoqueDeposito[]>(`${this.url}/${depositoId}/estoque`); }
  transferir(data: { depositoOrigemId: number; depositoDestinoId: number; produtoId: number; quantidade: number }): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.url}/transferir`, data);
  }
}
