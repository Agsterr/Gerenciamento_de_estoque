import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MovimentacaoProdutoDto } from '../models/movimentacao-produto.model';
import { TipoMovimentacao } from '../models/movimentacao-produto.model';


@Injectable({
  providedIn: 'root'
})
export class MovimentacaoProdutoService {
  private apiUrl = 'http://localhost:8080/movimentacoes';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado. O usuário precisa estar autenticado.');
    return new HttpHeaders().set('Authorization', `Bearer ${token}`);
  }

  // Registrar nova movimentação
  registrarMovimentacao(dto: MovimentacaoProdutoDto): Observable<MovimentacaoProdutoDto> {
    const headers = this.getAuthHeaders();
    return this.http.post<MovimentacaoProdutoDto>(this.apiUrl, dto, { headers });
  }

  // Buscar movimentações por data
  buscarPorData(tipo: TipoMovimentacao, data: string): Observable<MovimentacaoProdutoDto[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<MovimentacaoProdutoDto[]>(`${this.apiUrl}/por-data?tipo=${tipo}&data=${data}`, { headers });
  }

  // Buscar movimentações por período
  buscarPorPeriodo(tipo: TipoMovimentacao, inicio: string, fim: string): Observable<MovimentacaoProdutoDto[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<MovimentacaoProdutoDto[]>(`${this.apiUrl}/por-periodo?tipo=${tipo}&inicio=${inicio}&fim=${fim}`, { headers });
  }

  // Buscar movimentações por ano
  buscarPorAno(ano: number): Observable<MovimentacaoProdutoDto[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<MovimentacaoProdutoDto[]>(`${this.apiUrl}/por-ano?ano=${ano}`, { headers });
  }

  // Buscar movimentações por mês
  buscarPorMes(ano: number, mes: number): Observable<MovimentacaoProdutoDto[]> {
    const headers = this.getAuthHeaders();
    return this.http.get<MovimentacaoProdutoDto[]>(`${this.apiUrl}/por-mes?ano=${ano}&mes=${mes}`, { headers });
  }
}
