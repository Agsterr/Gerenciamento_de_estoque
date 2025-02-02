import { Component, OnInit } from '@angular/core';
import { CategoriaService } from '../services/categoria.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Categoria } from '../models/categoria.model';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ApiResponse } from '../models/api-response.model';

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
          this.categoriaForm.reset();
          this.showNovaCategoriaInput = false;
  
          // Atualiza a lista de categorias
          this.carregarCategorias();  // Recarrega categorias após a criação
        },
        (error) => {
          this.mensagemErro = 'Erro ao criar categoria.';
          console.error('Erro ao criar categoria:', error);
        }
      );
    } else {
      this.mensagemErro = 'Por favor, preencha o campo nome da categoria.';
    }
  }
}