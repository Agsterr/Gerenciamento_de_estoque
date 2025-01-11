
import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';

@Component({
  selector: 'app-produto',
  templateUrl: './produto.component.html',
  styleUrls: ['./produto.component.css']
})
export class ProdutoComponent implements OnInit {
  produtos: any[] = [];
  errorMessage: string = '';

  constructor(private produtoService: ProdutoService) {}

  ngOnInit(): void {
    this.loadProdutos();
  }

  loadProdutos(): void {
    this.produtoService.getProdutos().subscribe({
      next: (data) => {
        this.produtos = data;
      },
      error: (error) => {
        this.errorMessage = 'Erro ao carregar produtos.';
      }
    });
  }
}
