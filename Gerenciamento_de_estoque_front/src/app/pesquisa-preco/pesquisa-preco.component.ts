import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PesquisaPrecoService } from '../services/pesquisa-preco.service';
import { PesquisaPrecoResposta } from '../services/admin.service';

@Component({
  selector: 'app-pesquisa-preco',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pesquisa-preco.component.html',
  styleUrls: ['./pesquisa-preco.component.scss'],
})
export class PesquisaPrecoComponent implements OnInit {
  valorMin = 30;
  valorMax = 80;
  comentario = '';
  minhaResposta: PesquisaPrecoResposta | null = null;
  loading = false;
  mensagem = '';
  mensagemErro = '';

  constructor(private pesquisaPrecoService: PesquisaPrecoService) {}

  ngOnInit(): void {
    this.carregarMinhaResposta();
  }

  carregarMinhaResposta(): void {
    this.pesquisaPrecoService.minhaResposta().subscribe({
      next: (r) => {
        this.minhaResposta = r;
        if (r) {
          this.valorMin = Number(r.valorMin);
          this.valorMax = Number(r.valorMax);
          this.comentario = r.comentario ?? '';
        }
      },
      error: () => {},
    });
  }

  enviar(): void {
    if (this.valorMax < this.valorMin) {
      this.mensagemErro = 'O valor máximo deve ser maior ou igual ao mínimo.';
      return;
    }
    this.loading = true;
    this.mensagemErro = '';
    this.pesquisaPrecoService.enviar(this.valorMin, this.valorMax, this.comentario || undefined).subscribe({
      next: (r) => {
        this.loading = false;
        this.minhaResposta = r;
        this.mensagem = 'Obrigado! Sua resposta foi registrada.';
        setTimeout(() => (this.mensagem = ''), 4000);
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Não foi possível enviar. Tente novamente.';
      },
    });
  }
}
