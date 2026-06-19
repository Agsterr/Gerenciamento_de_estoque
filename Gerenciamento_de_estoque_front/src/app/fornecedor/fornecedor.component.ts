import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FornecedorService, Fornecedor } from '../services/fornecedor.service';
import { PageHintComponent } from '../shared/page-hint/page-hint.component';
import { PAGE_HINTS } from '../shared/help/help-content.data';

@Component({
  selector: 'app-fornecedor',
  standalone: true,
  imports: [CommonModule, FormsModule, PageHintComponent],
  templateUrl: './fornecedor.component.html',
  styleUrls: ['./fornecedor.component.scss'],
})
export class FornecedorComponent implements OnInit {
  pageHint = PAGE_HINTS['fornecedores'];
  fornecedores: Fornecedor[] = [];
  filteredFornecedores: Fornecedor[] = [];
  searchTerm = '';
  showList = true;
  showAddForm = false;
  editing = false;
  mensagem = '';
  mensagemErro = '';
  loading = false;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  form: Partial<Fornecedor> = this.emptyForm();

  constructor(private fornecedorService: FornecedorService) {}

  ngOnInit(): void {
    this.fetchFornecedores();
  }

  emptyForm(): Partial<Fornecedor> {
    return { nome: '', cnpj: '', email: '', telefone: '', endereco: '' };
  }

  fetchFornecedores(page = 0): void {
    this.loading = true;
    this.fornecedorService.listar(page, this.pageSize).subscribe({
      next: (resp) => {
        this.currentPage = page;
        this.totalElements = resp.totalElements;
        this.totalPages = Math.ceil(resp.totalElements / this.pageSize) || 1;
        this.fornecedores = resp.content.sort((a, b) => a.nome.localeCompare(b.nome));
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao buscar fornecedores!');
      },
    });
  }

  toggleList(): void {
    this.showList = true;
    this.showAddForm = false;
    this.editing = false;
    this.form = this.emptyForm();
    this.mensagem = '';
    this.mensagemErro = '';
    this.fetchFornecedores(this.currentPage);
  }

  toggleAddForm(): void {
    this.showAddForm = true;
    this.showList = false;
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
    this.fornecedorService.criar(this.form).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Fornecedor adicionado!');
      },
      error: (err) => {
        this.loading = false;
        this.onError(err.error?.error || 'Erro ao adicionar fornecedor!');
      },
    });
  }

  update(): void {
    if (!this.form.id) {
      this.onError('ID obrigatório para edição');
      return;
    }
    this.loading = true;
    this.fornecedorService.atualizar(this.form.id, this.form).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Fornecedor atualizado!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao editar fornecedor!');
      },
    });
  }

  delete(id: number): void {
    if (!confirm('Confirma exclusão do fornecedor?')) return;
    this.loading = true;
    this.fornecedorService.excluir(id).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Fornecedor excluído!');
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao excluir fornecedor!');
      },
    });
  }

  edit(f: Fornecedor): void {
    this.editing = true;
    this.showAddForm = true;
    this.showList = false;
    this.form = { ...f };
    this.mensagem = '';
    this.mensagemErro = '';
  }

  applyFilter(): void {
    const t = this.searchTerm.trim().toLowerCase();
    this.filteredFornecedores = t
      ? this.fornecedores.filter(
          (f) =>
            f.nome.toLowerCase().includes(t) ||
            (f.cnpj && f.cnpj.includes(t)) ||
            (f.email && f.email.toLowerCase().includes(t))
        )
      : [...this.fornecedores];
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.fetchFornecedores(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.fetchFornecedores(this.currentPage + 1);
  }

  irParaPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.fetchFornecedores(p);
  }

  onPageSizeChange(): void {
    this.currentPage = 0;
    this.fetchFornecedores(0);
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
