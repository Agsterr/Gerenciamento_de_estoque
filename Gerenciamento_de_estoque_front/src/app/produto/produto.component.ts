import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { CategoriaService } from '../services/categoria.service';
import { MovimentacaoProdutoService } from '../services/movimentacao-produto.service';
import { Produto } from '../models/produto.model';
import { Categoria } from '../models/categoria.model';
import { TipoMovimentacao } from '../models/movimentacao-produto.model';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  FormsModule,
} from '@angular/forms';
import { CommonModule } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { PageHintComponent } from '../shared/page-hint/page-hint.component';
import { PAGE_HINTS } from '../shared/help/help-content.data';
import { MovimentacaoModalComponent, MovimentacaoModalData } from '../movimentacao/movimentacao-modal/movimentacao-modal.component';

@Component({
  selector: 'app-produto',
  templateUrl: './produto.component.html',
  styleUrls: ['./produto.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, PageHintComponent],
})
export class ProdutoComponent implements OnInit {
  pageHint = PAGE_HINTS['produtos'];
  produtos: Produto[] = [];
  filteredProdutos: Produto[] = [];
  searchTerm: string = '';
  barcodeSearch: string = '';
  filtroEstoqueBaixo = false;
  categorias: Categoria[] = [];
  produtoForm: FormGroup;
  mensagem = '';
  mensagemErro = '';
  loading = false;
  currentPage = 0;
  pageSize = 10;
  pageSizeOptions = [10, 20, 30, 40];
  totalPages = 0;
  totalElements = 0;
  showList = true;
  showAddForm = false;
  editingProduto = false;
  produtoEditando: Produto | null = null;
  formSubmitted = false;

  constructor(
    private produtoService: ProdutoService,
    private categoriaService: CategoriaService,
    private movimentacaoService: MovimentacaoProdutoService,
    private dialog: MatDialog,
    private fb: FormBuilder
  ) {
    this.produtoForm = this.fb.group({
      id: [null],
      nome: ['', Validators.required],
      descricao: ['', Validators.required],
      preco: [null, [Validators.required, Validators.min(0.01)]],
      quantidade: [0, [Validators.min(0)]],
      quantidadeMinima: [null, [Validators.required, Validators.min(0)]],
      categoriaId: [null, Validators.required],
      sku: [''],
      codigoBarras: [''],
      custoMedio: [null, [Validators.min(0)]],
    });
  }

  campoInvalido(campo: string): boolean {
    const control = this.produtoForm.get(campo);
    return !!(control && control.invalid && (control.touched || this.formSubmitted));
  }

  getCampoErro(campo: string): string {
    const control = this.produtoForm.get(campo);
    if (!control?.errors) {
      return '';
    }
    const labels: Record<string, string> = {
      nome: 'Nome',
      descricao: 'Descrição',
      preco: 'Preço',
      quantidade: 'Quantidade',
      quantidadeMinima: 'Quantidade mínima',
      categoriaId: 'Categoria',
    };
    if (control.errors['required']) {
      return `${labels[campo] || 'Campo'} é obrigatório.`;
    }
    if (control.errors['min']) {
      return campo === 'preco' ? 'Preço deve ser maior que zero.' : 'Valor inválido.';
    }
    return 'Campo inválido.';
  }

  private resetFormulario(): void {
    this.formSubmitted = false;
    this.produtoForm.reset({
      id: null,
      nome: '',
      descricao: '',
      preco: null,
      quantidade: 0,
      quantidadeMinima: null,
      categoriaId: null,
      sku: '',
      codigoBarras: '',
      custoMedio: null,
    });
  }

  ngOnInit(): void {
    this.fetchProdutos();
    this.carregarCategorias();
  }

  fetchProdutos(page: number = 0): void {
    this.loading = true;
    this.produtoService.listarProdutos(page, this.pageSize).subscribe({
      next: (data) => {
        this.produtos = data.content;
        this.filteredProdutos = [...this.produtos];
        this.currentPage = data.currentPage || 0;
        this.totalPages = data.totalPages || 1;
        this.totalElements = data.totalElements || this.produtos.length;
        this.applyFilter();
        this.loading = false;
      },
      error: (error: any) => {
        this.mensagemErro = 'Erro ao carregar produtos.';
        this.loading = false;
        console.error('Erro ao carregar produtos:', error);
      }
    });
  }

  carregarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe({
      next: (data) => this.categorias = data.content,
      error: (error: any) => {
        this.mensagemErro = 'Erro ao carregar categorias.';
        console.error('Erro ao carregar categorias:', error);
      }
    });
  }

  toggleList(): void {
    this.showList = true;
    this.showAddForm = false;
    this.editingProduto = false;
    this.produtoEditando = null;
    this.resetFormulario();
    this.mensagem = '';
    this.mensagemErro = '';
    this.fetchProdutos(this.currentPage);
  }

  toggleAddForm(): void {
    this.showAddForm = true;
    this.showList = false;
    this.editingProduto = false;
    this.produtoEditando = null;
    this.resetFormulario();
    this.mensagem = '';
    this.mensagemErro = '';
  }

  submitAddForm(): void {
    this.formSubmitted = true;
    this.mensagem = '';
    this.produtoForm.markAllAsTouched();

    if (this.produtoForm.invalid) {
      this.mensagemErro = 'Preencha os campos obrigatórios destacados em vermelho.';
      return;
    }

    this.mensagemErro = '';
    this.editingProduto ? this.updateProduto() : this.createProduto();
  }

  createProduto(): void {
    const novo = this.produtoForm.value as Produto;
    this.loading = true;
    this.produtoService.criarProduto(novo).subscribe({
      next: () => {
        this.loading = false;
        this.mensagem = 'Produto adicionado!';
        this.toggleList();
      },
      error: err => {
        this.loading = false;
        const details = err.error?.details;
        if (Array.isArray(details) && details.length) {
          this.mensagemErro = details.join(' · ');
        } else {
          this.mensagemErro = err.error?.error || 'Erro ao adicionar produto!';
        }
      }
    });
  }

  updateProduto(): void {
    const upd = this.produtoForm.value as Produto;
    if (!upd.id) {
      this.mensagemErro = 'ID obrigatório para edição';
      return;
    }
    // Estoque não é editado pelo formulário
    if (this.produtoEditando) {
      upd.quantidade = this.produtoEditando.quantidade;
    }
    this.loading = true;
    this.produtoService.atualizarProduto(upd, upd.id).subscribe({
      next: () => {
        this.loading = false;
        this.mensagem = 'Produto atualizado!';
        this.toggleList();
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao editar produto!';
      }
    });
  }

  deleteProduto(id: number): void {
    if (!confirm('Confirma exclusão do produto?')) return;
    this.loading = true;
    this.produtoService.deletarProduto(id).subscribe({
      next: () => {
        this.loading = false;
        this.mensagem = 'Produto deletado!';
        this.fetchProdutos(this.currentPage);
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao deletar produto!';
      }
    });
  }

  applyFilter(): void {
    const t = this.searchTerm.trim().toLowerCase();
    let list = [...this.produtos];
    if (t) {
      list = list.filter(p =>
          p.nome.toLowerCase().includes(t) ||
          (p.descricao && p.descricao.toLowerCase().includes(t)) ||
          (p.categoriaNome && p.categoriaNome.toLowerCase().includes(t)) ||
          (p.sku && p.sku.toLowerCase().includes(t)) ||
          (p.codigoBarras && p.codigoBarras.includes(t))
        );
    }
    if (this.filtroEstoqueBaixo) {
      list = list.filter(p => this.isEstoqueBaixo(p));
    }
    this.filteredProdutos = list;
  }

  isEstoqueBaixo(p: Produto): boolean {
    const qtd = p.quantidade ?? 0;
    const min = p.quantidadeMinima ?? 0;
    return qtd <= min;
  }

  buscarPorBarcode(): void {
    if (!this.barcodeSearch.trim()) return;
    this.loading = true;
    this.produtoService.buscarPorCodigoBarras(this.barcodeSearch.trim()).subscribe({
      next: (p) => {
        this.loading = false;
        this.filteredProdutos = [Produto.fromDto(p)];
        this.mensagem = `Produto encontrado: ${(p as any).nome}`;
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Nenhum produto encontrado para este código de barras.';
      }
    });
  }

  editProduto(p: Produto): void {
    this.editingProduto = true;
    this.showAddForm = true;
    this.showList = false;
    this.produtoEditando = p;
    this.formSubmitted = false;
    this.produtoForm.patchValue({
      id: p.id,
      nome: p.nome,
      descricao: p.descricao,
      preco: p.preco,
      quantidade: p.quantidade,
      quantidadeMinima: p.quantidadeMinima,
      categoriaId: p.categoriaId,
      sku: p.sku,
      codigoBarras: p.codigoBarras,
      custoMedio: p.custoMedio
    });
    this.mensagem = '';
    this.mensagemErro = '';
  }

  irParaPagina(pagina: number): void {
    if (pagina >= 0 && pagina < this.totalPages) {
      this.fetchProdutos(pagina);
    }
  }

  onPageSizeChange(event: any): void {
    this.currentPage = 0;
    this.pageSize = event.target.value;
    this.fetchProdutos(0);
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.fetchProdutos(this.currentPage - 1);
    }
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) {
      this.fetchProdutos(this.currentPage + 1);
    }
  }

  entradaEstoque(produto: Produto): void {
    const modalData: MovimentacaoModalData = {
      modo: 'criar',
      tipoMovimentacao: TipoMovimentacao.ENTRADA,
      produtoId: produto.id,
      nomeProduto: produto.nome,
      bloquearTipo: true,
      tituloCustomizado: 'Entrada de Estoque',
      produtoSomenteLeitura: true,
    };

    const dialogRef = this.dialog.open(MovimentacaoModalComponent, {
      width: '600px',
      maxWidth: '90vw',
      data: modalData,
      disableClose: false,
      autoFocus: true,
    });

    dialogRef.afterClosed().subscribe(result => {
      if (!result?.success) return;

      this.movimentacaoService.registrarMovimentacao({
        ...result.data,
        observacao: result.observacoes?.trim() || undefined,
      }).subscribe({
        next: () => {
          this.mensagem = `Entrada registrada: +${result.data.quantidade} un. em "${produto.nome}".`;
          this.mensagemErro = '';
          if (this.produtoEditando?.id === produto.id) {
            this.produtoEditando = { ...this.produtoEditando, quantidade: (produto.quantidade ?? 0) + result.data.quantidade };
          }
          this.fetchProdutos(this.currentPage);
        },
        error: (err) => {
          this.mensagemErro = err.error?.message || err.error?.error || 'Erro ao registrar entrada de estoque.';
        },
      });
    });
  }
}
