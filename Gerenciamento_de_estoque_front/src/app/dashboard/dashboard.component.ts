import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { ConsumidorService } from '../services/consumidor.service';
import { Router, RouterModule } from '@angular/router';
import { Produto } from '../models/produto.model';
import { Consumer } from '../models/consumer.model';

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

  constructor(
    private produtoService: ProdutoService,
    private consumidorService: ConsumidorService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.carregarProdutos();
    this.carregarConsumidores();
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

  verDetalhesProduto(produtoId: number): void {
    this.router.navigate([`/dashboard/produtos/${produtoId}`]);
  }

  verDetalhesConsumidor(consumidorId: number): void {
    this.router.navigate([`/dashboard/consumidores/${consumidorId}`]);
  }
}
