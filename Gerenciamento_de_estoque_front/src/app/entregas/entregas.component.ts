import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EntregasService } from '../services/entregas.service';
import { ProdutoService } from '../services/produto.service';
import { ConsumidorService } from '../services/consumidor.service';
import { EntregaResponse } from '../models/src/app/models/entrega/entrega-response.model';
import { PageEntregaResponse } from '../models/src/app/models/entrega/PageEntregaResponse.model';
import { EntregaRequest } from '../models/src/app/models/entrega/entrega-request.model';
import { BuscaEntregaComponent } from './busca-entrega/busca-entrega.component';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-entregas',
  standalone: true,
  imports: [CommonModule, FormsModule,  BuscaEntregaComponent],
  templateUrl: './entregas.component.html',
  styleUrls: ['./entregas.component.scss']
})
export class EntregasComponent implements OnInit {
  entregas: EntregaResponse[] = [];
  filteredEntregas: EntregaResponse[] = [];
  searchTerm = '';
  showList = false;
  showAddForm = false;
  showEditForm = false;
  showBuscaEntrega: boolean = false;

  produtos: any[] = [];
  consumidores: any[] = [];
  porDia: EntregaResponse[] = [];


  totais: { [entregaId: number]: number } = {};
  errosTotais: { [entregaId: number]: string } = {};


  novaEntrega: Partial<EntregaRequest> = {};
  currentPage = 0;
  totalPages = 0;
  totalElements = 0;
  pageSize = 20;
  pageSizeOptions = [5, 10, 20, 50];

  mensagem = '';
  mensagemErro = '';

  idEntregaParaEditar: number | null = null;
  orgId: number | null = null;

  constructor(
    private entregasService: EntregasService,
    private produtoService: ProdutoService,
    private consumidorService: ConsumidorService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const user = this.authService.getLoggedUser();
    this.orgId = user?.orgId || null;
    this.carregarProdutos();
    this.carregarConsumidores();
    this.showList = true;
    this.fetchEntregas(this.currentPage);
  }

  // Método para capturar o evento emitido pelo BuscaEntregaComponent
  onBuscarEntregas(event: { filtro: string; entregas: EntregaResponse[] }): void {
    this.entregas = event.entregas;  // Atualiza a lista de entregas com os resultados da busca
    this.applyFilter();  // Aplica o filtro localmente
   
 
  }

  // Método para carregar os produtos
  carregarProdutos(): void {
    this.produtoService.listarProdutos(0, 100).subscribe({
      next: data => this.produtos = data.content,
      error: () => this.mensagemErro = 'Erro ao carregar produtos.'
    });
  }

  // Método para carregar os consumidores
  carregarConsumidores(): void {
    this.consumidorService.listarConsumidores().subscribe({
      next: data => this.consumidores = data,
      error: () => this.mensagemErro = 'Erro ao carregar consumidores.'
    });
  }

  // Método para buscar entregas e aplicar paginação
  fetchEntregas(page: number): void {
    this.entregasService.listarEntregas(page, this.pageSize).subscribe({
      next: (data: PageEntregaResponse) => {
        this.entregas = data.content;  // A lista de entregas
        this.currentPage = data.number;  // Número da página atual
        this.totalPages = data.totalPages;  // Total de páginas
        this.totalElements = data.totalElements; // Total de elementos
        this.applyFilter();  // Aplica o filtro

       
      },
      error: () => {
        this.mensagemErro = 'Erro ao carregar entregas.';
        console.error(this.mensagemErro);
      }
    });
  }

