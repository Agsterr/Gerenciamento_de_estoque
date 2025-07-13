import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { MovimentacaoProdutoDto, PageResponse } from '../models/movimentacao-produto.model';
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
  buscarPorData(tipo: TipoMovimentacao | null, data: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    if (!data) {
      throw new Error('A data não pode estar vazia');
    }
    
    // Formata a data para o formato ISO.DATE (YYYY-MM-DD)
    const dataFormatada = data.split('T')[0];
    
    let params = new HttpParams()
      .set('data', dataFormatada)
      .set('page', page.toString())
      .set('size', size.toString());
    
    // Adiciona o tipo de movimentação se fornecido
    if (tipo !== null) {
      params = params.set('tipo', tipo);
    }
    
    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-data`, { headers, params });
  }

  // Buscar movimentações por período
  buscarPorPeriodo(tipo: TipoMovimentacao | null, inicio: string, fim: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    if (!inicio || !fim) {
      throw new Error('As datas de início e fim são obrigatórias');
    }

    // Remove o Z e converte para o formato ISO.DATE (YYYY-MM-DD)
    const inicioFormatado = inicio.split('T')[0];
    const fimFormatado = fim.split('T')[0];
    
    let params = new HttpParams()
      .set('inicio', inicioFormatado)
      .set('fim', fimFormatado)
      .set('page', page.toString())
      .set('size', size.toString());

    // Adiciona o tipo de movimentação se fornecido
    if (tipo !== null) {
      params = params.set('tipo', tipo);
    }

    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-periodo`, { headers, params });
  }

  // Buscar movimentações por ano
  buscarPorAno(ano: number, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('ano', ano.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-ano`, { headers, params });
  }

  // Buscar movimentações por mês
  buscarPorMes(ano: number, mes: number, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('ano', ano.toString())
      .set('mes', mes.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-mes`, { headers, params });
  }

  // Buscar movimentações por nome do produto
  buscarPorNomeProduto(tipo: TipoMovimentacao | null, nomeProduto: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    // Validação adicional do nome do produto
    const nomeProdutoTrimmed = nomeProduto?.trim() || '';
    if (nomeProdutoTrimmed.length === 0) {
      throw new Error('O nome do produto não pode estar vazio');
    }
    
    let params = new HttpParams()
      .set('nomeProduto', nomeProdutoTrimmed) // Mantém o caso original para respeitar a busca do backend
      .set('page', page.toString())
      .set('size', size.toString());
    
    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-nome`, { headers, params });
  }

  // Buscar movimentações por categoria do produto
  buscarPorCategoriaProduto(tipo: TipoMovimentacao | null, categoriaProduto: string, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    let params = new HttpParams()
      .set('categoriaProduto', categoriaProduto)
      .set('page', page.toString())
      .set('size', size.toString());

    if (tipo !== null) {
      params = params.set('tipo', tipo);
    }
    
    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-categoria`, { headers, params });
  }

  // Buscar movimentações por ID do produto
  buscarPorIdProduto(produtoId: number, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('produtoId', produtoId.toString())
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-id`, { headers, params });
  }

  // Buscar movimentações de uma organização por produto, nome ou categoria, e por intervalo de datas
  buscarPorProdutoNomeCategoriaIdAndIntervalo(
    tipo: TipoMovimentacao | null,
    nome: string | null,
    categoria: string | null,
    produtoId: number | null,
    inicio: string,
    fim: string,
    page: number = 0,
    size: number = 20
  ): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    
    // Validação das datas
    if (!inicio || !fim) {
      throw new Error('As datas de início e fim são obrigatórias');
    }

    // Remover o 'Z' e converter para o formato correto (ISO.DATE)
    const inicioFormatado = inicio.split('T')[0];
    const fimFormatado = fim.split('T')[0];
    
    let params = new HttpParams()
      .set('inicio', inicioFormatado)
      .set('fim', fimFormatado)
      .set('page', page.toString())
      .set('size', size.toString());

    // Adiciona parâmetros opcionais apenas se fornecidos
    if (tipo !== null) {
      params = params.set('tipo', tipo);
    }
    if (nome) {
      params = params.set('nome', nome.trim());
    }
    if (categoria) {
      params = params.set('categoria', categoria.trim());
    }
    if (produtoId !== null) {
      params = params.set('produtoId', produtoId.toString());
    }

    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}/por-intervalo`, { headers, params });
  }

  // Buscar movimentações apenas por tipo (ENTRADA/SAIDA)
  buscarPorTipo(tipo: TipoMovimentacao, page: number = 0, size: number = 20): Observable<PageResponse<MovimentacaoProdutoDto>> {
    const headers = this.getAuthHeaders();
    const params = new HttpParams()
      .set('tipo', tipo)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PageResponse<MovimentacaoProdutoDto>>(`${this.apiUrl}`, { headers, params });
  }
}
