import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { Router, RouterModule } from '@angular/router';
import { Produto } from '../models/produto.model';

// ✅ Importação do Angular Material para ícones
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [
    RouterModule,
    MatIconModule // ⬅ necessário para que <mat-icon> funcione
  ],
})
export class DashboardComponent implements OnInit {
  produtos: Produto[] = [];

  constructor(
    private produtoService: ProdutoService,
    private router: Router
  ) {}

  ngOnInit(): void {
    
    
  }

  verDetalhesProduto(produtoId: number): void {
    this.router.navigate([`/dashboard/produtos/${produtoId}`]);
  }
}
