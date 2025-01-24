
import { Component, OnInit } from '@angular/core';
import { EntregasService } from '../services/entregas.service';

@Component({
  selector: 'app-entregas',
  templateUrl: './entregas.component.html',
  styleUrls: ['./entregas.component.css']
})
export class EntregasComponent implements OnInit {
  entregas: any[] = [];
  errorMessage: string = '';

  constructor(private entregasService: EntregasService) {}

  ngOnInit(): void {
    this.loadEntregas();
  }

  loadEntregas(): void {
    this.entregasService.getEntregas().subscribe({
      next: (data) => {
        this.entregas = data;
      },
      error: (error) => {
        this.errorMessage = 'Erro ao carregar entregas.';
      }
    });
  }
}
