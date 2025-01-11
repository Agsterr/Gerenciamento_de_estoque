import { Component, OnInit } from '@angular/core';
import { ConsumidorService } from '../services/consumidor.service';

@Component({
  selector: 'app-consumidor',
  templateUrl: './consumidor.component.html',
  styleUrls: ['./consumidor.component.css']
})
export class ConsumidorComponent implements OnInit {
  consumidores: any[] = [];
  errorMessage: string = '';

  constructor(private consumidorService: ConsumidorService) {}

  ngOnInit(): void {
    this.loadConsumidores();
  }

  loadConsumidores(): void {
    this.consumidorService.getConsumidores().subscribe({
      next: (data) => {
        this.consumidores = data;
      },
      error: (error) => {
        this.errorMessage = 'Erro ao carregar consumidores.';
      }
    });
  }
}

