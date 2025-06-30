// src/app/busca-entrega/busca-entrega.component.ts
import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-busca-entrega',
  standalone: true, // Tornando o componente standalone
  imports: [CommonModule, FormsModule], // Importando os módulos necessários
  templateUrl: './busca-entrega.component.html',
  styleUrls: ['./busca-entrega.component.scss']
})
export class BuscaEntregaComponent {
  @Output() buscar = new EventEmitter<{ inicio: string; fim: string }>(); // Emite o evento com as datas de início e fim
  dataInicio: string = '';
  dataFim: string = '';

  // Método que será chamado no submit do formulário
  onSubmit() {
    if (this.dataInicio && this.dataFim) {
      this.buscar.emit({ inicio: this.dataInicio, fim: this.dataFim });
    }
  }
}
