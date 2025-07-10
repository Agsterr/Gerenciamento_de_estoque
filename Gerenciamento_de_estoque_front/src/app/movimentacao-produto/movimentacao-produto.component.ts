import { HttpClientModule } from '@angular/common/http'; 
import { CommonModule } from '@angular/common'; 
import { FormsModule } from '@angular/forms'; 
import { Component, OnInit } from '@angular/core';
import { MovimentacaoProdutoService } from '../services/movimentacao-produto.service';
import { MovimentacaoProdutoDto, TipoMovimentacao } from '../models/movimentacao-produto.model';

@Component({
  selector: 'app-movimentacao-produto',
  templateUrl: './movimentacao-produto.component.html',
  styleUrls: ['./movimentacao-produto.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
})
export class MovimentacaoProdutoComponent implements OnInit {
  movimentacoes: MovimentacaoProdutoDto[] = [];
  tipoMovimentacao: TipoMovimentacao = TipoMovimentacao.ENTRADA;
  data: string = '';
  inicio: string = '';
  fim: string = '';
  ano: number = new Date().getFullYear();
  mes: number = new Date().getMonth() + 1;

  constructor(private movimentacaoService: MovimentacaoProdutoService) {}

  ngOnInit(): void {
    this.buscarMovimentacoes(); 
  }

  // Método para buscar movimentações com base nos filtros
  buscarMovimentacoes(): void {
    if (this.data) {
      this.movimentacaoService.buscarPorData(this.tipoMovimentacao, this.data).subscribe(data => {
        console.log('Movimentações por data:', data);
        this.movimentacoes = data;
      });
    } else if (this.inicio && this.fim) {
      this.movimentacaoService.buscarPorPeriodo(this.tipoMovimentacao, this.inicio, this.fim).subscribe(data => {
        console.log('Movimentações por período:', data);
        this.movimentacoes = data;
      });
    } else if (this.ano && this.mes) {
      this.movimentacaoService.buscarPorMes(this.ano, this.mes).subscribe(data => {
        console.log('Movimentações por mês:', data);
        this.movimentacoes = data;
      });
    } else if (this.ano) {
      this.movimentacaoService.buscarPorAno(this.ano).subscribe(data => {
        console.log('Movimentações por ano:', data);
        this.movimentacoes = data;
      });
    }
  }

  // Método para registrar uma nova movimentação
  registrarMovimentacao(): void {
    const novoMovimento: MovimentacaoProdutoDto = {
      id: 0, 
      produtoId: 1, 
      tipo: this.tipoMovimentacao,
      quantidade: 10, 
      dataHora: new Date().toISOString(),
      orgId: 1, 
      nomeProduto: 'Produto Exemplo' 
    };

    this.movimentacaoService.registrarMovimentacao(novoMovimento).subscribe(response => {
      console.log('Movimentação registrada com sucesso!', response);
      this.buscarMovimentacoes(); 
    });
  }
}
