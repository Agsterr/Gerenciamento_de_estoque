import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ConsumidorService } from '../services/consumidor.service';
import { Consumer } from '../models/consumer.model';

@Component({
  selector: 'app-consumers',
  templateUrl: './consumidor.component.html',
  styleUrls: ['./consumidor.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
})
export class ConsumersComponent implements OnInit {
  consumerForm: FormGroup;  // FormGroup reativo para criação/edição
  searchTerm: string = '';  // Termo atual de busca
  consumers: Consumer[] = [];  // Lista completa de consumidores
  filteredConsumers: Consumer[] = [];  // Lista filtrada por busca
  showList = true;  // Controla exibição da lista
  showAddForm = false;  // Controla exibição do formulário
  editingConsumer = false;  // Flag de edição
  mensagem = '';  // Mensagem de sucesso
  mensagemErro = '';  // Mensagem de erro
  mensagemTipo = '';  // Tipo da mensagem: 'sucesso' ou 'erro'

  constructor(
    private consumidorService: ConsumidorService,
    private fb: FormBuilder,
    private router: Router
  ) {
    // Inicializa o FormGroup com campos e validações
    this.consumerForm = this.fb.group({
      id: [null],
      nome: ['', Validators.required],
      cpf: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
      endereco: ['', Validators.required],
    });
  }

  /**
   * Ciclo de vida OnInit: busca consumidores ao carregar o componente
   */
  ngOnInit(): void {
    this.fetchConsumers();
  }

  /**
   * Busca consumidores do backend, ordena e aplica filtro inicial
   */
  fetchConsumers(): void {
    this.consumidorService.listarConsumidores().subscribe({
      next: (data) => {
        this.consumers = data.sort((a, b) =>
          a.nome.toLowerCase().localeCompare(b.nome.toLowerCase())
        );
        this.mensagem = 'Lista de consumidores carregada com sucesso.';
        this.mensagemTipo = 'sucesso';
        this.applyFilter();
      },
      error: (err) => {
        console.error('Erro ao buscar consumidores:', err);
        this.mensagemErro = 'Erro ao buscar consumidores!';
        this.mensagemTipo = 'erro';
      },
    });
  }

  /**
   * Alterna para exibir a lista de consumidores
   */
  toggleList(): void {
    this.showList = true;
    this.showAddForm = false;
    this.editingConsumer = false;
    this.resetForm();
    this.fetchConsumers();
  }

  /**
   * Alterna para exibir o formulário de adição de consumidor
   */
  toggleAddForm(): void {
    this.showAddForm = true;
    this.showList = false;
    this.editingConsumer = false;
    this.resetForm();
  }

  /**
   * Submete o formulário: Decide entre criar ou editar
   */
  submitAddForm(): void {
    if (this.consumerForm.invalid) {
      this.mensagemErro = 'Por favor, preencha todos os campos corretamente!';
      this.mensagemTipo = 'erro';
      return;
    }
    this.editingConsumer ? this.updateConsumer() : this.createConsumer();
  }

  /**
   * Chama o serviço para criar um novo consumidor
   */
  createConsumer(): void {
    const novoConsumidor: Partial<Consumer> = this.consumerForm.value;
    this.consumidorService.criarConsumidor(novoConsumidor).subscribe({
      next: () => this.onSuccess('Consumidor adicionado com sucesso!'),
      error: () => this.onError('Erro ao adicionar consumidor!'),
    });
  }

  /**
   * Chama o serviço para atualizar um consumidor existente
   */
  updateConsumer(): void {
    const updated: Partial<Consumer> = this.consumerForm.value;
    if (!updated.id) {
      this.onError('ID do consumidor é obrigatório para edição.');
      return;
    }
    this.consumidorService.editarConsumidor(updated).subscribe({
      next: () => this.onSuccess('Consumidor atualizado com sucesso!'),
      error: () => this.onError('Erro ao editar consumidor!'),
    });
  }

  /**
   * Chama o serviço para deletar um consumidor pelo ID
   */
  deleteConsumer(id: number): void {
    if (!confirm('Tem certeza que deseja deletar este consumidor?')) return;
    this.consumidorService.deletarConsumidor(id).subscribe({
      next: () => this.onSuccess('Consumidor deletado com sucesso!'),
      error: () => this.onError('Erro ao deletar consumidor!'),
    });
  }

  /**
   * Filtra a lista de consumidores pelo termo de busca
   */
  applyFilter(): void {
    const term = this.searchTerm.trim().toLowerCase();
    this.filteredConsumers = term
      ? this.consumers.filter(c => c.nome.toLowerCase().includes(term))
      : [...this.consumers];
  }

  /**
   * Prepara o formulário para edição, populando campos
   */
  editConsumer(consumer: Consumer): void {
    this.editingConsumer = true;
    this.showAddForm = true;
    this.showList = false;
    this.consumerForm.patchValue(consumer);
  }

  /**
   * Reseta o formulário e limpa mensagens e filtros
   */
  resetForm(): void {
    this.consumerForm.reset();
    this.searchTerm = '';
    this.mensagemErro = '';
    this.mensagem = '';
    this.mensagemTipo = '';
  }

  /**
   * Handler genérico para sucesso: exibe mensagem e retorna à lista
   */
  private onSuccess(msg: string): void {
    this.mensagem = msg;
    this.mensagemTipo = 'sucesso';
    this.toggleList();
  }

  /**
   * Handler genérico para erro: exibe mensagem de erro
   */
  private onError(msg: string): void {
    this.mensagemErro = msg;
    this.mensagemTipo = 'erro';
  }
}
