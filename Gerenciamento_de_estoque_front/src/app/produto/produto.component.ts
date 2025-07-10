import { Component, OnInit } from '@angular/core';
import { ProdutoService } from '../services/produto.service';
import { CategoriaService } from '../services/categoria.service';
import { Produto } from '../models/produto.model';
import { Categoria } from '../models/categoria.model';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
  FormsModule,
} from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-produto',
  templateUrl: './produto.component.html',
  styleUrls: ['./produto.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
})
export class ProdutoComponent implements OnInit {
  produtos: Produto[] = [];
  produtosFiltrados: Produto[] = [];
  searchTerm: string = ''; // Variável de busca
  categorias: Categoria[] = [];
  produtoForm: FormGroup;
  mensagem = '';
  mensagemErro = '';
  alertaEstoqueBaixo = false;
  currentPage = 0;
  totalPages = 0;

  exibirLista = true;
  exibirCriar = false;
  exibirEditar = false;
  produtoDetalhado: Produto | null = null;

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
    this.verificarEstoqueBaixo();
  }
  

  

  exibirListaProdutos(): void {
    this.exibirLista = true;
    this.exibirCriar = false;
    this.exibirEditar = false;
    this.mensagem = '';
    this.mensagemErro = '';
    this.produtoDetalhado = null;
  }

  exibirCriarProduto(): void {
    this.exibirLista = false;
    this.exibirCriar = true;
    this.exibirEditar = false;
    this.mensagem = '';
    this.mensagemErro = '';
    this.produtoDetalhado = null;
  }



  

  atualizarProduto(): void {
    if (this.produtoForm.valid && this.produtoDetalhado) {
      const produtoAtualizado: Produto = {
        ...this.produtoForm.value,
        id: this.produtoDetalhado.id,
        orgId: Number(this.getOrgId()),
        ativo: true,
        criadoEm: this.produtoDetalhado.criadoEm,
        status: '',
        categoriaNome: '',
      };

      this.produtoService.atualizarProduto(produtoAtualizado, produtoAtualizado.id).subscribe({
        next: () => {
          this.mensagem = 'Produto atualizado com sucesso!';
          this.carregarProdutos();
          this.produtoForm.reset();
          this.exibirListaProdutos();
        },
        error: (error: any) => {
          this.mensagemErro = 'Erro ao atualizar produto.';
          console.error('Erro ao atualizar produto:', error);
        }
      });
    }
  }

  private getOrgId(): string {
    const token = localStorage.getItem('jwtToken');
    if (!token) throw new Error('Token não encontrado.');
    const payload = this.decodeJwt(token);
    if (payload && payload.org_id) return payload.org_id;
    throw new Error('OrgId não encontrado no token.');
  }

  private decodeJwt(token: string): any {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Token JWT inválido.');
    const payload = atob(parts[1]);
    return JSON.parse(payload);
  }

