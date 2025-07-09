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
    entregas: EntregaResponse[]; // Tipo que pode ser um array de entregas
  }>();

  // Filtros de busca
  dataInicio: string = '';
  dataFim: string = '';
  mes: number | null = null;
  ano: number | null = null;
  produtoId: number | null = null;
  consumidorId: number | null = null;

  // Lista de entregas filtradas
  entregas: EntregaResponse[] = [];
  
  // Flag para verificar se a busca foi realizada
  isSearched: boolean = false;

  constructor(private entregasService: EntregasService) {}

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
        next: (response: any) => {
          if (Array.isArray(response.content)) {
            this.entregas = response.content;  // Atualiza as entregas com o conteúdo da resposta
            this.isSearched = true;  // Marca como busca realizada
            this.emitirResultados('porPeriodo');
          } else {
            this.entregas = [];  // Se não for um array válido, limpa as entregas
            this.isSearched = true;  // Marca como busca realizada
            this.emitirResultados('porPeriodo');
          }
        },
        error: (error) => {
          console.error('Erro ao buscar entregas por período', error);
          this.entregas = [];  // Limpa as entregas em caso de erro
          this.isSearched = true;  // Marca como busca realizada
          this.emitirResultados('porPeriodo');
        }
      });
    }
  }

  // Método para buscar entregas por mês e ano
  onBuscarPorMesAno() {
    if (this.mes && this.ano) {
      this.entregasService.porMes(this.mes, this.ano).subscribe({
        next: (response: any) => {
          if (Array.isArray(response.content)) {
            this.entregas = response.content;  // Atualiza as entregas com o conteúdo da resposta
            this.isSearched = true;  // Marca como busca realizada
            this.emitirResultados('porMesAno');
          } else {
            this.entregas = [];  // Se não for um array válido, limpa as entregas
            this.isSearched = true;  // Marca como busca realizada
            this.emitirResultados('porMesAno');
          }
        },
        error: (error) => {
          console.error('Erro ao buscar entregas por mês e ano', error);
          this.entregas = [];  // Limpa as entregas em caso de erro
          this.isSearched = true;  // Marca como busca realizada
          this.emitirResultados('porMesAno');
        }
      });
    }
  }

  // Método para buscar entregas por produto
  onBuscarPorProduto() {
    if (this.produtoId) {
      this.entregasService.porProduto(this.produtoId, 1, 0, 20).subscribe({
        next: (data: any) => {
          this.entregas = data.content; // Atualiza as entregas com a resposta paginada
          this.isSearched = true;  // Marca como busca realizada
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
        next: (response: any) => {
          if (Array.isArray(response.content)) {
            this.entregas = response.content;  // Atualiza as entregas com a resposta
            this.isSearched = true;  // Marca como busca realizada
            this.emitirResultados('porConsumidor');
          } else {
            this.entregas = [];  // Limpa as entregas se a resposta não for válida
            this.isSearched = true;  // Marca como busca realizada
            this.emitirResultados('porConsumidor');
          }
        },
        error: (error) => {
          console.error('Erro ao buscar entregas por consumidor', error);
          this.entregas = [];  // Limpa as entregas em caso de erro
          this.isSearched = true;  // Marca como busca realizada
          this.emitirResultados('porConsumidor');
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

  // Método para limpar os filtros
  limparFiltros() {
    // Limpa todos os filtros
    this.dataInicio = '';
    this.dataFim = '';
    this.mes = null;
    this.ano = null;
    this.produtoId = null;
    this.consumidorId = null;
    this.entregas = [];
    this.isSearched = false;  // Marca como não buscado
    this.emitirResultados('limpar');
  }

  // Método para submeter o formulário
  onSubmit() {
    if (this.dataInicio && this.dataFim) {
      this.onBuscarPorPeriodo();
    }
  }

  // Método para submeter o formulário de mês/ano
  onSubmitMesAno() {
    if (this.mes && this.ano) {
      this.onBuscarPorMesAno();  // Método que busca as entregas por mês e ano
    }
  }
}
