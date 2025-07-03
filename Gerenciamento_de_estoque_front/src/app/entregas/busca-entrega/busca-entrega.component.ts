import { Component, EventEmitter, Output } from '@angular/core';
import { CommonModule } from '@angular/common';  // Importando CommonModule
import { FormsModule } from '@angular/forms';  // Importando FormsModule
import { EntregasService } from '../../services/entregas.service'; // Importando o serviço
import { EntregaResponse } from '../../models/src/app/models/entrega/entrega-response.model'; // Modelos de resposta

@Component({
  selector: 'app-busca-entrega',
  standalone: true, // Tornando o componente standalone
  imports: [CommonModule, FormsModule], // Importando os módulos necessários
  templateUrl: './busca-entrega.component.html',
  styleUrls: ['./busca-entrega.component.scss']
})
export class BuscaEntregaComponent {
  @Output() buscar = new EventEmitter<{
    filtro: string;
    entregas: EntregaResponse[] | any; // Tipo que pode ser um array ou um objeto com páginas
  }>();

  dataInicio: string = '';
  dataFim: string = '';
  mes: number | null = null;
  ano: number | null = null;
  produtoId: number | null = null;
  consumidorId: number | null = null;
  entregas: EntregaResponse[] = [];

  constructor(private entregasService: EntregasService) {} // Injeção do serviço

  // Método para formatar as datas no formato ISO
  formatarDataISO(data: string): string {
    const date = new Date(data);
    return date.toISOString();  // Retorna a data no formato ISO
  }

  // Método para buscar entregas por período (data)
  onBuscarPorPeriodo() {
    if (this.dataInicio && this.dataFim) {
      const inicioISO = this.formatarDataISO(this.dataInicio);
      const fimISO = this.formatarDataISO(this.dataFim);

      this.entregasService.porPeriodo(inicioISO, fimISO).subscribe({
        next: (entregas: EntregaResponse[]) => {
          this.entregas = entregas;
          this.emitirResultados('porPeriodo');
        },
        error: (error) => {
          console.error('Erro ao buscar entregas por período', error);
        }
      });
    }
  }

  // Método para buscar entregas por mês e ano
  onBuscarPorMesAno() {
    if (this.mes && this.ano) {
      this.entregasService.porMes(this.mes, this.ano).subscribe({
        next: (entregas: EntregaResponse[]) => {
          this.entregas = entregas;
          this.emitirResultados('porMesAno');
        },
        error: (error) => {
          console.error('Erro ao buscar entregas por mês e ano', error);
        }
      });
    }
  }

  // Método para buscar entregas por produto
  onBuscarPorProduto() {
    if (this.produtoId) {
      this.entregasService.porProduto(this.produtoId, 1, 0, 20).subscribe({
        next: (data: any) => {
          this.entregas = data.content; // Aqui usamos content pois é uma resposta paginada
          this.emitirResultados('porProduto');
        },
        error: (error) => {
          console.error('Erro ao buscar entregas por produto', error);
        }
      });
    }
  }

  // Método para buscar entregas por consumidor
  onBuscarPorConsumidor() {
    if (this.consumidorId) {
      this.entregasService.porConsumidor(this.consumidorId).subscribe({
        next: (entregas: EntregaResponse[]) => {
          this.entregas = entregas;
          this.emitirResultados('porConsumidor');
        },
        error: (error) => {
          console.error('Erro ao buscar entregas por consumidor', error);
        }
      });
    }
  }

  // Emitir os resultados para o componente pai
  emitirResultados(filtro: string): void {
    this.buscar.emit({
      filtro: filtro,
      entregas: this.entregas
    });
  }

  // Método que será chamado no submit do formulário de datas
  onSubmit() {
    if (this.dataInicio && this.dataFim) {
      this.onBuscarPorPeriodo();
    }
  }

  // Método que será chamado no submit do formulário de mês/ano
  onSubmitMesAno() {
    if (this.mes && this.ano) {
      this.onBuscarPorMesAno();
    }
  }

  // Método para limpar os filtros
  limparFiltros() {
    this.dataInicio = '';
    this.dataFim = '';
    this.mes = null;
    this.ano = null;
    this.produtoId = null;
    this.consumidorId = null;
    this.entregas = [];
    this.buscar.emit({ filtro: 'limpar', entregas: [] }); // Emite evento com os filtros limpos
  }
}
