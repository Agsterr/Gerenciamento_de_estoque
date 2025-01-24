
import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { CategoriaService } from '../services/categoria.service';
import { Produto } from '../models/produto.model';
import { Categoria } from '../models/categoria.model';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-produto',
  templateUrl: './produto.component.html',
  styleUrls: ['./produto.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
})
export class ProdutoComponent implements OnInit {
  produtos: Produto[] = [];
  categorias: Categoria[] = [];
  produtoForm: FormGroup;
  mensagem: string = ''; // Mensagem de sucesso
  mensagemErro: string = ''; // Mensagem de erro
  currentPage: number = 0;
  totalPages: number = 0;

  // Controle de exibição
  exibirLista: boolean = true;
  exibirCriar: boolean = false;

  constructor(
    private produtoService: ProdutoService,
    private categoriaService: CategoriaService,
    private fb: FormBuilder
  ) {
    this.produtoForm = this.fb.group({
      nome: ['', Validators.required],
      descricao: ['', Validators.required],
      preco: [0, [Validators.required, Validators.min(0.01)]],
      quantidade: [0, [Validators.required, Validators.min(0)]],
      quantidadeMinima: [0, [Validators.required, Validators.min(0)]],
      categoriaId: [null, Validators.required],
    });
  }

  ngOnInit(): void {
    this.carregarProdutos();
    this.carregarCategorias();
  }

  exibirListaProdutos(): void {
    this.exibirLista = true;
    this.exibirCriar = false;
    this.mensagem = '';
    this.mensagemErro = '';
  }

  exibirCriarProduto(): void {
    this.exibirLista = false;
    this.exibirCriar = true;
    this.mensagem = '';
    this.mensagemErro = '';
  }

  carregarProdutos(page: number = 0): void {
    this.produtoService.listarProdutos(page).subscribe(
      (data) => {
        this.produtos = data.content;
        this.currentPage = data.number;
        this.totalPages = data.totalPages;
      },
      (error) => {
        this.mensagemErro = 'Erro ao carregar produtos.';
        console.error('Erro ao carregar produtos:', error);
      }
    );
  }

  carregarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe(
      (data) => {
        this.categorias = data;
      },
      (error) => {
        this.mensagemErro = 'Erro ao carregar categorias.';
        console.error('Erro ao carregar categorias:', error);
      }
    );
  }

  criarProduto(): void {
    if (this.produtoForm.valid) {
      this.produtoService.criarProduto(this.produtoForm.value).subscribe(
        () => {
          this.mensagem = 'Produto criado com sucesso!';
          this.carregarProdutos(); // Atualiza a lista automaticamente
          this.produtoForm.reset();
          this.exibirListaProdutos(); // Alterna para a lista automaticamente
        },
        (error) => {
          this.mensagemErro = 'Erro ao criar produto.';
          console.error('Erro ao criar produto:', error);
        }
      );
    }
  }

  deletarProduto(produtoId: number): void {
    this.produtoService.deletarProduto(produtoId).subscribe(
      () => {
        this.mensagem = `Produto deletado com sucesso.`;
        this.carregarProdutos(this.currentPage); // Atualiza a lista de produtos na página atual
      },
      (error) => {
        this.mensagemErro = 'Erro ao deletar produto.';
        console.error('Erro ao deletar produto:', error);
      }
    );
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.carregarProdutos(this.currentPage - 1);
    }
  }

  proximaPagina(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.carregarProdutos(this.currentPage + 1);
    }
  }
}
