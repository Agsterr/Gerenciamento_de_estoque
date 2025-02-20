
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [ReactiveFormsModule, CommonModule],
})
export class LoginComponent {
  loginForm: FormGroup;
  errorMessage: string = '';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      senha: ['', Validators.required],
      orgId: ['', Validators.required], // Campo para o ID da organização
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      const { username, senha, orgId } = this.loginForm.value;

      // Chama o serviço de login com os parâmetros necessários
      this.authService.login(username, senha, orgId).subscribe({
        next: (response) => {
          if (response.token) {
            localStorage.setItem('auth_token', response.token);
            this.router.navigate(['/dashboard']); // Redireciona após o login bem-sucedido
          }
        },
        error: (err) => {
          this.errorMessage = 'Erro ao fazer login. Tente novamente.';
          console.error('Erro de login:', err);
        },
      });
    }
  }
}