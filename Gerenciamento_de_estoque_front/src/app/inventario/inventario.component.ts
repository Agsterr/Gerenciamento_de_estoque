import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { InventarioService, ContagemInventario } from '../services/inventario.service';
import { DepositoService, Deposito } from '../services/deposito.service';
import { PageHintComponent } from '../shared/page-hint/page-hint.component';
import { PAGE_HINTS } from '../shared/help/help-content.data';

@Component({
  selector: 'app-inventario',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHintComponent],
  templateUrl: './inventario.component.html',
  styleUrls: ['./inventario.component.scss'],
})
export class InventarioComponent implements OnInit {
  pageHint = PAGE_HINTS['inventario'];
  contagens: ContagemInventario[] = [];
  depositos: Deposito[] = [];
  contagemAtual: ContagemInventario | null = null;
  showList = true;
  showNova = false;
  showContagem = false;
  mensagem = '';
  mensagemErro = '';
  loading = false;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  depositoId: number | null = null;
  observacao = '';
  contagensInput: Record<number, number> = {};

  constructor(
    private inventarioService: InventarioService,
    private depositoService: DepositoService
  ) {}

  ngOnInit(): void {
    this.fetchContagens();
    this.depositoService.listar().subscribe({
      next: (d) => (this.depositos = d),
      error: () => this.onError('Erro ao carregar depósitos.'),
    });
  }

  fetchContagens(page = 0): void {
    this.loading = true;
    this.inventarioService.listar(page, this.pageSize).subscribe({
      next: (resp) => {
        this.currentPage = page;
        this.totalElements = resp.totalElements;
        this.totalPages = Math.ceil(resp.totalElements / this.pageSize) || 1;
        this.contagens = resp.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao buscar contagens!');
      },
    });
  }

  toggleList(): void {
    this.showList = true;
    this.showNova = false;
    this.showContagem = false;
    this.contagemAtual = null;
    this.depositoId = null;
    this.observacao = '';
    this.mensagem = '';
    this.mensagemErro = '';
    this.fetchContagens(this.currentPage);
  }

  toggleNova(): void {
    this.showNova = true;
    this.showList = false;
    this.showContagem = false;
    this.depositoId = null;
    this.observacao = '';
    this.mensagem = '';
    this.mensagemErro = '';
  }

  iniciarContagem(): void {
    if (!this.depositoId) {
      this.onError('Selecione um depósito!');
      return;
    }
    this.loading = true;
    this.inventarioService.iniciar(this.depositoId, this.observacao || undefined).subscribe({
      next: (c) => {
        this.loading = false;
        this.abrirContagem(c);
        this.mensagem = 'Contagem iniciada!';
        setTimeout(() => (this.mensagem = ''), 3000);
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || 'Erro ao iniciar contagem!');
      },
    });
  }

  abrirContagem(c: ContagemInventario): void {
    this.loading = true;
    this.inventarioService.buscar(c.id).subscribe({
      next: (full) => {
        this.contagemAtual = full;
        this.contagensInput = {};
        full.itens.forEach((item) => {
          this.contagensInput[item.id] = item.quantidadeContada ?? item.quantidadeSistema;
        });
        this.showList = false;
        this.showNova = false;
        this.showContagem = true;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar contagem!');
      },
    });
  }

  isAberta(c: ContagemInventario): boolean {
    const s = (c.status || '').toUpperCase();
    return s !== 'FINALIZADO' && s !== 'FINALIZADA' && s !== 'CANCELADO' && s !== 'CANCELADA';
  }

  statusClass(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'FINALIZADO' || s === 'FINALIZADA') return 'finalizado';
    if (s === 'CANCELADO' || s === 'CANCELADA') return 'cancelado';
    return 'aberto';
  }

  salvarItens(): void {
    if (!this.contagemAtual) return;
    const updates = this.contagemAtual.itens.map((item) => ({
      itemId: item.id,
      quantidadeContada: this.contagensInput[item.id] ?? 0,
    }));
    this.loading = true;
    this.inventarioService.registrarItens(this.contagemAtual.id, updates).subscribe({
      next: (c) => {
        this.contagemAtual = c;
        this.loading = false;
        this.mensagem = 'Quantidades registradas!';
        setTimeout(() => (this.mensagem = ''), 3000);
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao registrar quantidades!');
      },
    });
  }

  finalizar(): void {
    if (!this.contagemAtual) return;
    if (!confirm('Finalizar contagem? O estoque será ajustado.')) return;
    this.loading = true;
    this.inventarioService.finalizar(this.contagemAtual.id).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Contagem finalizada!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao finalizar contagem!');
      },
    });
  }

  cancelarContagem(id?: number): void {
    const contagemId = id ?? this.contagemAtual?.id;
    if (!contagemId) return;
    if (!confirm('Cancelar esta contagem? Nenhum ajuste de estoque será aplicado.')) return;
    this.loading = true;
    this.inventarioService.cancelar(contagemId).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Contagem cancelada.');
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || 'Erro ao cancelar contagem!');
      },
    });
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.fetchContagens(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.fetchContagens(this.currentPage + 1);
  }

  irParaPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.fetchContagens(p);
  }

  onPageSizeChange(): void {
    this.currentPage = 0;
    this.fetchContagens(0);
  }

  private onSuccess(msg: string): void {
    this.mensagem = msg;
    setTimeout(() => (this.mensagem = ''), 3000);
    this.toggleList();
  }

  private onError(msg: string): void {
    this.mensagemErro = msg;
    setTimeout(() => (this.mensagemErro = ''), 3000);
  }
}
