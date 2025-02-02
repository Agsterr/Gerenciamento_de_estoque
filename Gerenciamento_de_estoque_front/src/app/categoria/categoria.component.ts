import { Component, OnInit } from '@angular/core';
import { CategoriaService } from '../services/categoria.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Categoria } from '../models/categoria.model';
import { ApiResponse } from '../models/api-response.model';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';

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
  mensagemTipo: string = ''; // Para definir se a mensagem é de sucesso ou erro
  showNovaCategoriaInput: boolean = false; // Controla a exibição do formulário de criação

  constructor(
    private categoriaService: CategoriaService,
    private fb: FormBuilder
  ) {
    this.categoriaForm = this.fb.group({
      nome: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.carregarCategorias();
  }

  // Método para carregar as categorias da API
  carregarCategorias(): void {
    this.categoriaService.listarCategorias().subscribe(
      (data) => {
        if (Array.isArray(data)) {
          this.categorias = data;
        } else {
          this.mensagemErro = 'Erro ao carregar categorias.';
        }
      },
      (error) => {
        this.mensagemErro = 'Erro ao carregar categorias.';
        console.error('Erro ao carregar categorias:', error);
      }
    );
  }

  // Método para alternar a visibilidade do formulário de nova categoria
  toggleNovaCategoriaForm(): void {
    this.showNovaCategoriaInput = !this.showNovaCategoriaInput;
    this.mensagemErro = ''; // Limpa a mensagem de erro ao abrir o formulário
    this.mensagem = ''; // Limpa a mensagem de sucesso ao abrir o formulário
  }

  // Método para criar uma nova categoria
  criarCategoria(): void {
    if (this.categoriaForm.valid) {
      const nomeCategoria = this.categoriaForm.get('nome')?.value.trim();

      // Verifica se o nome da categoria já existe na lista de categorias carregada
      if (this.categorias.some(categoria => categoria.nome.toLowerCase() === nomeCategoria.toLowerCase())) {
        this.mensagemErro = 'Categoria já existe!';
        console.log('Categoria já existe!');
        return; // Não envia a requisição se a categoria já existir
      }

      // Se não existir, então cria a categoria
      this.categoriaService.criarCategoria(nomeCategoria).subscribe(
        (response: ApiResponse) => {
          const categoria = response.data;
          this.mensagem = 'Categoria criada com sucesso!';
          this.mensagemTipo = 'sucesso';  // Define que a mensagem é de sucesso
          this.categoriaForm.reset();
          this.showNovaCategoriaInput = false;
          this.carregarCategorias();  // Atualiza a lista de categorias
        },
        (error) => {
          this.mensagemErro = 'Erro ao criar categoria.';
          console.error('Erro ao criar categoria:', error);
          this.mensagemTipo = 'erro';  // Define que a mensagem é de erro
        }
      );
    } else {
      this.mensagemErro = 'Por favor, preencha o campo nome da categoria.';
    }
  }

  deletarCategoria(id: number): void {
    if (confirm('Tem certeza que deseja deletar esta categoria?')) {
      this.categoriaService.deletarCategoria(id).subscribe(
        (categoriaDeletada) => {
          // Exibe mensagem de sucesso
          this.mensagem = `Categoria "${categoriaDeletada.nome}" deletada com sucesso!`;
  
          // Atualiza a lista de categorias removendo a categoria deletada
          this.categorias = this.categorias.filter(categoria => categoria.id !== categoriaDeletada.id);
        },
        (error) => {
          this.mensagemErro = 'Erro ao deletar categoria!';
          console.error('Erro ao deletar categoria:', error);
        }
      );
    }
  }
  
  
}