  // Formatar data e hora no formato Brasileiro
  formatarDataHoraBrasil(dataIso: string | undefined): string {
    if (!dataIso) return '';

    const data = new Date(dataIso);

    return new Intl.DateTimeFormat('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      timeZone: 'America/Sao_Paulo'
    }).format(data);
  }

  // Método para adicionar uma nova entrega
  submitAddForm(): void {
    if (!this.novaEntrega.produtoId || !this.novaEntrega.consumidorId || !this.novaEntrega.quantidade) {
      this.mensagemErro = 'Todos os campos são obrigatórios.';
      return;
    }

    setTimeout(() => {
      this.mensagem = '';
      this.mensagemErro = '';
    }, 5000);  // Esconde a mensagem após 5 segundos

    const payload: EntregaRequest = {
      produtoId: this.novaEntrega.produtoId!,
      quantidade: this.novaEntrega.quantidade!,
      consumidorId: this.novaEntrega.consumidorId!,
      horarioEntrega: this.novaEntrega.horarioEntrega
    };

    this.entregasService.criarEntrega(payload).subscribe({
      next: response => {
        this.mensagem = response.mensagemEstoqueBaixo || 'Entrega criada com sucesso!';
        this.showAddForm = false;
        this.fetchEntregas(this.currentPage);
      },
      error: () => this.mensagemErro = 'Erro ao registrar entrega.'
    });
  }

  // Método para editar a entrega
  editEntrega(id: number): void {
    const entrega = this.entregas.find(e => e.id === id);
    if (entrega) {
      this.novaEntrega = {
        produtoId: entrega.produtoId,
        quantidade: entrega.quantidade,
        consumidorId: entrega.consumidorId,
        horarioEntrega: entrega.horarioEntrega
      };
      this.idEntregaParaEditar = entrega.id;
      this.showEditForm = true;
      this.showAddForm = false;
    }
  }

  // Submeter o formulário de edição
  submitEditForm(): void {
    if (!this.novaEntrega.produtoId || !this.novaEntrega.consumidorId || !this.novaEntrega.quantidade) {
      this.mensagemErro = 'Todos os campos são obrigatórios.';
      return;
    }

    const payload: EntregaRequest = {
      produtoId: this.novaEntrega.produtoId!,
      quantidade: this.novaEntrega.quantidade!,
      consumidorId: this.novaEntrega.consumidorId!,
      horarioEntrega: this.novaEntrega.horarioEntrega
    };

    if (this.idEntregaParaEditar !== null) {
      this.entregasService.editarEntrega(this.idEntregaParaEditar, payload).subscribe({
        next: () => {
          this.mensagem = 'Entrega atualizada com sucesso!';
          this.showEditForm = false;
          this.fetchEntregas(this.currentPage);
        },
        error: () => {
          this.mensagemErro = 'Erro ao atualizar entrega.';
        }
      });
    }
  }

  // Método para excluir uma entrega
  deleteEntrega(id: number): void {
    if (!confirm('Deseja realmente excluir esta entrega?')) return;

    this.entregasService.deletarEntrega(id).subscribe({
      next: () => {
        this.entregas = this.entregas.filter(e => e.id !== id);
        this.applyFilter();  // Aplica o filtro após a exclusão
      },
      error: () => alert('Erro ao deletar entrega.')
    });
  }

  // Buscar entregas por dia
  fetchEntregasPorDia(dia: string): void {
    this.entregasService.porDia(dia).subscribe({
      next: (entregas: EntregaResponse[]) => {
        this.porDia = entregas;
      },
      error: (error) => {
        this.mensagemErro = 'Erro ao buscar entregas para o dia especificado!';
        console.error(error);
      }
    });
  }

  // Navegar para a próxima página
  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) {
      this.fetchEntregas(this.currentPage + 1);
    }
  }

  // Navegar para a página anterior
  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.fetchEntregas(this.currentPage - 1);
    }
  }

  irParaPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPages) {
      this.fetchEntregas(pagina);
    }
  }

  // Método para aplicar filtros na lista de entregas
  applyFilter(): void {
    const term = this.searchTerm.toLowerCase();
    this.filteredEntregas = this.searchTerm
      ? this.entregas.filter(e =>
          e.nomeConsumidor.toLowerCase().includes(term) ||
          e.nomeProduto.toLowerCase().includes(term) ||
          e.nomeEntregador.toLowerCase().includes(term)
        )
      : [...this.entregas];
  }

  // Método para alternar o componente de busca
  toggleBuscaEntrega() {
    this.showBuscaEntrega = !this.showBuscaEntrega;
    if (this.showBuscaEntrega) {
      this.showAddForm = false;  // Fecha o formulário de nova entrega se o de busca for aberto
    }
  }

  onPageSizeChange(event: any): void {
    this.currentPage = 0;
    this.fetchEntregas(this.currentPage);
  }



    /**
   * Retorna as entregas realizadas por um consumidor específico dentro de um intervalo anual para uma organização, com paginação.
   *
   * @param consumidorId ID do consumidor
   * @param inicioAno Data de início do intervalo anual
   * @param fimAno Data de fim do intervalo anual
   * @param orgId ID da organização
   * @param page Número da página
   * @param size Tamanho da página
   */
    fetchEntregasPorConsumidorAno(
      consumidorId: number,
      inicioAno: string,
      fimAno: string,
      orgId?: number,
      page: number = this.currentPage,
      size: number = this.pageSize
    ): void {
      const org = orgId ?? this.orgId;
      if (!org) {
        this.mensagemErro = 'Organização não encontrada!';
        return;
      }
      this.entregasService.porConsumidorAno(consumidorId, inicioAno, fimAno, org, page, size).subscribe({
        next: (data: PageEntregaResponse) => {
          this.entregas = data.content;
          this.currentPage = data.number;
          this.totalPages = data.totalPages;
          this.totalElements = data.totalElements;
          this.applyFilter();
        },
        error: () => {
          this.mensagemErro = 'Erro ao carregar entregas por consumidor e ano.';
          console.error(this.mensagemErro);
        }
      });
    }
  
    /**
     * Retorna as entregas realizadas por um consumidor específico dentro de um intervalo mensal para uma organização, com paginação.
     *
     * @param consumidorId ID do consumidor
     * @param inicioMes Data de início do intervalo mensal
     * @param fimMes Data de fim do intervalo mensal
     * @param orgId ID da organização
     * @param page Número da página
     * @param size Tamanho da página
     */
    fetchEntregasPorConsumidorMes(
      consumidorId: number,
      inicioMes: string,
      fimMes: string,
      orgId?: number,
      page: number = this.currentPage,
      size: number = this.pageSize
    ): void {
      const org = orgId ?? this.orgId;
      if (!org) {
        this.mensagemErro = 'Organização não encontrada!';
        return;
      }
      this.entregasService.porConsumidorMes(consumidorId, inicioMes, fimMes, org, page, size).subscribe({
        next: (data: PageEntregaResponse) => {
          this.entregas = data.content;
          this.currentPage = data.number;
          this.totalPages = data.totalPages;
          this.totalElements = data.totalElements;
          this.applyFilter();
        },
        error: () => {
          this.mensagemErro = 'Erro ao carregar entregas por consumidor e mês.';
          console.error(this.mensagemErro);
        }
      });
    }
  
    /**
     * Retorna as entregas realizadas por uma organização dentro de um intervalo anual, com paginação.
     *
     * @param inicioAno Data de início do intervalo anual
     * @param fimAno Data de fim do intervalo anual
     * @param orgId ID da organização
     * @param page Número da página
     * @param size Tamanho da página
     */
    fetchEntregasPorOrganizacaoAno(
      inicioAno: string,
      fimAno: string,
      orgId?: number,
      page: number = this.currentPage,
      size: number = this.pageSize
    ): void {
      const org = orgId ?? this.orgId;
      if (!org) {
        this.mensagemErro = 'Organização não encontrada!';
        return;
      }
      this.entregasService.porOrganizacaoAno(inicioAno, fimAno, org, page, size).subscribe({
        next: (data: PageEntregaResponse) => {
          this.entregas = data.content;
          this.currentPage = data.number;
          this.totalPages = data.totalPages;
          this.totalElements = data.totalElements;
          this.applyFilter();
        },
        error: () => {
          this.mensagemErro = 'Erro ao carregar entregas por organização e ano.';
          console.error(this.mensagemErro);
        }
      });
    }
  
    /**
     * Retorna as entregas realizadas por uma organização dentro de um intervalo mensal, com paginação.
     *
     * @param inicioMes Data de início do intervalo mensal
     * @param fimMes Data de fim do intervalo mensal
     * @param orgId ID da organização
     * @param page Número da página
     * @param size Tamanho da página
     */
    fetchEntregasPorOrganizacaoMes(
      inicioMes: string,
      fimMes: string,
      orgId?: number,
      page: number = this.currentPage,
      size: number = this.pageSize
    ): void {
      const org = orgId ?? this.orgId;
      if (!org) {
        this.mensagemErro = 'Organização não encontrada!';
        return;
      }
      this.entregasService.porOrganizacaoMes(inicioMes, fimMes, org, page, size).subscribe({
        next: (data: PageEntregaResponse) => {
          this.entregas = data.content;
          this.currentPage = data.number;
          this.totalPages = data.totalPages;
          this.totalElements = data.totalElements;
          this.applyFilter();
        },
        error: () => {
          this.mensagemErro = 'Erro ao carregar entregas por organização e mês.';
          console.error(this.mensagemErro);
        }
      });
    }




  }
