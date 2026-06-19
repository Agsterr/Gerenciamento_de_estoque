import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  PedidoVendaService, PedidoVenda, PedidoVendaItem,
  FORMAS_PAGAMENTO, CONDICOES_PAGAMENTO, TIPOS_PEDIDO
} from '../services/pedido-venda.service';
import { ConsumidorService } from '../services/consumidor.service';
import { ProdutoService } from '../services/produto.service';
import { UsuarioService } from '../services/usuario.service';
import { Produto } from '../models/produto.model';
import { Consumer } from '../models/consumer.model';
import { Usuario } from '../models/usuario.model';
import { PageHintComponent } from '../shared/page-hint/page-hint.component';
import { PAGE_HINTS } from '../shared/help/help-content.data';

interface ItemForm {
  produtoId: number | null;
  quantidade: number;
  precoUnitario: number;
}

@Component({
  selector: 'app-pedido-venda',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHintComponent],
  templateUrl: './pedido-venda.component.html',
  styleUrls: ['./pedido-venda.component.scss'],
})
export class PedidoVendaComponent implements OnInit {
  pageHint = PAGE_HINTS['pedidos-venda'];
  pedidos: PedidoVenda[] = [];
  clientes: Consumer[] = [];
  produtos: Produto[] = [];
  vendedores: Usuario[] = [];
  tiposPedido = TIPOS_PEDIDO;
  formasPagamento = FORMAS_PAGAMENTO;
  condicoesPagamento = CONDICOES_PAGAMENTO;

  showList = true;
  showAddForm = false;
  mensagem = '';
  mensagemErro = '';
  loading = false;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  tipoPedido: 'VENDA' | 'INTERNO' = 'VENDA';
  consumidorId: number | null = null;
  funcionarioId: number | null = null;
  vendedorId: number | null = null;
  dataHora = '';
  formaPagamento = 'PIX';
  condicaoPagamento = 'A_VISTA';
  observacao = '';
  confirmarAoSalvar = true;
  itens: ItemForm[] = [{ produtoId: null, quantidade: 1, precoUnitario: 0 }];

  get isInterno(): boolean {
    return this.tipoPedido === 'INTERNO';
  }

  constructor(
    private pedidoService: PedidoVendaService,
    private consumidorService: ConsumidorService,
    private produtoService: ProdutoService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.fetchPedidos();
    this.loadReferencias();
    this.setDataHoraNow();
  }

  setDataHoraNow(): void {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    this.dataHora = now.toISOString().slice(0, 16);
  }

  loadReferencias(): void {
    this.consumidorService.listarConsumidores(0, 500).subscribe({
      next: (list) => (this.clientes = list || []),
      error: () => this.onError('Erro ao carregar clientes.'),
    });
    this.produtoService.listarProdutos(0, 500).subscribe({
      next: (r) => (this.produtos = r.content),
      error: () => this.onError('Erro ao carregar produtos.'),
    });
    this.usuarioService.listarAtivos(0, 100).subscribe({
      next: (r) => (this.vendedores = r.content || []),
      error: () => {},
    });
  }

  onTipoChange(): void {
    this.consumidorId = null;
    this.funcionarioId = null;
    if (this.isInterno) {
      this.itens.forEach((i) => (i.precoUnitario = 0));
    }
  }

  onProdutoChange(item: ItemForm): void {
    if (this.isInterno) return;
    const p = this.produtos.find((x) => x.id === item.produtoId);
    if (p && (!item.precoUnitario || item.precoUnitario === 0)) {
      item.precoUnitario = p.preco;
    }
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
        this.onError('Erro ao buscar pedidos!');
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
    this.tipoPedido = 'VENDA';
    this.consumidorId = null;
    this.funcionarioId = null;
    this.vendedorId = null;
    this.formaPagamento = 'PIX';
    this.condicaoPagamento = 'A_VISTA';
    this.observacao = '';
    this.confirmarAoSalvar = true;
    this.itens = [{ produtoId: null, quantidade: 1, precoUnitario: 0 }];
    this.setDataHoraNow();
  }

  addItem(): void {
    this.itens.push({ produtoId: null, quantidade: 1, precoUnitario: 0 });
  }

