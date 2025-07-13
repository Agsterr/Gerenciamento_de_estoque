import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatNativeDateModule } from '@angular/material/core';
import { MatPaginatorModule, PageEvent, MatPaginator } from '@angular/material/paginator';
import { MovimentacaoProdutoService } from '../services/movimentacao-produto.service';
import { MovimentacaoProdutoDto, TipoMovimentacao, PageResponse } from '../models/movimentacao-produto.model';


@Component({
  selector: 'app-movimentacao-produto',
  templateUrl: './movimentacao.produto.component.html',
  styleUrls: ['./movimentacao.produto.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDatepickerModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatNativeDateModule,
    MatPaginatorModule
  ]
})
export class MovimentacaoProdutoComponent implements OnInit {
  // ViewChild para o paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  // Dados da lista
  movimentacoes: MovimentacaoProdutoDto[] = [];
  loading: boolean = false;

  // Parâmetros de paginação
  totalItems: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;
  pageSizeOptions: number[] = [5, 10, 25, 50];

  // Filtros
  tipoMovimentacao: TipoMovimentacao = TipoMovimentacao.ENTRADA;
  data: string = '';
  inicio: string = '';
  fim: string = '';
  ano: number = new Date().getFullYear();
  mes: number = new Date().getMonth() + 1;
  nomeProduto: string = '';
  categoriaProduto: string = '';
  produtoId: number = 0;

  constructor(private movimentacaoService: MovimentacaoProdutoService) {}

  ngOnInit(): void {
    this.buscarMovimentacoes(); 
  }

  // Função auxiliar para formatar data como ISO.DATE_TIME
  private formatarDataISO(data: string | Date | null, comHora: boolean = false): string | null {
    if (!data) return null;
    
    const dataObj = typeof data === 'string' ? new Date(data) : data;
    
    // Ajusta o fuso horário para meia-noite ou 23:59:59 conforme necessário
    if (comHora) {
      dataObj.setHours(23, 59, 59, 999); // Para data final
    } else {
      dataObj.setHours(0, 0, 0, 0); // Para data inicial
    }
    
    // Formata a data no padrão ISO.DATE_TIME
    const ano = dataObj.getFullYear();
    const mes = String(dataObj.getMonth() + 1).padStart(2, '0');
    const dia = String(dataObj.getDate()).padStart(2, '0');
    const horas = String(dataObj.getHours()).padStart(2, '0');
    const minutos = String(dataObj.getMinutes()).padStart(2, '0');
    const segundos = String(dataObj.getSeconds()).padStart(2, '0');
    
    return `${ano}-${mes}-${dia}T${horas}:${minutos}:${segundos}`; // Formato ISO.DATE_TIME
  }

  // Método para buscar movimentações com base nos filtros
  buscarMovimentacoes(): void {
    this.loading = true;
    this.movimentacoes = []; // Limpa a lista antes de cada busca

    try {
      // Verifica se há algum outro filtro ativo além do tipo
      const temOutrosFiltrosAtivos = this.data || this.inicio || this.fim || 
                                    this.ano || this.mes || this.nomeProduto || 
                                    this.categoriaProduto || this.produtoId;

      // Se não houver outros filtros ativos, busca apenas por tipo
      if (!temOutrosFiltrosAtivos) {
        this.movimentacaoService.buscarPorTipo(this.tipoMovimentacao, this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por tipo:', error);
            this.loading = false;
          }
        });
        return;
      }

      // Se houver outros filtros ativos, segue a ordem de prioridade
      if (this.data) {
        const dataFormatada = this.formatarDataISO(this.data);
        this.movimentacaoService.buscarPorData(this.tipoMovimentacao, dataFormatada || '', this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por data:', error);
            this.loading = false;
          }
        });
        return;
      }

      if (this.inicio && this.fim) {
        const inicioFormatado = this.formatarDataISO(this.inicio);
        const fimFormatado = this.formatarDataISO(this.fim, true);

        this.movimentacaoService.buscarPorPeriodo(this.tipoMovimentacao, inicioFormatado || '', fimFormatado || '', this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por período:', error);
            this.loading = false;
          }
        });
        return;
      }

      if (this.nomeProduto) {
        this.movimentacaoService.buscarPorNomeProduto(this.tipoMovimentacao, this.nomeProduto, this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por nome do produto:', error);
            this.loading = false;
          }
        });
        return;
      }

      if (this.categoriaProduto) {
        this.movimentacaoService.buscarPorCategoriaProduto(this.tipoMovimentacao, this.categoriaProduto, this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por categoria:', error);
            this.loading = false;
          }
        });
        return;
      }

      if (this.ano && this.mes) {
        this.movimentacaoService.buscarPorMes(this.ano, this.mes, this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por mês:', error);
            this.loading = false;
          }
        });
        return;
      }

      if (this.ano) {
        this.movimentacaoService.buscarPorAno(this.ano, this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por ano:', error);
            this.loading = false;
          }
        });
        return;
      }

      if (this.produtoId) {
        this.movimentacaoService.buscarPorIdProduto(this.produtoId, this.pageIndex, this.pageSize).subscribe({
          next: (data) => this.atualizarDadosPaginados(data),
          error: (error) => {
            console.error('Erro ao buscar por ID do produto:', error);
            this.loading = false;
          }
        });
        return;
      }

      // Se nenhum filtro estiver ativo, limpa a lista e desativa o loading
      this.loading = false;
      this.movimentacoes = [];
      this.totalItems = 0;

    } catch (error) {
      console.error('Erro ao processar a busca:', error);
      this.loading = false;
      this.movimentacoes = [];
      this.totalItems = 0;
    }
  }

  // Método auxiliar para atualizar os dados paginados
  private atualizarDadosPaginados(response: PageResponse<MovimentacaoProdutoDto>): void {
    this.movimentacoes = response.content || [];
    this.totalItems = response.totalElements;
    this.loading = false;

    if (this.paginator && this.movimentacoes.length === 0 && this.pageIndex > 0) {
      this.pageIndex = 0;
      this.buscarMovimentacoes();
    }
  }

  // Método para registrar uma nova movimentação
  registrarMovimentacao(): void {
    const novoMovimento: MovimentacaoProdutoDto = {
      id: 0,
      produtoId: 1,
      tipo: this.tipoMovimentacao,
      quantidade: 10,
      dataHora: new Date().toISOString(),
      orgId: 1,
      nomeProduto: 'Produto Exemplo',
    };

    this.movimentacaoService.registrarMovimentacao(novoMovimento).subscribe(response => {
      console.log('Movimentação registrada com sucesso!', response);
      this.buscarMovimentacoes(); 
    });
  }

  // Método para lidar com eventos de paginação
  handlePageEvent(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.buscarMovimentacoes();
  }

  // Método para limpar filtros
  limparFiltros(): void {
    // Reset de todos os filtros
    this.tipoMovimentacao = TipoMovimentacao.ENTRADA;
    this.data = '';
    this.inicio = '';
    this.fim = '';
    this.ano = new Date().getFullYear();
    this.mes = new Date().getMonth() + 1;
    this.nomeProduto = '';
    this.categoriaProduto = '';
    this.produtoId = 0;

    // Reset da paginação
    if (this.paginator) {
      this.paginator.pageIndex = 0;
      this.pageIndex = 0;
    }
    this.pageSize = 10;

    // Limpa a lista e busca novamente
    this.movimentacoes = [];
    this.totalItems = 0;
    this.buscarMovimentacoes();
  }
}
