import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { ConsumidorService } from '../services/consumidor.service';
import { Router, RouterModule } from '@angular/router';
import { Produto } from '../models/produto.model';
import { Consumer } from '../models/consumer.model';
import { EntregasService } from '../services/entregas.service';
import { CategoriaService } from '../services/categoria.service';
import { MovimentacaoProdutoService } from '../services/movimentacao-produto.service';
import { EntregaResponse } from '../models/src/app/models/entrega/entrega-response.model';
import { Categoria } from '../models/categoria.model';
import { MovimentacaoProduto } from '../models/movimentacao-produto.model';

// ✅ Angular Material para ícones
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [
    RouterModule,
    MatIconModule
  ],
})
export class DashboardComponent implements OnInit {
  produtos: Produto[] = [];
  consumidores: Consumer[] = [];
  entregas: EntregaResponse[] = [];
  categorias: Categoria[] = [];
  movimentacoes: MovimentacaoProduto[] = [];

  constructor(
    private produtoService: ProdutoService,
    private consumidorService: ConsumidorService,
    private entregasService: EntregasService,
    private categoriaService: CategoriaService,
    private movimentacaoProdutoService: MovimentacaoProdutoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.carregarProdutos();
    this.carregarConsumidores();
    this.carregarEntregas();
    this.carregarCategorias();
    this.carregarMovimentacoes();
  }

 carregarProdutos(): void {
  this.produtoService.listarProdutos().subscribe({
    next: (data) => {
      this.produtos = data.content; // ✅ Corrigido
    },
    error: (err) => {
      console.error('Erro ao carregar produtos:', err);
    },
  });
}


  carregarConsumidores(): void {
    this.consumidorService.listarConsumidores().subscribe({
      next: (data: Consumer[]) => {
        this.consumidores = data;
      },
      error: (err) => {
        console.error('Erro ao carregar consumidores:', err);
      },
    });
  }

  carregarEntregas(): void {
    // Busca a primeira página de entregas (ajuste se necessário)
    this.entregasService.listarEntregas(0, 10).subscribe({
      next: (data) => {
        this.entregas = data.content || [];
      },
      error: (err) => {
        console.error('Erro ao carregar entregas:', err);
      },
    });
  }

  carregarCategorias(): void {
    this.categoriaService.listarCategorias(0, 10).subscribe({
      next: (data) => {
        this.categorias = data.content || [];
      },
      error: (err) => {
        console.error('Erro ao carregar categorias:', err);
      },
    });
  }

  carregarMovimentacoes(): void {
    // Busca movimentações da primeira página (ajuste se necessário)
    this.movimentacaoProdutoService.buscarPorAno(new Date().getFullYear(), 0, 10).subscribe({
      next: (data) => {
        this.movimentacoes = data.content || [];
      },
      error: (err) => {
        console.error('Erro ao carregar movimentações:', err);
      },
    });
  }

  verDetalhesProduto(produtoId: number): void {
    this.router.navigate([`/dashboard/produtos/${produtoId}`]);
  }

  verDetalhesConsumidor(consumidorId: number): void {
    this.router.navigate([`/dashboard/consumidores/${consumidorId}`]);
  }
}
