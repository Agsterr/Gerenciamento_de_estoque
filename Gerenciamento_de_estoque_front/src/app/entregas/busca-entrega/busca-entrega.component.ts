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
      next: (response: any) => {
        // Verificar se a resposta contém a chave 'content' e se ela é um array válido
        if (Array.isArray(response.content)) {
          this.entregas = response.content;  // Atualiza as entregas com o conteúdo da resposta
          this.emitirResultados('porPeriodo');
        } else {
          console.error('Resposta de entregas inválida:', response);
          this.entregas = [];  // Define a lista de entregas como vazia
          this.emitirResultados('porPeriodo');
        }
      },
      error: (error) => {
        console.error('Erro ao buscar entregas por período', error);
        this.entregas = [];  // Limpa as entregas em caso de erro
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
        // Verifica se a resposta contém a chave 'content' com o array de entregas
        if (Array.isArray(response.content)) {
          this.entregas = response.content;  // Atualiza as entregas com o conteúdo da resposta
          this.emitirResultados('porMesAno'); // Emite os resultados para o componente pai
        } else {
          console.error('Resposta de entregas inválida:', response);
          this.entregas = [];  // Se a resposta não for válida, define um array vazio
          this.emitirResultados('porMesAno');
        }
      },
      error: (error) => {
        console.error('Erro ao buscar entregas por mês e ano', error);
        this.entregas = [];  // Limpa as entregas em caso de erro
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
          this.entregas = data.content; // Aqui usamos 'content' pois é uma resposta paginada
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
    console.log('Buscando entregas para o consumidor com ID:', this.consumidorId);  // Verifique se o consumidorId está correto

    this.entregasService.porConsumidor(this.consumidorId).subscribe({
      next: (response: any) => {
        // Verifica se a resposta contém a chave 'content' com a lista de consumidores
        if (Array.isArray(response.content)) {
          this.entregas = response.content;  // Atualiza a lista de entregas com o conteúdo da resposta
          this.emitirResultados('porConsumidor');  // Emite os resultados para o componente pai
        } else {
          console.error('Resposta de entregas inválida:', response);
          this.entregas = [];  // Se a resposta não for válida, define um array vazio
          this.emitirResultados('porConsumidor');
        }
      },
      error: (error) => {
        console.error('Erro ao buscar entregas por consumidor', error);
        this.entregas = [];  // Limpa as entregas em caso de erro
        this.emitirResultados('porConsumidor');
      }
    });
  } else {
    console.log('Consumidor ID não definido');
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
    // Limpa todos os filtros
    this.dataInicio = '';
    this.dataFim = '';
    this.mes = null;
    this.ano = null;
    this.produtoId = null;
    this.consumidorId = null;
    
    // Limpa a lista de entregas
    this.entregas = [];
    
    // Emite evento com a lista vazia
    this.buscar.emit({ filtro: 'limpar', entregas: [] });
  }

  // Método para aplicar a busca de entregas e atualizar o estado
  onBuscarEntregas(event: { filtro: string; entregas: EntregaResponse[] }): void {
    console.log('Resultados do filtro:', event.entregas);
    this.entregas = event.entregas;  // Atualiza a lista de entregas com os dados emitidos
    this.emitirResultados(event.filtro); // Emite novamente, se necessário
  }
}
