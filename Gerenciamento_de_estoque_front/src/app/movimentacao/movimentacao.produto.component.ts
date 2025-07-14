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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MovimentacaoProdutoService } from '../services/movimentacao-produto.service';
import { MovimentacaoProduto, TipoMovimentacao, PageResponse } from '../models/movimentacao-produto.model';
import { MovimentacaoModalComponent, MovimentacaoModalData } from './movimentacao-modal/movimentacao-modal.component';


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
    MatPaginatorModule,
    MatDialogModule
  ]
})
export class MovimentacaoProdutoComponent implements OnInit {
  // ViewChild para o paginador
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  // Constantes para tipos de movimentação
  readonly TIPO_MOVIMENTACAO_ENTRADA = TipoMovimentacao.ENTRADA;
  readonly TIPO_MOVIMENTACAO_SAIDA = TipoMovimentacao.SAIDA;

  // Dados da lista
  movimentacoes: MovimentacaoProduto[] = [];
  loading: boolean = false;

  // Parâmetros de paginação
  totalItems: number = 0;
  pageSize: number = 10;
  pageIndex: number = 0;
  pageSizeOptions: number[] = [5, 10, 25, 50];

  // Filtros
  tipoMovimentacao: TipoMovimentacao = TipoMovimentacao.ENTRADA;
  tiposMovimentacao: TipoMovimentacao[] = [TipoMovimentacao.ENTRADA, TipoMovimentacao.SAIDA];
  data: Date | null = null;
  inicio: Date | null = null;
  fim: Date | null = null;
  ano: number | null = null;
  mes: number | null = null;
  nomeProduto: string = '';
  categoriaProduto: string = '';
  produtoId: number = 0;

