

import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';  // Importação do ProdutoService
import { Router,RouterModule } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  standalone: true,  // Certifique-se de que o componente é standalone
  imports: [RouterModule],  // Importe RouterModule aqui
})
export class DashboardComponent implements OnInit {
  produtos: any[] = [];  // Inicializando com um array vazio

  constructor(private produtoService: ProdutoService, private router: Router) {}

  ngOnInit(): void {
    // Corrigindo a tipagem do parâmetro 'data'
    this.produtoService.getProdutos().subscribe((data: any[]) => {
      this.produtos = data;  // Atribuindo a resposta à variável 'produtos'
    });
  }

  // Método para visualizar os detalhes do produto
  verDetalhesProduto(produtoId: number): void {
    this.router.navigate([`/dashboard/estoque/${produtoId}`]);
  }
}
