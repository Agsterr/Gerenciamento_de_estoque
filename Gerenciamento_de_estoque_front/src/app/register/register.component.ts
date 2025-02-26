import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';
import { HttpErrorResponse } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule], // Importando módulos necessários
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  roles: any[] = []; // Lista de roles
  successMessage: string = ''; // Variável para mensagens de sucesso
  errorMessage: string = ''; // Variável para mensagens de erro

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private roleService: RoleService
  ) {
    this.registerForm = this.fb.group({
      username: ['', Validators.required],
      senha: ['', [Validators.required, Validators.minLength(6)]],
      email: ['', [Validators.required, Validators.email]],
      orgId: ['', Validators.required], // Campo para o ID da organização
      roles: [[], Validators.required], // Campo para selecionar múltiplas roles
    });
  }

  ngOnInit(): void {
    // Verifica se há uma mensagem de erro armazenada no localStorage
    const errorMessage = localStorage.getItem('authErrorMessage');
    if (errorMessage) {
      this.errorMessage = errorMessage;
      localStorage.removeItem('authErrorMessage'); // Limpa a mensagem após exibir
    }

    // Carregar as roles do backend
    this.roleService.listarRoles().subscribe({
      next: (roles: any[]) => {
        this.roles = roles; // Armazenar as roles para exibir no formulário
      },
      error: (err: HttpErrorResponse) => {
        console.error('Erro ao carregar roles:', err);
        this.errorMessage = 'Erro ao carregar roles.';
      }
    });
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      const { username, senha, email, orgId, roles } = this.registerForm.value;

      // Verifica se pelo menos uma role foi selecionada
      if (roles.length === 0) {
        this.errorMessage = 'Você precisa selecionar pelo menos uma role.';
        return;
      }

      // Chama o serviço de autenticação para registrar o usuário
      this.authService
        .register({ username, senha, email, orgId, roles })
        .subscribe({
          next: (response) => {
            console.log('Registro bem-sucedido:', response);
            this.successMessage = 'Usuário registrado com sucesso!';
            this.errorMessage = '';
            this.router.navigate(['/login']); // Redireciona para a tela de login
          },
          error: (err: HttpErrorResponse) => {
            console.error('Erro ao registrar:', err);
            this.errorMessage = 'Erro ao registrar. Verifique os dados.';
            this.successMessage = '';
          },
        });
    } else {
      this.errorMessage = 'Por favor, preencha todos os campos corretamente.';
    }
  }

  cancel(): void {
    this.registerForm.reset();
    this.successMessage = '';
    this.errorMessage = '';
  }
}
