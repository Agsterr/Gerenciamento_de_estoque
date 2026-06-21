import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface MesValor { mes: string; valor: number; }
export interface ProdutoRanking { nome: string; quantidade: number; valor: number; }

export interface DashboardResumo {
  totalProdutos: number;
  totalClientes: number;
  totalPedidosVenda: number;
  pedidosConfirmadosMes: number;
  faturamentoMes: number;
  valorEstoque: number;
  valorEstoqueCusto: number;
  produtosEstoqueBaixo: number;
  entradasMes: number;
  saidasMes: number;
  faturamentoPorMes: MesValor[];
  topProdutosVendidos: ProdutoRanking[];
  topProdutosEstoque: ProdutoRanking[];
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private url = `${environment.apiUrl}/api/dashboard`;

  constructor(private http: HttpClient) {}

  resumo(): Observable<DashboardResumo> {
    return this.http.get<DashboardResumo>(`${this.url}/resumo`);
  }
}
