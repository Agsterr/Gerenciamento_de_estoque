import { Component, OnInit } from '@angular/core';
import { ConsumidorService } from '../services/consumidor.service';
import { Consumer } from '../models/consumer.model';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-consumers',
  templateUrl: './consumidor.component.html',
  styleUrls: ['./consumidor.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
})
export class ConsumersComponent implements OnInit {
  consumerForm: FormGroup;
  consumers: Consumer[] = [];
  filteredConsumers: Consumer[] = [];
  searchTerm: string = '';
  showList: boolean = false;
  showAddForm: boolean = false;
  editingConsumer: boolean = false;
  novoConsumidor: Partial<Consumer> = { nome: '', cpf: '', endereco: '' };
  mensagem: string = '';
  mensagemErro: string = '';
  mensagemTipo: string = '';

  constructor(
    private consumidorService: ConsumidorService,
    private fb: FormBuilder,
    private router: Router
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

 // Carrega consumidores da API e aplica filtro após ordenação alfabética
fetchConsumers(): void {
  this.consumidorService.listarConsumidores().subscribe({
    next: (data: Consumer[]) => {
      // Ordena os consumidores por nome (ordem alfabética, sem diferenciar maiúsculas de minúsculas)
      this.consumers = data.sort((a, b) =>
        a.nome.toLowerCase().localeCompare(b.nome.toLowerCase())
      );

      // Exibe mensagem de sucesso
      this.mensagem = 'Lista de consumidores carregada com sucesso.';
      this.mensagemTipo = 'sucesso';

      // Atualiza a lista exibida aplicando o filtro de busca, se houver
      this.applyFilter();
    },
    error: (err: any) => {
      // Loga o erro no console para depuração
      console.error('Erro ao buscar consumidores:', err);

      // Exibe mensagem de erro ao usuário
      this.mensagemErro = 'Erro ao buscar consumidores!';
      this.mensagemTipo = 'erro';
    },
  });
}



  toggleList(): void {
    this.showList = !this.showList;
    this.showAddForm = false;
    if (this.showList) {
      this.fetchConsumers();
    }
  }

  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    this.showList = false;
    this.editingConsumer = false;
    this.consumerForm.reset();
  }

  submitAddForm(): void {
    if (this.consumerForm.invalid) {
      this.mensagemErro = 'Por favor, preencha todos os campos corretamente!';
      this.mensagemTipo = 'erro';
      return;
    }

    if (this.editingConsumer) {
      this.updateConsumer();
    } else {
      this.createConsumer();
    }
  }

  createConsumer(): void {
    const novoConsumidor: Partial<Consumer> = this.consumerForm.value;

    this.consumidorService.criarConsumidor(novoConsumidor).subscribe({
      next: (response) => {
        this.mensagem = 'Consumidor adicionado com sucesso!';
        this.mensagemTipo = 'sucesso';
        this.resetForm();
        this.fetchConsumers();
      },
      error: (err) => {
        console.error('Erro ao criar consumidor:', err);
        this.mensagemErro = 'Erro ao adicionar consumidor!';
        this.mensagemTipo = 'erro';
      }
    });
  }

  updateConsumer(): void {
    const updatedConsumer: Partial<Consumer> = this.consumerForm.value;

    if (!updatedConsumer.id) {
      const storedId = localStorage.getItem('consumerId');
      if (storedId) {
        updatedConsumer.id = parseInt(storedId, 10);
      } else {
        this.mensagemErro = 'ID do consumidor não encontrado.';
        this.mensagemTipo = 'erro';
        return;
      }
    }

    this.consumidorService.editarConsumidor(updatedConsumer).subscribe({
      next: (data) => {
        this.mensagem = 'Consumidor atualizado com sucesso!';
        this.mensagemTipo = 'sucesso';
        this.resetForm();
        this.fetchConsumers();
      },
      error: (err) => {
        console.error('Erro ao editar consumidor:', err);
        this.mensagemErro = 'Erro ao editar consumidor!';
        this.mensagemTipo = 'erro';
      },
    });
  }

  deleteConsumer(id: number): void {
    if (confirm('Tem certeza que deseja deletar este consumidor?')) {
      this.consumidorService.deletarConsumidor(id).subscribe({
        next: () => {
          this.mensagem = 'Consumidor deletado com sucesso!';
          this.mensagemTipo = 'sucesso';
          this.consumers = this.consumers.filter((consumer) => consumer.id !== id);
          this.applyFilter();
        },
        error: (err) => {
          this.mensagemErro = 'Erro ao deletar consumidor!';
          this.mensagemTipo = 'erro';
          console.error('Erro ao deletar consumidor:', err);
        },
      });
    }
  }

  editConsumer(consumer: Consumer): void {
    this.editingConsumer = true;
    this.showAddForm = true;
    this.showList = false;
    localStorage.setItem('consumerId', consumer.id.toString());

    this.consumerForm.patchValue({
      id: consumer.id,
      nome: consumer.nome,
      cpf: consumer.cpf,
      endereco: consumer.endereco
    });
  }

  applyFilter(): void {
    if (this.searchTerm.trim() === '') {
      this.filteredConsumers = this.consumers;
    } else {
      this.filteredConsumers = this.consumers.filter((consumer) =>
        consumer.nome.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }
  }

  resetForm(): void {
    this.consumerForm.reset();
    this.novoConsumidor = { nome: '', cpf: '', endereco: '' };
    this.editingConsumer = false;
    this.showAddForm = false;
    this.showList = true;
    localStorage.removeItem('consumerId');
  }
}
