import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PedidoCompraService, PedidoCompra, PedidoCompraItem } from '../services/pedido-compra.service';
import { FornecedorService, Fornecedor } from '../services/fornecedor.service';
import { DepositoService, Deposito } from '../services/deposito.service';
import { ProdutoService } from '../services/produto.service';
import { Produto } from '../models/produto.model';
import { PageHintComponent } from '../shared/page-hint/page-hint.component';
import { PAGE_HINTS } from '../shared/help/help-content.data';

interface ItemForm {
  produtoId: number | null;
  quantidade: number;
  precoUnitario: number;
}

@Component({
  selector: 'app-pedido-compra',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHintComponent],
  templateUrl: './pedido-compra.component.html',
  styleUrls: ['./pedido-compra.component.scss'],
})
export class PedidoCompraComponent implements OnInit {
  pageHint = PAGE_HINTS['pedidos-compra'];
  pedidos: PedidoCompra[] = [];
  fornecedores: Fornecedor[] = [];
  depositos: Deposito[] = [];
  produtos: Produto[] = [];
  showList = true;
  showAddForm = false;
  mensagem = '';
  mensagemErro = '';
  loading = false;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  fornecedorId: number | null = null;
  depositoId: number | null = null;
  observacao = '';
  itens: ItemForm[] = [{ produtoId: null, quantidade: 1, precoUnitario: 0 }];

  constructor(
    private pedidoService: PedidoCompraService,
    private fornecedorService: FornecedorService,
    private depositoService: DepositoService,
    private produtoService: ProdutoService
  ) {}

  ngOnInit(): void {
    this.fetchPedidos();
    this.loadReferencias();
  }

  loadReferencias(): void {
    this.fornecedorService.listar(0, 500).subscribe({
      next: (r) => (this.fornecedores = r.content),
      error: () => this.onError('Erro ao carregar fornecedores.'),
    });
    this.depositoService.listar().subscribe({
      next: (d) => (this.depositos = d),
      error: () => this.onError('Erro ao carregar depósitos.'),
    });
    this.produtoService.listarProdutos(0, 500).subscribe({
      next: (r) => (this.produtos = r.content),
      error: () => this.onError('Erro ao carregar produtos.'),
    });
  }

  fetchPedidos(page = 0): void {
    this.loading = true;
    this.pedidoService.listar(page, this.pageSize).subscribe({
      next: (resp) => {
        this.currentPage = page;
        this.totalElements = resp.totalElements;
        this.totalPages = Math.ceil(resp.totalElements / this.pageSize) || 1;
        this.pedidos = resp.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao buscar pedidos de compra!');
      },
    });
  }

  toggleList(): void {
    this.showList = true;
    this.showAddForm = false;
    this.resetForm();
    this.mensagem = '';
    this.mensagemErro = '';
    this.fetchPedidos(this.currentPage);
  }

  toggleAddForm(): void {
    this.showAddForm = true;
    this.showList = false;
    this.resetForm();
    this.mensagem = '';
    this.mensagemErro = '';
  }

  resetForm(): void {
    this.fornecedorId = null;
    this.depositoId = null;
    this.observacao = '';
    this.itens = [{ produtoId: null, quantidade: 1, precoUnitario: 0 }];
  }

  addItem(): void {
    this.itens.push({ produtoId: null, quantidade: 1, precoUnitario: 0 });
  }

  removeItem(index: number): void {
    if (this.itens.length > 1) this.itens.splice(index, 1);
  }

  podeAcao(p: PedidoCompra): boolean {
    const s = (p.status || '').toUpperCase();
    return s !== 'RECEBIDO' && s !== 'CANCELADO';
  }

  statusClass(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'RECEBIDO') return 'recebido';
    if (s === 'CANCELADO') return 'cancelado';
    return 'pendente';
  }

  submitForm(): void {
    if (!this.fornecedorId) {
      this.onError('Selecione um fornecedor!');
      return;
    }
    const itensValidos = this.itens.filter((i) => i.produtoId && i.quantidade > 0);
    if (itensValidos.length === 0) {
      this.onError('Adicione pelo menos um item válido!');
      return;
    }
    const payload = {
      fornecedorId: this.fornecedorId,
      depositoId: this.depositoId || undefined,
      observacao: this.observacao || undefined,
      itens: itensValidos.map((i) => ({
        produtoId: i.produtoId!,
        quantidade: i.quantidade,
        precoUnitario: i.precoUnitario,
      })) as PedidoCompraItem[],
    };
    this.loading = true;
    this.pedidoService.criar(payload).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Pedido de compra criado!');
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || 'Erro ao criar pedido!');
      },
    });
  }

  receber(id: number): void {
    if (!confirm('Confirmar recebimento do pedido?')) return;
    this.loading = true;
    this.pedidoService.receber(id).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Pedido recebido com sucesso!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao receber pedido!');
      },
    });
  }

  cancelar(id: number): void {
    if (!confirm('Confirmar cancelamento do pedido?')) return;
    this.loading = true;
    this.pedidoService.cancelar(id).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Pedido cancelado!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao cancelar pedido!');
      },
    });
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.fetchPedidos(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.fetchPedidos(this.currentPage + 1);
  }

  irParaPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.fetchPedidos(p);
  }

  onPageSizeChange(): void {
    this.currentPage = 0;
    this.fetchPedidos(0);
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
