
// src/app/consumers/consumers.component.ts
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
  showList: boolean = false;
  showAddForm: boolean = false;

  novoConsumidor: Partial<Consumer> = { nome: '', cpf: '' }; // Modelo para o novo consumidor

  constructor(private consumidorService: ConsumidorService) {}

  ngOnInit(): void {}

  // Alterna a exibição da lista de consumidores
  toggleList(): void {
    this.showList = !this.showList;
    if (this.showList) {
      this.fetchConsumers();
    }
  }

  // Alterna a exibição do formulário de adicionar
  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
  }

  // Busca os consumidores do back-end
  fetchConsumers(): void {
    this.consumidorService.listarConsumidores().subscribe((data) => {
      this.consumers = data.sort((a, b) => a.nome.localeCompare(b.nome));
      this.applyFilter();
    });
  }

  // Envia os dados do novo consumidor para o backend
  submitAddForm(): void {
    this.consumidorService.criarConsumidor(this.novoConsumidor).subscribe({
      next: () => {
        alert('Consumidor adicionado com sucesso!');
        this.novoConsumidor = { nome: '', cpf: '' }; // Limpa o formulário
        this.showAddForm = false; // Fecha o formulário
        this.fetchConsumers(); // Atualiza a lista
      },
      error: (err) => {
        console.error('Erro ao adicionar consumidor:', err);
        alert('Erro ao adicionar consumidor!');
      },
    });
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
}



   

