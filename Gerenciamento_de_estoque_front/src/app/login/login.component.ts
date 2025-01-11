
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common'; // Certifique-se de que o CommonModule está importado
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [ReactiveFormsModule, CommonModule], // Certifique-se de que ambos estão aqui
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
    });
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      const { username, senha } = this.loginForm.value;
      this.authService.login(username, senha).subscribe({
        next: (response) => {
          // Se o login for bem-sucedido, armazena o token e redireciona para o dashboard
          console.log('Login bem-sucedido:', response);
          if (response.token) {
            this.router.navigate(['/dashboard']);
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
