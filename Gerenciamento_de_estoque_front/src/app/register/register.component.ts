import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';  // Adicione esta importação

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  registerForm: FormGroup;
  successMessage: string = ''; // Variável para mensagens de sucesso
  errorMessage: string = ''; // Variável para mensagens de erro

  constructor(private fb: FormBuilder, private router: Router, private authService: AuthService) {
    this.registerForm = this.fb.group({
      username: ['', Validators.required],
      senha: ['', [Validators.required, Validators.minLength(6)]],
      email: ['', [Validators.required, Validators.email]],
    });
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      const { username, senha, email } = this.registerForm.value;

      this.authService
        .register({ username, senha, email, roles: ['USER'] }) // Chamada ao serviço
        .subscribe({
          next: (response) => {
            console.log('Registro bem-sucedido:', response); // Log da resposta do backend
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
    }
  }

  cancel(): void {
    this.registerForm.reset(); // Reseta o formulário
    this.successMessage = ''; // Limpa a mensagem de sucesso
    this.errorMessage = ''; // Limpa a mensagem de erro
  }
}
