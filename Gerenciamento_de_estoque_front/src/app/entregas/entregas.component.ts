
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EntregasService } from '../services/entregas.service';
import { ProdutoService } from '../services/produto.service';
import { ConsumidorService } from '../services/consumidor.service';
import { Entrega } from '../models/entrega.model';
import { PageEntregaResponse } from '../models/PageEntregaResponse.model';
import { Produto } from '../models/produto.model';
import { Consumer } from '../models/consumer.model';
import { EntregaRequest } from '../models/EntregaRequest.model'; 

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

  produtos: Produto[] = [];
  consumidores: Consumer[] = [];

  selectedProduto: Produto | undefined;
  selectedConsumidor: Consumer | undefined;

  novaEntrega: Partial<EntregaRequest> = {
    produtoId: undefined,
    quantidade: 1,
    consumidorId: undefined,
    consumidor: {
      nome: '',
      cpf: '',
    },
  };

  currentPage: number = 0;
  totalPages: number = 0;
  pageSize: number = 20;

  mensagem: string = '';
  mensagemErro: string = '';

  // Remover a definição do orgId manual e obter ele diretamente do ConsumidorService
  constructor(
    private entregasService: EntregasService,
    private produtoService: ProdutoService,
    private consumidorService: ConsumidorService
  ) {}

  ngOnInit(): void {
    this.carregarProdutos();
    this.carregarConsumidores();
  }

  carregarProdutos(): void {
    this.produtoService.listarProdutos(0, 100).subscribe({
      next: (data) => {
        this.produtos = data.content;
        console.log('Produtos carregados:', this.produtos);
        this.onProdutoChange();
      },
      error: (err) => {
        console.error('Erro ao carregar produtos:', err);
        this.mensagemErro = 'Erro ao carregar produtos. Por favor, tente novamente.';
      },
    });
  }

  carregarConsumidores(): void {
    // Agora o orgId já está sendo gerenciado no ConsumidorService, então basta chamar a função
    this.consumidorService.listarConsumidoresPorOrg().subscribe({
      next: (data) => {
        if (data && data.consumidores) {
          this.consumidores = data.consumidores;  // Atribui a lista de consumidores
          console.log('Consumidores carregados:', this.consumidores);
          this.mensagem = data.message;  // Exibe a mensagem de sucesso (caso não tenha consumidores, ela será uma mensagem informativa)
          this.onConsumidorChange();  // Chama um método que faz o processamento após a carga dos consumidores
        } else {
          this.mensagemErro = 'Nenhum consumidor encontrado.';  // Caso não haja consumidores
        }
      },
      error: (err) => {
        console.error('Erro ao carregar consumidores:', err);
        this.mensagemErro = 'Erro ao carregar consumidores. Por favor, tente novamente.';
      },
    });
  }
  

  onProdutoChange(): void {
    if (this.novaEntrega.produtoId != null && this.produtos.length > 0) {
      this.selectedProduto = this.produtos.find(p => p.id === Number(this.novaEntrega.produtoId));
      console.log('Produto selecionado:', this.selectedProduto);
    } else {
      this.selectedProduto = undefined;
    }
  }

  onConsumidorChange(): void {
    if (this.novaEntrega.consumidorId != null && this.consumidores.length > 0) {
      this.selectedConsumidor = this.consumidores.find(c => c.id === Number(this.novaEntrega.consumidorId));
      console.log('Consumidor selecionado:', this.selectedConsumidor);
    } else {
      this.selectedConsumidor = undefined;
    }
  }

  toggleList(): void {
    this.showList = !this.showList;
    if (this.showList) {
      this.fetchEntregas(this.currentPage);
    }
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    if (!this.showAddForm) {
      this.clearMessages();
      this.selectedProduto = undefined;
      this.selectedConsumidor = undefined;
      this.novaEntrega.produtoId = undefined;
      this.novaEntrega.quantidade = 1;
      this.novaEntrega.consumidorId = undefined;
      this.novaEntrega.consumidor = { nome: '', cpf: '' };
    }
  }

  fetchEntregas(page: number): void {
    this.entregasService.listarEntregas(page, this.pageSize).subscribe({
      next: (data: PageEntregaResponse) => {
        this.entregas = data.content.map((entrega) => {
          let formattedDate = '';
          if (entrega.horarioEntrega) {
            const horarioISO = new Date(entrega.horarioEntrega);
            if (!isNaN(horarioISO.getTime())) {
              const day = String(horarioISO.getDate()).padStart(2, '0');
              const month = String(horarioISO.getMonth() + 1).padStart(2, '0');
              const year = String(horarioISO.getFullYear()).slice(-2);
              const hours = String(horarioISO.getHours()).padStart(2, '0');
              const minutes = String(horarioISO.getMinutes()).padStart(2, '0');
              formattedDate = `${day}/${month}/${year} ${hours}:${minutes}`;
            } else {
              console.error('Formato de data inválido:', entrega.horarioEntrega);
              formattedDate = 'Data inválida';
            }
          }
          return {
            ...entrega,
            horarioEntrega: formattedDate,
          };
        });
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

  submitAddForm(): void {
    if (!this.novaEntrega.produtoId || !this.novaEntrega.consumidorId || !this.novaEntrega.quantidade) {
      this.mensagemErro = 'Todos os campos são obrigatórios.';
      return;
    }

    if (!this.selectedConsumidor) {
      this.mensagemErro = 'Consumidor selecionado não é válido.';
      return;
    }

    const produtoId = Number(this.novaEntrega.produtoId);

    const entregaPayload = {
      consumidor: {
        id: this.novaEntrega.consumidorId,
        nome: this.selectedConsumidor?.nome,
        cpf: this.selectedConsumidor?.cpf,
      },
      produtoId: produtoId,
      quantidade: this.novaEntrega.quantidade,
    };

    this.entregasService.criarEntrega(entregaPayload).subscribe({
      next: (response) => {
        this.mensagem = response.message;
        this.novaEntrega = { produtoId: undefined, consumidorId: undefined, quantidade: undefined };
        this.showAddForm = false;
      },
      error: (err) => {
        console.error('Erro ao registrar entrega:', err);
        this.mensagemErro = 'Erro ao registrar entrega.';
      },
    });
  }

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

  private clearMessages(delay: number = 0): void {
    setTimeout(() => {
      this.mensagem = '';
      this.mensagemErro = '';
    }, delay);
  }
}
