
// src/app/consumers/consumers.component.ts


import { Component, OnInit } from '@angular/core';
import { ConsumidorService } from '../services/consumidor.service';
import { Consumer } from '../models/consumer.model'; // Importe a interface Consumer
import { ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-consumers',
  standalone: true, // Torna o componente standalone
  templateUrl: './consumidor.component.html',
  styleUrls: ['./consumidor.component.scss'],
  imports: [ReactiveFormsModule, CommonModule, FormsModule],
})
export class ConsumersComponent implements OnInit {
  consumers: Consumer[] = [];
  filteredConsumers: Consumer[] = [];
  searchTerm: string = '';

  constructor(private consumidorService: ConsumidorService) {}

  ngOnInit(): void {
    this.fetchConsumers();
  }

  // Busca os consumidores do back-end e ordena alfabeticamente pelo nome
  fetchConsumers(): void {
    this.consumidorService.listarConsumidores().subscribe((data) => {
      this.consumers = data.sort((a, b) => a.nome.localeCompare(b.nome));
      this.applyFilter();
    });
  }

  // Aplica o filtro de acordo com o searchTerm
  applyFilter(): void {
    if (this.searchTerm.trim() === '') {
      this.filteredConsumers = this.consumers;
    } else {
      this.filteredConsumers = this.consumers.filter((consumer) =>
        consumer.nome.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }
  }
}
