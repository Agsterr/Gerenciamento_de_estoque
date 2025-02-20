
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common'; // Para diretivas como *ngIf e *ngFor
import { FormsModule } from '@angular/forms'; // Para [(ngModel)]
import { ConsumidorService } from '../services/consumidor.service'; // Serviço para consumir API
import { Consumer } from '../models/consumer.model'; // Modelo Consumer

@Component({
  selector: 'app-consumers',
  standalone: true,
  templateUrl: './consumidor.component.html',
  styleUrls: ['./consumidor.component.scss'],
  imports: [CommonModule, FormsModule], // Módulos necessários para standalone
})
export class ConsumersComponent implements OnInit {
  consumers: Consumer[] = [];
  filteredConsumers: Consumer[] = [];
  searchTerm: string = '';
  showList: boolean = false; // Exibe a lista de consumidores
  showAddForm: boolean = false; // Exibe o formulário de adicionar consumidor
  editingConsumer: boolean = false; // Indica se estamos editando um consumidor
  novoConsumidor: Partial<Consumer> = { nome: '', cpf: '' }; // Modelo para o novo consumidor

  constructor(private consumidorService: ConsumidorService) {}

  ngOnInit(): void {}

  // Alterna a exibição da lista de consumidores
  toggleList(): void {
    this.showList = !this.showList;
    this.showAddForm = false; // Oculta o formulário de adicionar quando a lista é exibida
    if (this.showList) {
      this.fetchConsumers(); // Se a lista for exibida, carrega os consumidores
    }
  }

  // Alterna a exibição do formulário de adicionar
  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    this.showList = false; // Esconde a lista ao exibir o formulário
  }

  // Busca os consumidores do back-end
  fetchConsumers(): void {
    this.consumidorService.listarConsumidores().subscribe((data) => {
      this.consumers = data.sort((a, b) => a.nome.localeCompare(b.nome));
      this.applyFilter();
    });
  }

  // Envia os dados do novo consumidor ou do consumidor editado para o backend
  submitAddForm(): void {
    if (this.editingConsumer) {
      // Se estiver editando, chama o método de edição
      this.consumidorService.editarConsumidor(this.novoConsumidor).subscribe({
        next: () => {
          alert('Consumidor atualizado com sucesso!');
          this.resetForm();
          this.fetchConsumers(); // Atualiza a lista
        },
        error: (err) => {
          console.error('Erro ao editar consumidor:', err);
          alert('Erro ao editar consumidor!');
        },
      });
    } else {
      // Caso contrário, chama o método de criação
      this.consumidorService.criarConsumidor(this.novoConsumidor).subscribe({
        next: () => {
          alert('Consumidor adicionado com sucesso!');
          this.resetForm();
          this.fetchConsumers(); // Atualiza a lista
        },
        error: (err) => {
          console.error('Erro ao adicionar consumidor:', err);
          alert('Erro ao adicionar consumidor!');
        },
      });
    }
  }

  // Aplica o filtro de busca
  applyFilter(): void {
    if (this.searchTerm.trim() === '') {
      this.filteredConsumers = this.consumers;
    } else {
      this.filteredConsumers = this.consumers.filter((consumer) =>
        consumer.nome.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }
  }

  // Deleta um consumidor pelo ID
  deleteConsumer(id: number): void {
    this.consumidorService.deletarConsumidor(id).subscribe(() => {
      alert('Consumidor deletado com sucesso!');
      this.consumers = this.consumers.filter((consumer) => consumer.id !== id);
      this.applyFilter();
    });
  }

  // Inicia a edição de um consumidor
  editConsumer(consumer: Consumer): void {
    this.editingConsumer = true; // Marca que estamos editando
    this.novoConsumidor = { ...consumer }; // Preenche o formulário com os dados do consumidor
    this.showAddForm = true; // Exibe o formulário de edição
    this.showList = false; // Esconde a lista enquanto edita
  }

  // Reseta o formulário e fecha a tela de edição
  resetForm(): void {
    this.novoConsumidor = { nome: '', cpf: '' }; // Limpa os campos
    this.showAddForm = false; // Fecha o formulário
    this.editingConsumer = false; // Reseta a flag de edição
  }
}
