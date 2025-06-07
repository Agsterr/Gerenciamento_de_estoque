import { Component, OnInit } from '@angular/core';
import { CategoriaService } from '../services/categoria.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Categoria } from '../models/categoria.model';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-categoria',
  templateUrl: './categoria.component.html',
  styleUrls: ['./categoria.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
})
export class CategoriaComponent implements OnInit {
  categoriaForm: FormGroup;
  categorias: Categoria[] = [];
  mensagem: string = '';
  mensagemErro: string = '';
  mensagemTipo: 'sucesso' | 'erro' | '' = '';
  showNovaCategoriaInput = false;
  orgId: string = '';

  constructor(
    private categoriaService: CategoriaService,
    private fb: FormBuilder,
    private router: Router
  ) {
    this.categoriaForm = this.fb.group({
      nome: ['', [Validators.required, Validators.minLength(3)]],
      descricao: ['', [Validators.maxLength(255)]],
    });
  }

  ngOnInit(): void {
    try {
      this.orgId = this.categoriaService.getOrgId();
      this.carregarCategorias();
    } catch (error) {
      console.error('Erro ao obter orgId:', error);
      this.mensagemErro = 'OrgId não encontrado. O usuário precisa estar autenticado.';
      this.router.navigate(['/login']);
    }
  }

  /** Carrega as categorias da API */
  carregarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe({
      next: (categorias: Categoria[]) => {
        this.categorias = categorias;
      },
      error: (error: HttpErrorResponse) => {
        this.mensagemErro = 'Erro ao carregar categorias.';
        console.error('Erro ao carregar categorias:', error);
      }
    });
  }

  /** Mostra ou esconde o formulário de criação */
  toggleNovaCategoriaForm(): void {
    this.showNovaCategoriaInput = !this.showNovaCategoriaInput;
    this.mensagemErro = '';
    this.mensagem = '';
    this.mensagemTipo = '';
    this.categoriaForm.reset();
  }

  /** Cria nova categoria após validações */
  criarCategoria(): void {
    if (this.categoriaForm.valid) {
      const nome = this.categoriaForm.get('nome')?.value.trim();
      const descricao = this.categoriaForm.get('descricao')?.value?.trim() || '';

      if (this.categorias.some(cat => cat.nome.toLowerCase() === nome.toLowerCase())) {
        this.mensagemErro = 'Categoria já existe!';
        this.mensagemTipo = 'erro';
        return;
      }

      this.categoriaService.criarCategoria(nome, descricao).subscribe({
        next: (categoria) => {
          this.mensagem = 'Categoria criada com sucesso!';
          this.mensagemTipo = 'sucesso';
          this.categoriaForm.reset();
          this.showNovaCategoriaInput = false;
          this.carregarCategorias();
        },
        error: (error: HttpErrorResponse) => {
          this.mensagemErro = 'Erro ao criar categoria.';
          console.error('Erro ao criar categoria:', error);
          this.mensagemTipo = 'erro';
        }
      });
    } else {
      this.mensagemErro = 'Preencha corretamente os campos do formulário.';
      this.mensagemTipo = 'erro';
    }
  }

  /** Deleta uma categoria após confirmação */
  deletarCategoria(id: number): void {
    if (confirm('Tem certeza que deseja deletar esta categoria?')) {
      this.categoriaService.deletarCategoria(id).subscribe({
        next: () => {
          this.mensagem = 'Categoria deletada com sucesso!';
          this.mensagemTipo = 'sucesso';
          this.categorias = this.categorias.filter(cat => cat.id !== id);
        },
        error: (error: HttpErrorResponse) => {
          this.mensagemErro = 'Erro ao deletar categoria.';
          console.error('Erro ao deletar categoria:', error);
          this.mensagemTipo = 'erro';
        }
      });
    }
  }
}
