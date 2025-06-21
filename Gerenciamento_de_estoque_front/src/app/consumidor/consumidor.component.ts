// src/app/components/consumers.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConsumidorService } from '../services/consumidor.service';
import { Consumer } from '../models/consumer.model';
import { ConsumerPagedResponse } from '../models/consumer-paged-response.model';

@Component({
  selector: 'app-consumers',
  templateUrl: './consumidor.component.html',
  styleUrls: ['./consumidor.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
})
export class ConsumersComponent implements OnInit {
  consumerForm: FormGroup;
  searchTerm = '';
  consumers: Consumer[] = [];
  filteredConsumers: Consumer[] = [];
  showList = true;
  showAddForm = false;
  editingConsumer = false;
  mensagem = '';
  mensagemErro = '';
  mensagemTipo = '';

  // Paginação
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  constructor(
    private consumidorService: ConsumidorService,
    private fb: FormBuilder
  ) {
    this.consumerForm = this.fb.group({
      id: [null],
      nome: ['', Validators.required],
      cpf: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
      endereco: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.fetchConsumers();
  }

  /** Busca paginada de consumidores */
 fetchConsumers(page: number = 0): void {
  this.consumidorService
    .listarConsumidoresPaged(page, this.pageSize)  // Chama o método que retorna a resposta paginada
    .subscribe({
      next: (resp: ConsumerPagedResponse) => {
        // Atualizando os metadados de paginação
        this.currentPage   = resp.number;           // Página atual
        this.pageSize      = resp.size;             // Tamanho da página
        this.totalPages    = resp.totalPages;       // Total de páginas
        this.totalElements = resp.totalElements;    // Total de consumidores

        // Ordenando os consumidores pelo nome
        this.consumers = resp.content.sort((a, b) => a.nome.localeCompare(b.nome));

        // Aplicando filtro (se houver algum)
        this.applyFilter();  // Função para aplicar filtros nos consumidores

        // Mensagem de sucesso pode ser removida, se não for necessária.
        // this.onSuccess('Lista carregada com sucesso');
      },
      error: () => {
        // Em caso de erro, exibe uma mensagem de erro
        this.onError('Erro ao buscar consumidores!');
      }
    });
}


  /** Mostrar lista */
  toggleList(): void {
    this.showList = true;
    this.showAddForm = false;
    this.editingConsumer = false;
    this.resetForm();
    this.fetchConsumers(this.currentPage);
  }

  /** Mostrar form de adicionar */
  toggleAddForm(): void {
    this.showAddForm = true;
    this.showList = false;
    this.editingConsumer = false;
    this.resetForm();
  }

  submitAddForm(): void {
    if (this.consumerForm.invalid) {
      this.onError('Preencha todos os campos corretamente!');
      return;
    }
    this.editingConsumer ? this.updateConsumer() : this.createConsumer();
  }

  createConsumer(): void {
    const novo = this.consumerForm.value as Partial<Consumer>;
    this.consumidorService.criarConsumidor(novo).subscribe({
      next: () => this.onSuccess('Consumidor adicionado!'),
      error: err => this.onError(err.error?.error || 'Erro ao adicionar consumidor!')
    });
  }

  updateConsumer(): void {
    const upd = this.consumerForm.value as Partial<Consumer>;
    if (!upd.id) {
      this.onError('ID obrigatório para edição');
      return;
    }
    this.consumidorService.editarConsumidor(upd).subscribe({
      next: () => this.onSuccess('Consumidor atualizado!'),
      error: () => this.onError('Erro ao editar consumidor!')
    });
  }

  deleteConsumer(id: number): void {
    if (!confirm('Confirma exclusão?')) return;
    this.consumidorService.deletarConsumidor(id).subscribe({
      next: () => this.onSuccess('Consumidor deletado!'),
      error: () => this.onError('Erro ao deletar consumidor!')
    });
  }

  applyFilter(): void {
    const t = this.searchTerm.trim().toLowerCase();
    this.filteredConsumers = t
      ? this.consumers.filter(c => c.nome.toLowerCase().includes(t))
      : [...this.consumers];
  }

  editConsumer(c: Consumer): void {
    this.editingConsumer = true;
    this.showAddForm = true;
    this.showList = false;
    this.consumerForm.patchValue(c);
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.fetchConsumers(this.currentPage - 1);
    }
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) {
      this.fetchConsumers(this.currentPage + 1);
    }
  }

  resetForm(): void {
    this.consumerForm.reset();
    this.searchTerm = '';
    this.mensagem = '';
    this.mensagemErro = '';
    this.mensagemTipo = '';
  }

  private onSuccess(msg: string): void {
    this.mensagem = msg;
    this.mensagemTipo = 'sucesso';
    this.toggleList();  // volta para lista após operação de sucesso
  }

  private onError(msg: string): void {
    this.mensagemErro = msg;
    this.mensagemTipo = 'erro';
  }
}
