
import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { CategoriaService } from '../services/categoria.service';
import { Produto } from '../models/produto.model';
import { Categoria } from '../models/categoria.model';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms'; // Import necessário para ngModel

@Component({
  selector: 'app-produto',
  templateUrl: './produto.component.html',
  styleUrls: ['./produto.component.scss'],
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule], // Adicionado FormsModule
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
  produtoDetalhado: Produto | null = null; // Armazenar produto detalhado

  // Novo: Armazena as quantidades a adicionar por produto
  quantidadesAdicionar: { [key: number]: number } = {};

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
    this.produtoDetalhado = null; // Limpar detalhes ao voltar para a lista
  }

  exibirCriarProduto(): void {
    this.exibirLista = false;
    this.exibirCriar = true;
    this.mensagem = '';
    this.mensagemErro = '';
    this.produtoDetalhado = null; // Limpar detalhes ao criar novo produto
  }

  carregarProdutos(page: number = 0): void {
    this.produtoService.listarProdutos(page).subscribe(
      (data) => {
        const produtosCarregados: Produto[] = data.content; // Tipagem explícita

        // Redução para somar quantidades de produtos duplicados
        this.produtos = produtosCarregados.reduce((acc: Produto[], produto: Produto) => {
          const existente = acc.find((p: Produto) => p.id === produto.id); // Busca por id
          if (existente) {
            // Caso o produto já exista, soma as quantidades
            existente.quantidade += produto.quantidade;
          } else {
            // Se o produto não existir, cria uma nova instância de Produto
            const novoProduto = new Produto(
              produto.id,
              produto.nome,
              produto.descricao,
              produto.preco,
              produto.quantidade,
              produto.quantidadeMinima,
              produto.categoria,
              produto.dateTime,
              produto.entradas,
              produto.saidas
            );
            acc.push(novoProduto); // Adiciona o produto ou o atualizado
          }
          return acc;
        }, []); // Inicializa o acumulador como um array vazio de tipo Produto[]

        // Atualiza informações de paginação
        this.currentPage = data.number;
        this.totalPages = data.totalPages;
      },
      (error) => {
        this.mensagemErro = 'Erro ao carregar produtos.';
        console.error('Erro ao carregar produtos:', error);
      }
    );
  }

  // Novo: Método para alternar a exibição do campo de adicionar quantidade
  toggleAdicionar(produtoId: number): void {
    if (this.quantidadesAdicionar[produtoId] !== undefined) {
      delete this.quantidadesAdicionar[produtoId];
    } else {
      this.quantidadesAdicionar[produtoId] = 0;
    }
  }

  // Novo: Método para confirmar a adição da quantidade
  confirmarAdicionar(produtoId: number): void {
    const quantidade = this.quantidadesAdicionar[produtoId];
    if (quantidade > 0) {
      this.produtoService.atualizarProdutoQuantidade(produtoId, quantidade).subscribe(
        (updatedProduto) => {
          // Atualiza a quantidade localmente
          const index = this.produtos.findIndex(p => p.id === produtoId);
          if (index !== -1) {
            this.produtos[index].quantidade = updatedProduto.quantidade;
          }
          this.mensagem = 'Quantidade adicionada com sucesso!';
          delete this.quantidadesAdicionar[produtoId];
        },
        (error) => {
          this.mensagemErro = 'Erro ao adicionar quantidade.';
          console.error('Erro ao atualizar produto', error);
        }
      );
    } else {
      this.mensagemErro = 'Quantidade inválida. Deve ser maior que zero.';
    }
  }

  verDetalhes(produtoId: number): void {
    this.produtoService.getProdutoById(produtoId).subscribe(
      (produto) => {
        this.produtoDetalhado = produto; // Exibir detalhes do produto
        this.exibirLista = false; // Esconde a lista ao exibir os detalhes
        this.exibirCriar = false;
      },
      (error) => {
        this.mensagemErro = 'Erro ao carregar detalhes do produto.';
        console.error('Erro ao carregar detalhes do produto:', error);
      }
    );
  }

  fecharDetalhes(): void {
    this.produtoDetalhado = null;
    this.exibirListaProdutos(); // Voltar para a lista de produtos
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

  // Métodos de navegação de páginas
  paginaAnterior(): void {
    if (this.currentPage > 0) {
      this.carregarProdutos(this.currentPage - 1); // Navega para a página anterior
    }
  }

  proximaPagina(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.carregarProdutos(this.currentPage + 1); // Navega para a próxima página
    }
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
}