carregarProdutos(page: number = 0): void {
  this.produtoService.listarProdutos(page).subscribe({
    next: (data) => {
      this.produtos = data.content;
      this.produtosFiltrados = [...this.produtos]; // Inicia a lista filtrada com todos os produtos
      this.currentPage = data.currentPage || 0;
      this.totalPages = data.totalPages || 1;
      this.filtrarProdutos();  // Aplica o filtro após carregar produtos
    },
    error: (error: any) => {
      this.mensagemErro = 'Erro ao carregar produtos.';
      console.error('Erro ao carregar produtos:', error);
    }
  });
}



 
  // Método que será chamado para atualizar o produto já carregado no formulário
  editarProduto(): void {
    if (this.produtoForm.valid && this.produtoDetalhado) {
      // Monta o objeto Produto para envio (garantindo o id e orgId)
      const produtoAtualizado: Produto = {
        ...this.produtoForm.value,
        id: this.produtoDetalhado.id,
        orgId: Number(this.getOrgId()),
        ativo: true,
        criadoEm: this.produtoDetalhado.criadoEm,
        status: this.produtoDetalhado.status || '',
        categoriaNome: this.produtoDetalhado.categoriaNome || '',
      };

      this.produtoService.atualizarProduto(produtoAtualizado, produtoAtualizado.id).subscribe({
        next: () => {
          this.mensagem = 'Produto atualizado com sucesso!';
          this.carregarProdutos(this.currentPage);
          this.produtoForm.reset();
          this.exibirListaProdutos(); // Volta para a lista após editar
          this.produtoDetalhado = null;
        },
        error: (error: any) => {
          this.mensagemErro = 'Erro ao atualizar produto.';
          console.error('Erro ao atualizar produto:', error);
        }
      });
    } else {
      this.mensagemErro = 'Por favor, preencha corretamente os campos do formulário.';
    }
  }

  // Atualizar exibirEditarProduto para abrir o formulário com dados do produto
  exibirEditarProduto(produtoId: number): void {
    this.exibirLista = false;
    this.exibirCriar = false;
    this.exibirEditar = true;
    this.mensagem = '';
    this.mensagemErro = '';

    this.produtoService.getProdutoById(produtoId).subscribe({
      next: (produto) => {
        this.produtoForm.setValue({
          nome: produto.nome,
          descricao: produto.descricao,
          preco: produto.preco,
          quantidade: produto.quantidade,
          quantidadeMinima: produto.quantidadeMinima,
          categoriaId: produto.categoriaId,
        });
        this.produtoDetalhado = produto;
      },
      error: (error: any) => {
        this.mensagemErro = 'Erro ao carregar produto para edição.';
        console.error('Erro ao carregar produto:', error);
      }
    });
  }



  toggleAdicionar(produtoId: number): void {
    this.quantidadesAdicionar[produtoId] !== undefined
      ? delete this.quantidadesAdicionar[produtoId]
      : (this.quantidadesAdicionar[produtoId] = 0);
  }

  confirmarAdicionar(produtoId: number): void {
    const quantidade = this.quantidadesAdicionar[produtoId];
    if (quantidade > 0) {
      this.produtoService.atualizarProdutoQuantidade(produtoId, quantidade).subscribe({
        next: (updatedProduto) => {
          const index = this.produtos.findIndex(p => p.id === produtoId);
          if (index !== -1) this.produtos[index].quantidade = updatedProduto.quantidade;
          this.mensagem = 'Quantidade adicionada com sucesso!';
          delete this.quantidadesAdicionar[produtoId];
        },
        error: (error: any) => {
          this.mensagemErro = 'Erro ao adicionar quantidade.';
          console.error('Erro ao atualizar produto', error);
        }
      });
    } else {
      this.mensagemErro = 'Quantidade inválida. Deve ser maior que zero.';
    }
  }

  verDetalhes(produtoId: number): void {
    this.produtoService.getProdutoById(produtoId).subscribe({
      next: (produto) => {
        this.produtoDetalhado = produto;
        this.exibirLista = false;
        this.exibirCriar = false;
      },
      error: (error: any) => {
        this.mensagemErro = 'Erro ao carregar detalhes do produto.';
        console.error('Erro ao carregar detalhes do produto:', error);
      }
    });
  }

  fecharDetalhes(): void {
    this.produtoDetalhado = null;
    this.exibirListaProdutos();
  }

 criarProduto(): void {
  if (this.produtoForm.valid) {
    // Copia os valores do formulário para o objeto produto
    const produtoData = { ...this.produtoForm.value };

    // Garantir que a quantidade não seja null ou undefined, e atribuir valor padrão 0 se necessário
    if (produtoData.quantidade === null || produtoData.quantidade === undefined) {
      produtoData.quantidade = 0;  // Valor padrão
    }

    // Caso algum outro campo obrigatório tenha valor inválido, atribuir valor padrão ou notificar
    if (produtoData.nome.trim() === '') {
      this.mensagemErro = 'O nome do produto é obrigatório.';
      return;
    }

    // Envia a requisição para o serviço
    this.produtoService.criarProduto(produtoData).subscribe({
      next: () => {
        this.mensagem = 'Produto criado com sucesso!';
        this.carregarProdutos();  // Recarrega os produtos após a criação
        this.produtoForm.reset();  // Limpa o formulário
        this.exibirListaProdutos();  // Retorna para a lista de produtos
      },
      error: (error: any) => {
        // Exibe uma mensagem de erro caso a requisição falhe
        this.mensagemErro = 'Erro ao criar produto. Verifique os dados e tente novamente.';
        console.error('Erro ao criar produto:', error);

        // Detalha mais informações do erro no console
        if (error.error) {
          console.error('Detalhes do erro:', error.error);
        }
      }
    });
  } else {
    // Caso o formulário não seja válido, exibe um erro genérico
    this.mensagemErro = 'Por favor, preencha todos os campos obrigatórios.';
  }
}


 deletarProduto(produtoId: number): void {
  const confirmacao = window.confirm('Tem certeza que deseja excluir este produto?');

  if (confirmacao) {
    this.produtoService.deletarProduto(produtoId).subscribe({
      next: () => {
        this.mensagem = `Produto deletado com sucesso.`;
        this.carregarProdutos(this.currentPage);
      },
      error: (error: any) => {
        this.mensagemErro = 'Erro ao deletar produto.';
        console.error('Erro ao deletar produto:', error);
      }
    });
  } else {
    // Se o usuário cancelar, você pode fazer algo, por exemplo, exibir uma mensagem
    console.log('A exclusão do produto foi cancelada.');
  }
}


  paginaAnterior(): void {
    if (this.currentPage > 0) this.carregarProdutos(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage < this.totalPages - 1) this.carregarProdutos(this.currentPage + 1);
  }

  carregarCategorias(): void {
   this.categoriaService.listarCategorias().subscribe({
  next: (data) => this.categorias = data.content,  // data.content é o array Categoria[]
  error: (error: any) => {
    this.mensagemErro = 'Erro ao carregar categorias.';
    console.error('Erro ao carregar categorias:', error);
  }
});
  }

 verificarEstoqueBaixo(): void {
  this.produtoService.listarProdutosComEstoqueBaixo().subscribe({
    next: (produtosBaixoEstoque) => {
      this.alertaEstoqueBaixo = produtosBaixoEstoque.length > 0;
    },
    error: (error) => {
      console.error('Erro ao verificar estoque baixo:', error);
    }
  });
}

// Método para filtrar os produtos com base no texto de busca
filtrarProdutos(): void {
  if (this.searchTerm.trim() === '') {
    // Se o campo de busca estiver vazio, exibe todos os produtos
    this.produtosFiltrados = [...this.produtos]; 
  } else {
    // Se houver texto no campo de busca, filtra os produtos
    this.produtosFiltrados = this.produtos.filter(produto =>
      produto.nome.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
      produto.descricao.toLowerCase().includes(this.searchTerm.toLowerCase())
    );
  }
}




  fecharAlertaEstoque(): void {
    this.alertaEstoqueBaixo = false;
  }
}
