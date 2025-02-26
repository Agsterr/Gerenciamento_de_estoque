
import { Component, OnInit } from '@angular/core';
import { ConsumidorService } from '../services/consumidor.service';  // Serviço para lidar com os dados do consumidor
import { Consumer } from '../models/consumer.model';  // Modelo do consumidor
import { FormBuilder, FormGroup, Validators } from '@angular/forms';  // Para formulários reativos
import { Router } from '@angular/router';  // Para navegação entre páginas
import { CommonModule } from '@angular/common';  // Para importar módulos comuns
import { FormsModule, ReactiveFormsModule } from '@angular/forms';  // Para usar formulários reativos e ngModel

@Component({
  selector: 'app-consumers',
  templateUrl: './consumidor.component.html',
  styleUrls: ['./consumidor.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],  // Importando os módulos necessários
})
export class ConsumersComponent implements OnInit {
  consumerForm: FormGroup;
  consumers: Consumer[] = [];
  filteredConsumers: Consumer[] = [];
  searchTerm: string = '';
  showList: boolean = false;
  showAddForm: boolean = false;
  editingConsumer: boolean = false;
  novoConsumidor: Partial<Consumer> = { nome: '', cpf: '', endereco: '' };
  mensagem: string = '';
  mensagemErro: string = '';
  mensagemTipo: string = ''; // Para definir se a mensagem é de sucesso ou erro

  constructor(
    private consumidorService: ConsumidorService,  // Serviço que lida com os dados do consumidor
    private fb: FormBuilder,  // Para criar formulários reativos
    private router: Router  // Para redirecionar o usuário
  ) {
    this.consumerForm = this.fb.group({
      nome: ['', Validators.required],
      cpf: ['', Validators.required],
      endereco: ['', Validators.required],
    });
  }

  ngOnInit(): void {
    this.fetchConsumers(); // Carregar consumidores ao inicializar
  }

  // Método para buscar consumidores
  fetchConsumers(): void {
    this.consumidorService.listarConsumidoresPorOrg().subscribe({
      next: (data) => {
        this.consumers = data.consumidores.sort((a, b) => a.nome.localeCompare(b.nome));
        this.mensagem = data.message;  // Atualiza a mensagem com a resposta do backend
        this.applyFilter();
      },
      error: (err) => {
        console.error('Erro ao buscar consumidores:', err);
        alert('Erro ao buscar consumidores!');
      },
    });
  }

  // Alterna entre a exibição da lista e o formulário
  toggleList(): void {
    this.showList = !this.showList;
    this.showAddForm = false;
    if (this.showList) {
      this.fetchConsumers();
    }
  }

  // Alterna entre o formulário de adicionar ou editar
  toggleAddForm(): void {
    this.showAddForm = !this.showAddForm;
    this.showList = false;
    this.editingConsumer = false;
    this.novoConsumidor = { nome: '', cpf: '', endereco: '' };  // Limpar campos
  }

  // Envia o formulário para adicionar ou editar um consumidor
  submitAddForm(): void {
    console.log('Submit chamado!');  // Verificar se o evento está sendo disparado
    if (this.consumerForm.invalid) {
      this.mensagemErro = 'Por favor, preencha todos os campos corretamente!';
      return;
    }
  
    console.log('Formulário válido. Submetendo...');
    if (this.editingConsumer) {
      this.updateConsumer();  // Atualiza o consumidor se estiver em modo de edição
    } else {
      this.createConsumer();  // Cria um novo consumidor se não estiver editando
    }
  }
  

  // Método para criar um novo consumidor
// Método para criar um novo consumidor
createConsumer(): void {
  console.log('Criando consumidor com os seguintes dados:', this.consumerForm.value);

  // Cria o consumidor a partir dos valores do formulário
  const novoConsumidor: Partial<Consumer> = {
    nome: this.consumerForm.value.nome,
    cpf: this.consumerForm.value.cpf,
    endereco: this.consumerForm.value.endereco,
  };

  // Envia os dados para o serviço de criar consumidor
  this.consumidorService.criarConsumidor(novoConsumidor).subscribe({
    next: (response) => {
      console.log('Consumidor criado com sucesso!', response);  // Log de sucesso
      this.mensagem = 'Consumidor adicionado com sucesso!';
      this.mensagemTipo = 'sucesso';
      this.resetForm();
      this.fetchConsumers(); // Atualiza a lista de consumidores
    },
    error: (err) => {
      console.error('Erro ao criar consumidor:', err);
      this.mensagemErro = 'Erro ao adicionar consumidor!';
      this.mensagemTipo = 'erro';
    }
  });
}
 

  // Método para editar um consumidor existente
 
  
  updateConsumer(): void {
    const updatedConsumer: Partial<Consumer> = this.consumerForm.value;
  
    // Verifique se o id está presente no formulário
    if (!updatedConsumer.id) {
      const storedId = localStorage.getItem('consumerId');  // Tentando pegar o id do localStorage
      if (storedId) {
        updatedConsumer.id = parseInt(storedId, 10);  // Converte o id para número
      } else {
        console.error('ID do consumidor não encontrado no localStorage.');
        this.mensagemErro = 'ID do consumidor não encontrado.';
        return; // Interrompe a execução caso o id não seja encontrado
      }
    }
  
    console.log('Atualizando consumidor com os seguintes dados:', updatedConsumer);
  
    this.consumidorService.editarConsumidor(updatedConsumer).subscribe({
      next: (data) => {
        console.log('Resposta da atualização:', data);
        this.mensagem = 'Consumidor atualizado com sucesso!';
        this.mensagemTipo = 'sucesso';
        this.resetForm();
        this.fetchConsumers();  // Atualiza a lista de consumidores
      },
      error: (err) => {
        console.error('Erro ao editar consumidor:', err);
        this.mensagemErro = 'Erro ao editar consumidor!';
        this.mensagemTipo = 'erro';
      },
    });
  }
   
   
   
  
  
  
  

  // Aplica o filtro de busca
  applyFilter(): void {
    if (this.searchTerm.trim() === '') {
      this.filteredConsumers = this.consumers;
    } else {
      this.filteredConsumers = this.consumers.filter((consumer) =>
        consumer.nome.toLowerCase().includes(this.searchTerm.toLowerCase())
      );
    }
  }

  // Deleta um consumidor
  deleteConsumer(id: number): void {
    if (confirm('Tem certeza que deseja deletar este consumidor?')) {
      this.consumidorService.deletarConsumidor(id).subscribe({
        next: () => {
          this.mensagem = 'Consumidor deletado com sucesso!';
          this.consumers = this.consumers.filter((consumer) => consumer.id !== id);
          this.applyFilter();
        },
        error: (err) => {
          this.mensagemErro = 'Erro ao deletar consumidor!';
          console.error('Erro ao deletar consumidor:', err);
        },
      });
    }
  }

// Inicia a edição de um consumidor


// Editar consumidor

editConsumer(consumer: Consumer): void {
  this.editingConsumer = true;
  this.showAddForm = true;
  this.showList = false;

  // Armazenando o id no localStorage
  localStorage.setItem('consumerId', consumer.id.toString());  // Armazena o id do consumidor como string no localStorage

  // Preenche o formulário com os dados do consumidor, incluindo o id
  this.consumerForm.patchValue({
    id: consumer.id,  // O id será incluído no formulário
    nome: consumer.nome,
    cpf: consumer.cpf,
    endereco: consumer.endereco
  });

  console.log('Consumidor para editar:', consumer);  // Verifique se o id está aqui
}




  resetForm(): void {
    // Limpa os campos do formulário
    this.consumerForm.reset();
    this.novoConsumidor = { nome: '', cpf: '', endereco: '' }; // Reseta o objeto novoConsumidor
    this.editingConsumer = false; // Define como não está mais em modo de edição
    this.showAddForm = false; // Esconde o formulário de adicionar/editar
    this.showList = true; // Exibe a lista de consumidores
  }
  
  
}
