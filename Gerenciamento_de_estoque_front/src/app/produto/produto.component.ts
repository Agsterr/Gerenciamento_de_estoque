
import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { CategoriaService } from '../services/categoria.service';
import { Produto } from '../models/produto.model';
import { Categoria } from '../models/categoria.model';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-produto',
  templateUrl: './produto.component.html',
  styleUrls: ['./produto.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule], // Corrigido para importar ReactiveFormsModule e FormsModule diretamente no componente
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
  exibirEditar: boolean = false; // Controle para exibir o formulário de edição
  produtoDetalhado: Produto | null = null; // Armazenar produto detalhado

  // Propriedade para armazenar as quantidades a adicionar por produto
  quantidadesAdicionar: { [key: number]: number } = {};

  constructor(
    private produtoService: ProdutoService,
    private categoriaService: CategoriaService,
    private fb: FormBuilder
  ) {
    // Inicializando o FormGroup com validações
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

  // Alterna para exibir a lista de produtos
  exibirListaProdutos(): void {
    this.exibirLista = true;
    this.exibirCriar = false;
    this.exibirEditar = false; // Fechar o formulário de edição
    this.mensagem = '';
    this.mensagemErro = '';
    this.produtoDetalhado = null;
  }

  // Alterna para exibir o formulário de criar produto
  exibirCriarProduto(): void {
    this.exibirLista = false;
    this.exibirCriar = true;
    this.exibirEditar = false; // Fechar o formulário de edição
    this.mensagem = '';
    this.mensagemErro = '';
    this.produtoDetalhado = null;
  }

 // Método para carregar o produto e exibir o formulário de edição
exibirEditarProduto(produtoId: number): void {
  this.exibirLista = false;
  this.exibirCriar = false;
  this.exibirEditar = true; // Exibe o formulário de edição
  this.mensagem = '';
  this.mensagemErro = '';

  // Chama o serviço para buscar os dados do produto pelo ID
  this.produtoService.getProdutoById(produtoId).subscribe(
    (produto) => {
      // Preenche o formulário com os dados do produto
      this.produtoForm.setValue({
        nome: produto.nome,
        descricao: produto.descricao,
        preco: produto.preco,
        quantidade: produto.quantidade,
        quantidadeMinima: produto.quantidadeMinima,
        categoriaId: produto.categoria.id,
      });

      // Aqui, você garante que o produtoDetalhado está sendo preenchido com o produto completo, incluindo o id.
      this.produtoDetalhado = produto;
    },
    (error) => {
      this.mensagemErro = 'Erro ao carregar produto.';
      console.error('Erro ao carregar produto:', error);
    }
  );
}
 

atualizarProduto(): void {
  if (this.produtoForm.valid) {
    const produtoAtualizado: Produto = { 
      ...this.produtoForm.value,
      id: this.produtoDetalhado?.id // Garantir que o id seja o do produto que estamos editando
    };

    this.produtoService.atualizarProduto(produtoAtualizado).subscribe(
      (produto) => {
        this.mensagem = 'Produto atualizado com sucesso!'; // Mensagem de sucesso
        this.carregarProdutos(); // Atualiza a lista automaticamente
        this.produtoForm.reset();
        this.exibirListaProdutos(); // Volta para a lista de produtos
      },
      (error) => {
        this.mensagemErro = 'Erro ao atualizar produto.';
        console.error('Erro ao atualizar produto:', error);
      }
    );
  }
}


  
 

  // Carrega a lista de produtos
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

  // Método para adicionar a quantidade de um produto
  toggleAdicionar(produtoId: number): void {
    if (this.quantidadesAdicionar[produtoId] !== undefined) {
      delete this.quantidadesAdicionar[produtoId];
    } else {
      this.quantidadesAdicionar[produtoId] = 0;
    }
  }

  // Método para confirmar a adição da quantidade ao produto
  confirmarAdicionar(produtoId: number): void {
    const quantidade = this.quantidadesAdicionar[produtoId];
    if (quantidade > 0) {
      // Chama o serviço para atualizar a quantidade do produto
      this.produtoService.atualizarProdutoQuantidade(produtoId, quantidade).subscribe(
        (updatedProduto: Produto) => {  // Define o tipo de updatedProduto
          // Atualiza a quantidade localmente na lista de produtos
          const index = this.produtos.findIndex(p => p.id === produtoId);
          if (index !== -1) {
            this.produtos[index].quantidade = updatedProduto.quantidade;
          }
          this.mensagem = 'Quantidade adicionada com sucesso!';
          delete this.quantidadesAdicionar[produtoId]; // Limpa o campo de quantidade adicionada
        },
        (error: any) => {  // Pode ser tipado como 'any' ou um tipo específico de erro
          this.mensagemErro = 'Erro ao adicionar quantidade.';
          console.error('Erro ao atualizar produto', error);
        }
      );
    } else {
      this.mensagemErro = 'Quantidade inválida. Deve ser maior que zero.';
    }
  }

  // Visualiza os detalhes do produto
  verDetalhes(produtoId: number): void {
    this.produtoService.getProdutoById(produtoId).subscribe(
      (produto) => {
        this.produtoDetalhado = produto;
        this.exibirLista = false; // Esconde a lista ao exibir os detalhes
        this.exibirCriar = false;
      },
      (error) => {
        this.mensagemErro = 'Erro ao carregar detalhes do produto.';
        console.error('Erro ao carregar detalhes do produto:', error);
      }
    );
  }

  // Fecha a visualização de detalhes do produto
  fecharDetalhes(): void {
    this.produtoDetalhado = null;
    this.exibirListaProdutos(); // Volta para a lista de produtos
  }

  // Cria um novo produto
  criarProduto(): void {
    if (this.produtoForm.valid) {
      this.produtoService.criarProduto(this.produtoForm.value).subscribe(
        () => {
          this.mensagem = 'Produto criado com sucesso!';
          this.carregarProdutos();
          this.produtoForm.reset();
          this.exibirListaProdutos();
        },
        (error) => {
          this.mensagemErro = 'Erro ao criar produto.';
          console.error('Erro ao criar produto:', error);
        }
      );
    }
  }

  // Deleta um produto
  deletarProduto(produtoId: number): void {
    this.produtoService.deletarProduto(produtoId).subscribe(
      () => {
        this.mensagem = `Produto deletado com sucesso.`;
        this.carregarProdutos(this.currentPage);
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
      this.carregarProdutos(this.currentPage - 1);
    }
  }

  proximaPagina(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.carregarProdutos(this.currentPage + 1);
    }
  }

  // Carrega as categorias disponíveis
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
