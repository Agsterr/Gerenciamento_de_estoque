import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProdutoService } from '../services/produto.service';

@Component({
  selector: 'app-produto',
  templateUrl: './produto.component.html',
  styleUrls: ['./produto.component.scss']
})
export class ProdutoComponent implements OnInit {
  produto: any;

  constructor(private produtoService: ProdutoService, private route: ActivatedRoute) {}

  ngOnInit(): void {
    const produtoId = this.route.snapshot.paramMap.get('id');
    if (produtoId) {
      this.produtoService.getProdutoDetalhado(+produtoId).subscribe(data => {
        this.produto = data;
      });
    }
  }
}
