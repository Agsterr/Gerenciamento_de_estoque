import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DepositoService, Deposito, EstoqueDeposito } from '../services/deposito.service';
import { ProdutoService } from '../services/produto.service';
import { Produto } from '../models/produto.model';
import { PageHintComponent } from '../shared/page-hint/page-hint.component';
import { PAGE_HINTS } from '../shared/help/help-content.data';

@Component({
  selector: 'app-deposito',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHintComponent],
  templateUrl: './deposito.component.html',
  styleUrls: ['./deposito.component.scss'],
})
export class DepositoComponent implements OnInit {
  pageHint = PAGE_HINTS['depositos'];
  depositos: Deposito[] = [];
  filteredDepositos: Deposito[] = [];
  estoque: EstoqueDeposito[] = [];
  depositoEstoqueId: number | null = null;
  depositoEstoqueNome = '';
  searchTerm = '';
  showList = true;
  showAddForm = false;
  showEstoque = false;
  editing = false;
  mensagem = '';
  mensagemErro = '';
  loading = false;

  form: Partial<Deposito> = this.emptyForm();
  showTransfer = false;
  transferOrigemId: number | null = null;
  transferDestinoId: number | null = null;
  transferProdutoId: number | null = null;
  transferQuantidade = 1;
  produtos: Produto[] = [];

  constructor(private depositoService: DepositoService, private produtoService: ProdutoService) {}

  ngOnInit(): void {
    this.fetchDepositos();
    this.produtoService.listarProdutos(0, 500).subscribe({
      next: (data) => (this.produtos = data.content || []),
      error: () => {},
    });
  }

  emptyForm(): Partial<Deposito> {
    return { nome: '', endereco: '', padrao: false };
  }

  fetchDepositos(): void {
    this.loading = true;
    this.depositoService.listar().subscribe({
      next: (data) => {
        this.depositos = data.sort((a, b) => a.nome.localeCompare(b.nome));
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao buscar depósitos!');
      },
    });
  }

  toggleList(): void {
    this.showList = true;
    this.showAddForm = false;
    this.showEstoque = false;
    this.editing = false;
    this.form = this.emptyForm();
    this.mensagem = '';
    this.mensagemErro = '';
    this.fetchDepositos();
  }

  toggleAddForm(): void {
    this.showAddForm = true;
    this.showList = false;
    this.showEstoque = false;
    this.editing = false;
    this.form = this.emptyForm();
    this.mensagem = '';
    this.mensagemErro = '';
  }

  submitForm(): void {
    if (!this.form.nome?.trim()) {
      this.onError('Nome é obrigatório!');
      return;
    }
    this.editing ? this.update() : this.create();
  }

  create(): void {
    this.loading = true;
    this.depositoService.criar(this.form).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Depósito adicionado!');
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || 'Erro ao adicionar depósito!');
      },
    });
  }

  update(): void {
    if (!this.form.id) {
      this.onError('ID obrigatório para edição');
      return;
    }
    this.loading = true;
    this.depositoService.atualizar(this.form.id, this.form).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Depósito atualizado!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao editar depósito!');
      },
    });
  }

  delete(id: number): void {
    if (!confirm('Confirma exclusão do depósito?')) return;
    this.loading = true;
    this.depositoService.excluir(id).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Depósito excluído!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao excluir depósito!');
      },
    });
  }

  edit(d: Deposito): void {
    this.editing = true;
    this.showAddForm = true;
    this.showList = false;
    this.showEstoque = false;
    this.form = { ...d };
    this.mensagem = '';
    this.mensagemErro = '';
  }

  verEstoque(d: Deposito): void {
    this.showList = false;
    this.showAddForm = false;
    this.showEstoque = true;
    this.showTransfer = false;
    this.depositoEstoqueId = d.id;
    this.depositoEstoqueNome = d.nome;
    this.transferOrigemId = d.id;
    this.loading = true;
    this.depositoService.estoque(d.id).subscribe({
      next: (data) => {
        this.estoque = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar estoque do depósito!');
      },
    });
  }

  toggleTransfer(): void {
    this.showTransfer = !this.showTransfer;
    if (this.showTransfer && this.depositoEstoqueId) {
      this.transferOrigemId = this.depositoEstoqueId;
    }
  }

  executarTransferencia(): void {
    if (!this.transferOrigemId || !this.transferDestinoId || !this.transferProdutoId || this.transferQuantidade < 1) {
      this.onError('Preencha origem, destino, produto e quantidade.');
      return;
    }
    this.loading = true;
    this.depositoService.transferir({
      depositoOrigemId: this.transferOrigemId,
      depositoDestinoId: this.transferDestinoId,
      produtoId: this.transferProdutoId,
      quantidade: this.transferQuantidade,
    }).subscribe({
      next: () => {
        this.loading = false;
        this.mensagem = 'Transferência realizada!';
        setTimeout(() => (this.mensagem = ''), 3000);
        if (this.depositoEstoqueId) {
          this.depositoService.estoque(this.depositoEstoqueId).subscribe({
            next: (data) => (this.estoque = data),
          });
        }
        this.showTransfer = false;
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || err.error?.message || 'Erro na transferência.');
      },
    });
  }

  applyFilter(): void {
    const t = this.searchTerm.trim().toLowerCase();
    this.filteredDepositos = t
      ? this.depositos.filter(
          (d) =>
            d.nome.toLowerCase().includes(t) ||
            (d.endereco && d.endereco.toLowerCase().includes(t))
        )
      : [...this.depositos];
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
