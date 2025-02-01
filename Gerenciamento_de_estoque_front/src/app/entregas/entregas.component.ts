
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EntregasService } from '../services/entregas.service';
import { ProdutoService } from '../services/produto.service';
import { ConsumidorService } from '../services/consumidor.service';
import { Entrega } from '../models/entrega.model';
import { PageEntregaResponse } from '../models/PageEntregaResponse.model';
import { EntregaRequest } from '../models/EntregaRequest.model';
import { Produto } from '../models/produto.model';
import { Consumer } from '../models/consumer.model';

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
        this.onProdutoChange(); // Chama a função de seleção após os produtos serem carregados
      },
      error: (err) => {
        console.error('Erro ao carregar produtos:', err);
        this.mensagemErro = 'Erro ao carregar produtos. Por favor, tente novamente.';
      },
    });
  }

  carregarConsumidores(): void {
    this.consumidorService.listarConsumidores().subscribe({
      next: (data) => {
        this.consumidores = data;
        console.log('Consumidores carregados:', this.consumidores);
        this.onConsumidorChange(); // Chama a função de seleção após os consumidores serem carregados
      },
      error: (err) => {
        console.error('Erro ao carregar consumidores:', err);
        this.mensagemErro = 'Erro ao carregar consumidores. Por favor, tente novamente.';
      },
    });
  }

  onProdutoChange(): void {
    console.log('Produto ID:', this.novaEntrega.produtoId); // Verifique o produtoId
    if (this.novaEntrega.produtoId != null && this.produtos.length > 0) {
      // Garantindo que a comparação de ID seja feita corretamente
      this.selectedProduto = this.produtos.find(p => p.id === Number(this.novaEntrega.produtoId));
      console.log('Produto selecionado:', this.selectedProduto); // Verifique o produto selecionado
    } else {
      this.selectedProduto = undefined;
    }
  }

  onConsumidorChange(): void {
    console.log('Consumidor ID:', this.novaEntrega.consumidorId); // Verifique o consumidorId
    if (this.novaEntrega.consumidorId != null && this.consumidores.length > 0) {
      // Garantindo que a comparação de ID seja feita corretamente
      this.selectedConsumidor = this.consumidores.find(c => c.id === Number(this.novaEntrega.consumidorId));
      console.log('Consumidor selecionado:', this.selectedConsumidor); // Verifique o consumidor selecionado
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
    console.log('Produto ID:', this.novaEntrega.produtoId);
    console.log('Consumidor ID:', this.novaEntrega.consumidorId);
    console.log('Quantidade:', this.novaEntrega.quantidade);
  
    // Verificar se todos os campos obrigatórios foram preenchidos
    if (!this.novaEntrega.produtoId || !this.novaEntrega.consumidorId || !this.novaEntrega.quantidade) {
      this.mensagemErro = 'Todos os campos são obrigatórios.';
      return;
    }
  
    // Verificar se o consumidor foi selecionado corretamente
    if (!this.selectedConsumidor) {
      this.mensagemErro = 'Consumidor selecionado não é válido.';
      console.log('Erro: Nenhum consumidor foi selecionado');
      return;
    }
  
    // Garantir que o produtoId seja tratado como número
    const produtoId = Number(this.novaEntrega.produtoId);
  
    // Ajustar o formato para enviar ao backend conforme o formato esperado
    const entregaPayload = {
      consumidor: {
        id: this.novaEntrega.consumidorId,
        nome: this.selectedConsumidor?.nome,
        cpf: this.selectedConsumidor?.cpf,
      },
      produtoId: produtoId,
      quantidade: this.novaEntrega.quantidade,
    };
  
    console.log('Payload enviado para o servidor:', entregaPayload);
  
    // Envia a entrega ao backend
    this.entregasService.criarEntrega(entregaPayload).subscribe({
      next: (response) => {
        // A resposta agora contém 'message' e 'data'
        this.mensagem = response.message;  // Exibe a mensagem
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
