import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { Sugestao, SugestaoService } from '../services/login-logs.service';

@Component({
  selector: 'app-sugestao',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sugestao.component.html',
  styleUrls: ['./sugestao.component.scss'],
})
export class SugestaoComponent implements OnInit {
  texto = '';
  loading = false;
  mensagem = '';
  mensagemErro = '';
  isAdmin = false;
  sugestoes: Sugestao[] = [];
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;

  constructor(
    private sugestaoService: SugestaoService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    if (this.isAdmin) {
      this.carregarSugestoes();
    }
  }

  enviar(): void {
    const t = this.texto.trim();
    if (t.length < 5) {
      this.mensagemErro = 'Escreva pelo menos 5 caracteres.';
      setTimeout(() => (this.mensagemErro = ''), 3000);
      return;
    }
    this.loading = true;
    this.sugestaoService.enviar(t).subscribe({
      next: () => {
        this.loading = false;
        this.texto = '';
        this.mensagem = 'Sugestão enviada. Obrigado!';
        setTimeout(() => (this.mensagem = ''), 3000);
        if (this.isAdmin) this.carregarSugestoes();
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao enviar sugestão.';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  carregarSugestoes(page = 0): void {
    this.loading = true;
    this.sugestaoService.listarOrg(page, this.pageSize).subscribe({
      next: (data) => {
        this.sugestoes = data.content ?? [];
        this.totalPages = data.totalPages ?? 0;
        this.currentPage = page;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      },
    });
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.carregarSugestoes(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.carregarSugestoes(this.currentPage + 1);
  }
}
