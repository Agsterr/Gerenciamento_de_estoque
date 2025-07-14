import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MovimentacaoProduto, PageResponse } from '../models/movimentacao-produto.model';
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
  registrarMovimentacao(dto: MovimentacaoProduto): Observable<MovimentacaoProduto> {
    const headers = this.getAuthHeaders();
    return this.http.post<MovimentacaoProduto>(this.apiUrl, dto, { headers });
  }

  // Buscar movimentações por data
  buscarPorData(tipo: TipoMovimentacao, data: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    if (!data) {
      throw new Error('A data não pode estar vazia');
    }
    
    // Formata a data para o formato ISO.DATE (YYYY-MM-DD)
    const dataFormatada = data.split('T')[0];
    
    const params = new HttpParams()
      .set('tipo', tipo)
      .set('data', dataFormatada)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-data`, { headers, params });
  }

  // Buscar movimentações por período
  buscarPorPeriodo(tipo: TipoMovimentacao, inicio: string, fim: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    if (!inicio || !fim) {
      throw new Error('As datas de início e fim são obrigatórias');
    }

    const params = new HttpParams()
      .set('tipo', tipo)
      .set('inicio', inicio)
      .set('fim', fim)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-periodo`, { headers, params });
  }

  // Buscar movimentações por ano
  buscarPorAno(ano: number, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('ano', ano.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-ano`, { headers, params });
  }

  // Buscar movimentações por mês
  buscarPorMes(ano: number, mes: number, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('ano', ano.toString())
      .set('mes', mes.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-mes`, { headers, params });
  }

  // Buscar movimentações por nome do produto
  buscarPorNomeProduto(nomeProduto: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    // Validação adicional do nome do produto
    const nomeProdutoTrimmed = nomeProduto?.trim() || '';
    if (nomeProdutoTrimmed.length === 0) {
      throw new Error('O nome do produto não pode estar vazio');
    }
    
    const params = new HttpParams()
      .set('nomeProduto', nomeProdutoTrimmed)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-nome`, { headers, params });
  }

  // Buscar movimentações por categoria do produto
  buscarPorCategoriaProduto(categoriaProduto: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('categoriaProduto', categoriaProduto)
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-categoria`, { headers, params });
  }

  // Buscar movimentações por ID do produto
  buscarPorIdProduto(produtoId: number, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('produtoId', produtoId.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-id`, { headers, params });
  }

  // Buscar movimentações de uma organização por produto, nome ou categoria, e por intervalo de datas
  buscarPorProdutoNomeCategoriaIdAndIntervalo(
    nome: string | null,
    categoria: string | null,
    produtoId: number | null,
    inicio: string,
    fim: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    
    if (!inicio || !fim) {
      throw new Error('As datas de início e fim são obrigatórias');
    }
    
    let params = new HttpParams()
      .set('inicio', inicio)
      .set('fim', fim)
      .set('page', page.toString())
      .set('size', size.toString());

    // Adiciona parâmetros opcionais apenas se fornecidos
    if (nome) {
      params = params.set('nome', nome.trim());
    }
    if (categoria) {
      params = params.set('categoria', categoria.trim());
    }
    if (produtoId !== null) {
      params = params.set('produtoId', produtoId.toString());
    }

    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-intervalo`, { headers, params });
  }



  // Buscar movimentações por tipos (ENTRADA/SAIDA)
  buscarPorTipos(tipos: TipoMovimentacao[], page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProduto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('tipos', tipos.join(','))
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<MovimentacaoProduto>>(`${this.apiUrl}/por-tipos`, { headers, params });
  }
}
