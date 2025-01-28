
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para diretivas como *ngIf e *ngFor
import { FormsModule } from '@angular/forms'; // Para [(ngModel)]
import { EntregasService } from '../services/entregas.service'; // Serviço para consumir API
import { Entrega } from '../models/entrega.model'; // Modelo Entrega
import { PageEntregaResponse } from '../models/PageEntregaResponse.model'; // Modelo PageEntregaResponse
import { EntregaRequest } from '../models/EntregaRequest.model'; // Modelo EntregaRequest

@Component({
  selector: 'app-entregas',
  standalone: true,
  templateUrl: './entregas.component.html',
  styleUrls: ['./entregas.component.scss'],
  imports: [CommonModule, FormsModule],
})
export class EntregasComponent implements OnInit {
  entregas: Entrega[] = [];
  filteredEntregas: Entrega[] = [];
  searchTerm: string = '';
  showList: boolean = false;
  showAddForm: boolean = false;

  // Objeto inicializado corretamente
  novaEntrega: Partial<EntregaRequest> = {
    produtoId: undefined,
    quantidade: undefined,
    consumidor: {
      nome: '',
      cpf: '',
      endereco: '',
    },
  };

  currentPage: number = 0;
  totalPages: number = 0;
  pageSize: number = 20;

  mensagem: string = '';
  mensagemErro: string = '';

  constructor(private entregasService: EntregasService) {}

  ngOnInit(): void {}

  // Getter e setter para 'nome'
  get nome(): string {
    return this.novaEntrega.consumidor?.nome || '';
  }

  set nome(value: string) {
    if (this.novaEntrega.consumidor) {
      this.novaEntrega.consumidor.nome = value;
    }
  }

  // Getter e setter para 'cpf'
  get cpf(): string {
    return this.novaEntrega.consumidor?.cpf || '';
  }

  set cpf(value: string) {
    if (this.novaEntrega.consumidor) {
      this.novaEntrega.consumidor.cpf = value;
    }
  }

  // Getter e setter para 'endereco'
  get endereco(): string {
    return this.novaEntrega.consumidor?.endereco || '';
  }

  set endereco(value: string) {
    if (this.novaEntrega.consumidor) {
      this.novaEntrega.consumidor.endereco = value;
    }
  }

  // Alterna a exibição da lista de entregas
  toggleList(): void {
    this.showList = !this.showList;
    if (this.showList) {
      this.fetchEntregas(this.currentPage);
    }
  }

  // Alterna a exibição do formulário de adicionar
  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    if (!this.showAddForm) {
      this.clearMessages();
    }
  }

  // Busca as entregas do back-end com paginação
  fetchEntregas(page: number): void {
    this.entregasService.listarEntregas(page, this.pageSize).subscribe({
      next: (data: PageEntregaResponse) => {
        this.entregas = data.content.sort((a, b) =>
          a.nomeConsumidor.localeCompare(b.nomeConsumidor)
        );
        this.currentPage = data.number;
        this.totalPages = data.totalPages;
        this.applyFilter();
      },
      error: (err) => {
        console.error('Erro ao carregar entregas:', err);
        this.mensagemErro = 'Erro ao carregar entregas. Por favor, tente novamente.';
      },
    });
  }

  // Envia os dados da nova entrega para o backend
  submitAddForm(): void {
    if (
      this.novaEntrega.produtoId == null ||
      this.novaEntrega.quantidade == null ||
      !this.novaEntrega.consumidor?.nome?.trim() ||
      !this.novaEntrega.consumidor?.cpf?.trim() ||
      !this.novaEntrega.consumidor?.endereco?.trim()
    ) {
      this.mensagemErro = 'Todos os campos são obrigatórios.';
      return;
    }

    this.entregasService
      .criarEntrega(this.novaEntrega as EntregaRequest)
      .subscribe({
        next: (res: Entrega) => {
          this.mensagem = 'Entrega registrada com sucesso!';
          this.novaEntrega = {
            produtoId: undefined,
            quantidade: undefined,
            consumidor: { nome: '', cpf: '', endereco: '' },
          };
          this.showAddForm = false;
          this.fetchEntregas(this.currentPage);
          this.clearMessages(3000); // Limpa mensagens após 3 segundos
        },
        error: (err) => {
          console.error('Erro ao registrar entrega:', err);
          this.mensagemErro = 'Erro ao registrar entrega. Por favor, tente novamente.';
        },
      });
  }

  // Aplica o filtro de busca
  applyFilter(): void {
    if (this.searchTerm.trim() === '') {
      this.filteredEntregas = this.entregas;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredEntregas = this.entregas.filter(
        (entrega) =>
          entrega.nomeConsumidor.toLowerCase().includes(term) ||
          entrega.nomeProduto.toLowerCase().includes(term) ||
          entrega.nomeEntregador.toLowerCase().includes(term)
      );
    }
  }

  // Deleta uma entrega pelo ID
  deleteEntrega(id: number): void {
    if (confirm('Tem certeza que deseja deletar esta entrega?')) {
      this.entregasService.deletarEntrega(id).subscribe({
        next: () => {
          alert('Entrega deletada com sucesso!');
          this.entregas = this.entregas.filter((entrega) => entrega.id !== id);
          this.applyFilter();
        },
        error: (err) => {
          console.error('Erro ao deletar entrega:', err);
          alert('Erro ao deletar entrega. Por favor, tente novamente.');
        },
      });
    }
  }

  // Navegação de páginas
  proximaPagina(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.fetchEntregas(this.currentPage + 1);
    }
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.fetchEntregas(this.currentPage - 1);
    }
  }

  // Limpa mensagens de erro e sucesso
  private clearMessages(delay: number = 0): void {
    setTimeout(() => {
      this.mensagem = '';
      this.mensagemErro = '';
    }, delay);
  }
}