  removeItem(index: number): void {
    if (this.itens.length > 1) this.itens.splice(index, 1);
  }

  podeAcao(p: PedidoVenda): boolean {
    const s = (p.status || '').toUpperCase();
    return s !== 'CONFIRMADO' && s !== 'CANCELADO';
  }

  podeConfirmar(p: PedidoVenda): boolean {
    return (p.status || '').toUpperCase() === 'RASCUNHO';
  }

  isPedidoInterno(p: PedidoVenda): boolean {
    return (p.tipoPedido || 'VENDA').toUpperCase() === 'INTERNO';
  }

  destinatarioLabel(p: PedidoVenda): string {
    if (this.isPedidoInterno(p)) {
      return p.funcionarioNome || `Func. #${p.funcionarioId}`;
    }
    return p.consumidorNome || String(p.consumidorId);
  }

  statusClass(status: string): string {
    const s = (status || '').toUpperCase();
    if (s === 'CONFIRMADO') return 'confirmado';
    if (s === 'CANCELADO') return 'cancelado';
    return 'rascunho';
  }

  labelForma(f?: string): string {
    if (!f) return '—';
    return this.formasPagamento.find((x) => x.value === f)?.label || f;
  }

  labelCondicao(c?: string): string {
    if (!c) return '—';
    return this.condicoesPagamento.find((x) => x.value === c)?.label || c;
  }

  submitForm(): void {
    if (this.isInterno) {
      if (!this.funcionarioId) {
        this.onError('Selecione o funcionário!');
        return;
      }
    } else if (!this.consumidorId) {
      this.onError('Selecione o cliente!');
      return;
    }

    const itensValidos = this.itens.filter((i) => i.produtoId && i.quantidade > 0);
    if (itensValidos.length === 0) {
      this.onError('Adicione pelo menos um produto!');
      return;
    }

    const payload: Parameters<PedidoVendaService['criar']>[0] = {
      tipoPedido: this.tipoPedido,
      vendedorId: this.vendedorId || undefined,
      dataHora: this.dataHora ? new Date(this.dataHora).toISOString() : undefined,
      observacao: this.observacao || undefined,
      confirmar: this.confirmarAoSalvar,
      itens: itensValidos.map((i) => ({
        produtoId: i.produtoId!,
        quantidade: i.quantidade,
        precoUnitario: this.isInterno ? 0 : i.precoUnitario,
      })) as PedidoVendaItem[],
    };

    if (this.isInterno) {
      payload.funcionarioId = this.funcionarioId!;
    } else {
      payload.consumidorId = this.consumidorId!;
      payload.formaPagamento = this.formaPagamento;
      payload.condicaoPagamento = this.condicaoPagamento;
    }

    this.loading = true;
    this.pedidoService.criar(payload).subscribe({
      next: () => {
        this.loading = false;
        const msg = this.isInterno
          ? (this.confirmarAoSalvar ? 'Retirada confirmada — estoque atualizado!' : 'Retirada salva como rascunho!')
          : (this.confirmarAoSalvar ? 'Pedido confirmado — estoque atualizado!' : 'Pedido salvo como rascunho!');
        this.onSuccess(msg);
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || err.error?.message || 'Erro ao criar pedido!');
      },
    });
  }

  confirmar(id: number): void {
    if (!confirm('Confirmar pedido e baixar estoque?')) return;
    this.loading = true;
    this.pedidoService.confirmar(id).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Pedido confirmado!');
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || 'Erro ao confirmar!');
      },
    });
  }

  cancelar(id: number): void {
    if (!confirm('Cancelar pedido?')) return;
    this.loading = true;
    this.pedidoService.cancelar(id).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Pedido cancelado!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao cancelar!');
      },
    });
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.fetchPedidos(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.fetchPedidos(this.currentPage + 1);
  }

  private onSuccess(msg: string): void {
    this.mensagem = msg;
    setTimeout(() => (this.mensagem = ''), 4000);
    this.toggleList();
  }

  private onError(msg: string): void {
    this.mensagemErro = msg;
    setTimeout(() => (this.mensagemErro = ''), 5000);
  }
}