  constructor(
    private movimentacaoService: MovimentacaoProdutoService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    console.log('🚀 Componente inicializado. Buscando movimentações padrão...');
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

  // Função auxiliar para formatar data como ISO.DATE (apenas data)
  private formatarDataISO_DATE(data: string | Date | null): string | null {
    if (!data) return null;
    
    const dataObj = typeof data === 'string' ? new Date(data) : data;
    
    // Formata a data no padrão ISO.DATE (YYYY-MM-DD)
    const ano = dataObj.getFullYear();
    const mes = String(dataObj.getMonth() + 1).padStart(2, '0');
    const dia = String(dataObj.getDate()).padStart(2, '0');
    
    return `${ano}-${mes}-${dia}`; // Formato ISO.DATE
  }

  // Método para buscar movimentações com base nos filtros
  buscarMovimentacoes(): void {
    console.log('🚀 Iniciando busca de movimentações...');
    console.log('📊 Estado dos filtros:', {
      tipoMovimentacao: this.tipoMovimentacao,
      data: this.data,
      inicio: this.inicio,
      fim: this.fim,
      ano: this.ano,
      mes: this.mes,
      nomeProduto: this.nomeProduto,
      categoriaProduto: this.categoriaProduto,
      produtoId: this.produtoId
    });

    // Limpa o estado completamente antes de cada busca
    this.loading = true;
    this.movimentacoes = [];
    this.totalItems = 0;

    // Garante que o pageIndex nunca seja negativo
    if (this.pageIndex < 0) {
      this.pageIndex = 0;
    }

    try {
      // Verifica se há combinações de filtros ativos
      const temFiltroData = this.data || (this.inicio && this.fim) || (this.ano && this.mes) || this.ano;
      const temFiltroProduto = (this.produtoId && this.produtoId > 0) || 
                              (this.nomeProduto && this.nomeProduto.trim()) || 
                              (this.categoriaProduto && this.categoriaProduto.trim());

      // Combinação: Tipo + Data específica
      if (this.data && this.tipoMovimentacao) {
        console.log('✅ Aplicando filtro: Tipo + Data específica');
        this.buscarPorTipoEDataEspecifica();
        return;
      }

      // Combinação: Tipo + Período
      if (this.inicio && this.fim && this.tipoMovimentacao) {
        console.log('✅ Aplicando filtro: Tipo + Período');
        this.buscarPorTipoEPeriodo();
        return;
      }

      // Combinação: Tipo + Ano/Mês
      if (this.ano && this.mes && this.tipoMovimentacao) {
        console.log('✅ Aplicando filtro: Tipo + Ano/Mês');
        this.buscarPorTipoEAnoMes();
        return;
      }

      // Combinação: Tipo + Ano
      if (this.ano && this.tipoMovimentacao) {
        console.log('✅ Aplicando filtro: Tipo + Ano');
        this.buscarPorTipoEAno();
        return;
      }

      // Combinação: Tipo + Produto (ID)
      if (this.produtoId && this.produtoId > 0 && this.tipoMovimentacao) {
        console.log('✅ Aplicando filtro: Tipo + ID do produto');
        this.buscarPorTipoEIdProduto();
        return;
      }

      // Combinação: Tipo + Nome do produto
      if (this.nomeProduto && this.nomeProduto.trim() && this.tipoMovimentacao) {
        console.log('✅ Aplicando filtro: Tipo + Nome do produto');
        this.buscarPorTipoENomeProduto();
        return;
      }



      // Combinação: Data + Produto (quando não há tipo específico)
      if (this.data && this.produtoId && this.produtoId > 0) {
        console.log('✅ Aplicando filtro: Data específica + ID do produto');
        this.buscarPorDataEIdProduto();
        return;
      }

      if (this.data && this.nomeProduto && this.nomeProduto.trim()) {
        console.log('✅ Aplicando filtro: Data específica + Nome do produto');
        this.buscarPorDataENomeProduto();
        return;
      }



      // Combinação: Período + Produto
      if (this.inicio && this.fim && this.produtoId && this.produtoId > 0) {
        console.log('✅ Aplicando filtro: Período + ID do produto');
        this.buscarPorPeriodoEIdProduto();
        return;
      }

      if (this.inicio && this.fim && this.nomeProduto && this.nomeProduto.trim()) {
        console.log('✅ Aplicando filtro: Período + Nome do produto');
        this.buscarPorPeriodoENomeProduto();
        return;
      }

      // Filtros individuais (sem combinação)
      if (this.data) {
        console.log('✅ Aplicando filtro: Data específica');
        this.buscarPorDataEspecifica();
        return;
      }

      if (this.inicio && this.fim) {
        console.log('✅ Aplicando filtro: Período');
        this.buscarPorPeriodo();
        return;
      }

      if (this.ano && this.mes) {
        console.log('✅ Aplicando filtro: Ano/Mês');
        this.buscarPorAnoMes();
        return;
      }

      if (this.ano) {
        console.log('✅ Aplicando filtro: Ano');
        this.buscarPorAno();
        return;
      }

      if (this.produtoId && this.produtoId > 0) {
        console.log('✅ Aplicando filtro: ID do produto');
        this.buscarPorIdProduto();
        return;
      }

      if (this.nomeProduto && this.nomeProduto.trim()) {
        console.log('✅ Aplicando filtro: Nome do produto');
        this.buscarPorNomeProduto();
        return;
      }

      if (this.categoriaProduto && this.categoriaProduto.trim()) {
        console.log('✅ Aplicando filtro: Categoria');
        this.buscarPorCategoria();
        return;
      }

      // Busca por tipo de movimentação (padrão)
      console.log('✅ Aplicando filtro: Tipo de movimentação (padrão)');
      this.buscarPorTipo();

    } catch (error) {
      console.error('❌ Erro ao processar a busca:', error);
      this.handleError(error);
    }
  }

  // Métodos específicos para cada tipo de busca
  private buscarPorDataEspecifica(): void {
    console.log('🔍 Buscando por data específica:', this.data);
    const dataFormatada = this.formatarDataISO_DATE(this.data);
    if (!dataFormatada) {
      this.loading = false;
      return;
    }
    console.log('📅 Data formatada:', dataFormatada);
    this.movimentacaoService.buscarPorData(this.tipoMovimentacao, dataFormatada, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorPeriodo(): void {
    console.log('🔍 Buscando por período:', this.inicio, 'até', this.fim);
    const inicioFormatado = this.formatarDataISO(this.inicio);
    const fimFormatado = this.formatarDataISO(this.fim, true);
    if (!inicioFormatado || !fimFormatado) {
      this.loading = false;
      return;
    }
    console.log('📅 Período formatado:', inicioFormatado, 'até', fimFormatado);
    this.movimentacaoService.buscarPorPeriodo(this.tipoMovimentacao, inicioFormatado, fimFormatado, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorAnoMes(): void {
    console.log('🔍 Buscando por ano/mês:', this.ano, '/', this.mes);
    this.movimentacaoService.buscarPorMes(this.ano!, this.mes!, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorAno(): void {
    console.log('🔍 Buscando por ano:', this.ano);
    this.movimentacaoService.buscarPorAno(this.ano!, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorIdProduto(): void {
    console.log('🔍 Buscando por ID do produto:', this.produtoId);
    this.movimentacaoService.buscarPorIdProduto(this.produtoId, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorNomeProduto(): void {
    console.log('🔍 Buscando por nome do produto:', this.nomeProduto);
    const nomeProdutoTrimmed = this.nomeProduto.trim();
    this.movimentacaoService.buscarPorNomeProduto(nomeProdutoTrimmed, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorCategoria(): void {
    console.log('🔍 Buscando por categoria:', this.categoriaProduto);
    this.movimentacaoService.buscarPorCategoriaProduto(this.categoriaProduto.trim(), this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorTipo(): void {
    console.log('🔍 Buscando por tipo:', this.tipoMovimentacao);
    this.movimentacaoService.buscarPorTipos([this.tipoMovimentacao], this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  // Métodos para combinações de filtros
  private buscarPorTipoEDataEspecifica(): void {
    console.log('🔍 Buscando por tipo + data específica:', this.tipoMovimentacao, this.data);
    const dataFormatada = this.formatarDataISO_DATE(this.data);
    if (!dataFormatada) {
      this.loading = false;
      return;
    }
    this.movimentacaoService.buscarPorData(this.tipoMovimentacao, dataFormatada, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorTipoEPeriodo(): void {
    console.log('🔍 Buscando por tipo + período:', this.tipoMovimentacao, this.inicio, 'até', this.fim);
    const inicioFormatado = this.formatarDataISO(this.inicio);
    const fimFormatado = this.formatarDataISO(this.fim, true);
    if (!inicioFormatado || !fimFormatado) {
      this.loading = false;
      return;
    }
    this.movimentacaoService.buscarPorPeriodo(this.tipoMovimentacao, inicioFormatado, fimFormatado, this.pageIndex, this.pageSize)
      .subscribe({
        next: this.atualizarDadosPaginados.bind(this),
        error: this.handleError.bind(this)
      });
  }

  private buscarPorTipoEAnoMes(): void {
    console.log('🔍 Buscando por tipo + ano/mês:', this.tipoMovimentacao, this.ano, '/', this.mes);
    // Como o backend não tem endpoint específico para tipo + ano/mês, vamos buscar por ano/mês e filtrar por tipo no frontend
    this.movimentacaoService.buscarPorMes(this.ano!, this.mes!, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por tipo no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.tipo === this.tipoMovimentacao) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }

  private buscarPorTipoEAno(): void {
    console.log('🔍 Buscando por tipo + ano:', this.tipoMovimentacao, this.ano);
    // Como o backend não tem endpoint específico para tipo + ano, vamos buscar por ano e filtrar por tipo no frontend
    this.movimentacaoService.buscarPorAno(this.ano!, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por tipo no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.tipo === this.tipoMovimentacao) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }

  private buscarPorTipoEIdProduto(): void {
    console.log('🔍 Buscando por tipo + ID do produto:', this.tipoMovimentacao, this.produtoId);
    // Como o backend não tem endpoint específico para tipo + ID, vamos buscar por ID e filtrar por tipo no frontend
    this.movimentacaoService.buscarPorIdProduto(this.produtoId, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por tipo no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.tipo === this.tipoMovimentacao) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }

  private buscarPorTipoENomeProduto(): void {
    console.log('🔍 Buscando por tipo + nome do produto:', this.tipoMovimentacao, this.nomeProduto);
    // Como o backend não tem endpoint específico para tipo + nome, vamos buscar por nome e filtrar por tipo no frontend
    const nomeProdutoTrimmed = this.nomeProduto.trim();
    this.movimentacaoService.buscarPorNomeProduto(nomeProdutoTrimmed, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por tipo no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.tipo === this.tipoMovimentacao) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }

  private buscarPorTipoECategoria(): void {
    console.log('🔍 Buscando por tipo + categoria:', this.tipoMovimentacao, this.categoriaProduto);
    // Como o backend não tem endpoint específico para tipo + categoria, vamos buscar por categoria e filtrar por tipo no frontend
    this.movimentacaoService.buscarPorCategoriaProduto(this.categoriaProduto.trim(), this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por tipo no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.tipo === this.tipoMovimentacao) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }

  private buscarPorDataEIdProduto(): void {
    console.log('🔍 Buscando por data específica + ID do produto:', this.data, this.produtoId);
    const dataFormatada = this.formatarDataISO_DATE(this.data);
    if (!dataFormatada) {
      this.loading = false;
      return;
    }
    this.movimentacaoService.buscarPorData(this.tipoMovimentacao, dataFormatada, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por ID no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.produtoId === this.produtoId) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }

  private buscarPorDataENomeProduto(): void {
    console.log('🔍 Buscando por data específica + nome do produto:', this.data, this.nomeProduto);
    const dataFormatada = this.formatarDataISO_DATE(this.data);
    if (!dataFormatada) {
      this.loading = false;
      return;
    }
    const nomeProdutoTrimmed = this.nomeProduto.trim();
    this.movimentacaoService.buscarPorData(this.tipoMovimentacao, dataFormatada, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por nome no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.nomeProduto === nomeProdutoTrimmed) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }



  private buscarPorPeriodoEIdProduto(): void {
    console.log('🔍 Buscando por período + ID do produto:', this.inicio, this.fim, this.produtoId);
    const inicioFormatado = this.formatarDataISO(this.inicio);
    const fimFormatado = this.formatarDataISO(this.fim, true);
    if (!inicioFormatado || !fimFormatado) {
      this.loading = false;
      return;
    }
    this.movimentacaoService.buscarPorPeriodo(this.tipoMovimentacao, inicioFormatado, fimFormatado, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por ID no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.produtoId === this.produtoId) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }

  private buscarPorPeriodoENomeProduto(): void {
    console.log('🔍 Buscando por período + nome do produto:', this.inicio, this.fim, this.nomeProduto);
    const inicioFormatado = this.formatarDataISO(this.inicio);
    const fimFormatado = this.formatarDataISO(this.fim, true);
    if (!inicioFormatado || !fimFormatado) {
      this.loading = false;
      return;
    }
    const nomeProdutoTrimmed = this.nomeProduto.trim();
    this.movimentacaoService.buscarPorPeriodo(this.tipoMovimentacao, inicioFormatado, fimFormatado, this.pageIndex, this.pageSize)
      .subscribe({
        next: (response) => {
          // Filtra por nome no frontend
          const movimentacoesFiltradas = response.content?.filter(m => m.nomeProduto === nomeProdutoTrimmed) || [];
          const responseFiltrada = {
            ...response,
            content: movimentacoesFiltradas,
            totalElements: movimentacoesFiltradas.length
          };
          this.atualizarDadosPaginados(responseFiltrada);
        },
        error: this.handleError.bind(this)
      });
  }



  // Método auxiliar para atualizar os dados paginados
  private atualizarDadosPaginados(response: PageResponse<MovimentacaoProduto>): void {
    // Reset do estado de carregamento
    this.loading = false;

    // Se não houver resposta, limpa tudo
    if (!response) {
      this.movimentacoes = [];
      this.totalItems = 0;
      this.pageIndex = 0;
      return;
    }
    
    // Atualiza os dados da página atual
    this.movimentacoes = response.content || [];
    this.totalItems = response.totalElements;
    this.pageIndex = response.pageable?.pageNumber || 0;
    
    // Atualiza o paginador se necessário
    if (this.paginator) {
      // Só atualiza se os valores forem diferentes para evitar loop
      if (this.paginator.pageIndex !== this.pageIndex) {
        this.paginator.pageIndex = this.pageIndex;
      }
      if (this.paginator.pageSize !== response.pageable?.pageSize) {
        this.pageSize = response.pageable?.pageSize || this.pageSize;
      }
      if (this.paginator.length !== response.totalElements) {
        this.paginator.length = response.totalElements;
      }
    }
  }

  // Método auxiliar para tratar erros
  private handleError(error: any): void {
    console.error('Erro na busca de movimentações:', error);
    this.loading = false;
    this.movimentacoes = [];
    this.totalItems = 0;
  }

  // Método para registrar uma nova movimentação
  registrarMovimentacao(): void {
    // Dados iniciais para o modal
    const modalData: MovimentacaoModalData = {
      tipoMovimentacao: this.tipoMovimentacao,
      produtoId: this.produtoId || undefined,
      nomeProduto: this.nomeProduto || undefined
    };

    // Abre o modal
    const dialogRef = this.dialog.open(MovimentacaoModalComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: modalData,
      disableClose: false,
      autoFocus: true
    });

    // Aguarda o resultado do modal
    dialogRef.afterClosed().subscribe(result => {
      if (result && result.success) {
        // Registra a movimentação
        this.movimentacaoService.registrarMovimentacao(result.data).subscribe({
          next: (response) => {
            console.log('Movimentação registrada com sucesso!', response);
            // TODO: Mostrar mensagem de sucesso
            this.buscarMovimentacoes(); // Atualiza a lista
          },
          error: (error) => {
            console.error('Erro ao registrar movimentação:', error);
            // TODO: Mostrar mensagem de erro para o usuário
          }
        });
      }
    });
  }

  // Método para lidar com eventos de paginação
  handlePageEvent(event: PageEvent): void {
    // Atualiza os valores de paginação
    this.pageSize = event.pageSize;
    this.pageIndex = event.pageIndex;
    
    // Busca os dados da nova página
    this.buscarMovimentacoes();
  }

  // Método para aplicar filtros
  aplicarFiltros(): void {
    // Limpa completamente o estado
    this.movimentacoes = [];
    this.totalItems = 0;
    
    // Reseta a paginação
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
      this.paginator.pageSize = this.pageSize;
    }
    
    // Busca com os novos filtros
    this.buscarMovimentacoes();
  }

  // Método para aplicar filtro por tipo de movimentação
  aplicarFiltroTipo(): void {
    // Não limpa outros filtros - permite combinações
    // Reseta apenas a paginação
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    
    // Busca com todos os filtros ativos
    this.buscarMovimentacoes();
  }

  // Método para aplicar filtro por data
  aplicarFiltroData(): void {
    // Não limpa outros filtros - permite combinações
    // Reseta apenas a paginação
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    
    // Busca com todos os filtros ativos
    this.buscarMovimentacoes();
  }

  // Método para aplicar filtro por produto
  aplicarFiltroProduto(): void {
    // Não limpa outros filtros - permite combinações
    // Reseta apenas a paginação
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    
    // Busca com todos os filtros ativos
    this.buscarMovimentacoes();
  }

  // Método para aplicar todos os filtros ativos
  aplicarTodosFiltros(): void {
    console.log('🚀 Aplicando todos os filtros ativos...');
    console.log('📊 Estado atual dos filtros:', {
      tipoMovimentacao: this.tipoMovimentacao,
      data: this.data,
      inicio: this.inicio,
      fim: this.fim,
      ano: this.ano,
      mes: this.mes,
      nomeProduto: this.nomeProduto,
      categoriaProduto: this.categoriaProduto,
      produtoId: this.produtoId
    });
    
    // Verifica se há filtros ativos além do tipo padrão
    const temFiltrosAtivos = this.temFiltrosAtivos();
    if (!temFiltrosAtivos) {
      console.log('ℹ️ Nenhum filtro adicional ativo. Aplicando apenas filtro de tipo padrão.');
    }
    
    // Reseta a paginação
    this.pageIndex = 0;
    if (this.paginator) {
      this.paginator.pageIndex = 0;
    }
    
    // Busca com todos os filtros ativos
    this.buscarMovimentacoes();
  }

  // Método para verificar se há filtros ativos além do tipo padrão
  temFiltrosAtivos(): boolean {
    return !!(this.data || 
             (this.inicio && this.fim) || 
             this.ano || 
             this.mes || 
             (this.nomeProduto && this.nomeProduto.trim()) || 
             (this.categoriaProduto && this.categoriaProduto.trim()) || 
             (this.produtoId && this.produtoId > 0));
  }

  // Método para limpar filtros
  limparFiltros(): void {
    console.log('🧹 Limpando todos os filtros...');
    
    // Reset de todos os filtros para valores neutros
    this.tipoMovimentacao = TipoMovimentacao.ENTRADA;
    this.data = null;
    this.inicio = null;
    this.fim = null;
    this.ano = null; // Não define ano padrão
    this.mes = null; // Não define mês padrão
    this.nomeProduto = '';
    this.categoriaProduto = '';
    this.produtoId = 0;

    // Reset da paginação
    if (this.paginator) {
      this.paginator.pageIndex = 0;
      this.pageIndex = 0;
    }
    this.pageSize = 10;

    // Limpa a lista e busca apenas por tipo (padrão)
    this.movimentacoes = [];
    this.totalItems = 0;
    
    console.log('✅ Filtros limpos. Buscando apenas por tipo padrão...');
    this.buscarMovimentacoes();
  }
}
