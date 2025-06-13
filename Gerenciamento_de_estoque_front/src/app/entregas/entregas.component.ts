import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EntregasService } from '../services/entregas.service';
import { ProdutoService } from '../services/produto.service';
import { ConsumidorService } from '../services/consumidor.service';
import { EntregaResponse } from '../models/src/app/models/entrega/entrega-response.model';
import { PageEntregaResponse } from '../models/src/app/models/entrega/PageEntregaResponse.model';
import { EntregaRequest } from '../models/src/app/models/entrega/entrega-request.model';

@Component({
  selector: 'app-entregas',
  standalone: true,
  imports: [CommonModule, FormsModule],
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

  produtos: any[] = [];
  consumidores: any[] = [];

  novaEntrega: Partial<EntregaRequest> = {};
  currentPage = 0;
  totalPages = 0;
  pageSize = 20;

  mensagem = '';
  mensagemErro = '';

  // Guarda o ID da entrega que será editada
  idEntregaParaEditar: number | null = null;

  constructor(
    private entregasService: EntregasService,
    private produtoService: ProdutoService,
    private consumidorService: ConsumidorService
  ) {}

 ngOnInit(): void {
  this.carregarProdutos();
  this.carregarConsumidores();
  this.showList = true;       // garante que a lista fique visível
  this.fetchEntregas(this.currentPage);
}



  carregarProdutos(): void {
    this.produtoService.listarProdutos(0, 100).subscribe({
      next: data => this.produtos = data.content,
      error: () => this.mensagemErro = 'Erro ao carregar produtos.'
    });
  }

  carregarConsumidores(): void {
    this.consumidorService.listarConsumidores().subscribe({
      next: data => this.consumidores = data,
      error: () => this.mensagemErro = 'Erro ao carregar consumidores.'
    });
  }

  toggleList(): void {
    this.showList = !this.showList;
    if (this.showList) {
      this.fetchEntregas(this.currentPage);
    }
  }

  fetchEntregas(page: number): void {
    this.entregasService.listarEntregas(page, this.pageSize).subscribe({
      next: (data: PageEntregaResponse) => {
        this.entregas = data.content;
        this.currentPage = data.page.number;
        this.totalPages = data.page.totalPages;
        this.applyFilter();
      },
      error: () => this.mensagemErro = 'Erro ao carregar entregas.'
    });
  }

 submitAddForm(): void {
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

  this.entregasService.criarEntrega(payload).subscribe({
    next: response => {
      this.mensagem = response.mensagemEstoqueBaixo || 'Entrega criada com sucesso!';
      this.showAddForm = false;
      this.fetchEntregas(this.currentPage);
    },
    error: () => this.mensagemErro = 'Erro ao registrar entrega.'
  });
}


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

  deleteEntrega(id: number): void {
    if (!confirm('Deseja realmente excluir esta entrega?')) return;

    this.entregasService.deletarEntrega(id).subscribe({
      next: () => {
        this.entregas = this.entregas.filter(e => e.id !== id);
        this.applyFilter();
      },
      error: () => alert('Erro ao deletar entrega.')
    });
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
}
