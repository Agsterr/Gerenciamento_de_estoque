

import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service'; // Serviço Produto
import { Router, RouterModule } from '@angular/router';
import { Produto } from '../models/produto.model'; // Modelo Produto

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,
  imports: [RouterModule],
})
export class DashboardComponent implements OnInit {
  produtos: Produto[] = []; // Lista de produtos

  constructor(private produtoService: ProdutoService, private router: Router) {}

  ngOnInit(): void {
    this.produtoService.listarProdutos().subscribe((data: Produto[]) => {
      this.produtos = data; // Atribuindo a resposta à variável 'produtos'
    });
  }

  verDetalhesProduto(produtoId: number): void {
    this.router.navigate([`/dashboard/produtos/${produtoId}`]); // Navegação para detalhes do produto
  }
}
