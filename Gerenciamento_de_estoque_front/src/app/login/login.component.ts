import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { Router, RouterModule } from '@angular/router';
import { LoginRequest } from '../models/login-request.model';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../environments/environment';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    RouterModule,
  ],
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage = '';
  hidePassword = true;
  isSubmitting = false;
  isDemoSubmitting = false;
  registrationEnabled = false;
  demoEnabled = true;
  demoUsername = 'demo';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      senha: ['', Validators.required],
      lembrarCredenciais: [false],
    });

    const savedUsername = localStorage.getItem('savedUsername');
    const savedPassword = localStorage.getItem('savedPassword');

    if (savedUsername && savedPassword) {
      this.loginForm.patchValue({
        username: savedUsername,
        senha: savedPassword,
        lembrarCredenciais: true,
      });
    }
  }

  ngOnInit(): void {
    this.authService.markVisited();
    if (this.authService.isTokenValid()) {
      this.router.navigate(['/dashboard'], { replaceUrl: true });
    }
    this.authService.getPublicConfig().subscribe({
      next: (cfg) => {
        this.registrationEnabled = cfg.registrationEnabled;
        this.demoEnabled = cfg.demoEnabled;
        this.demoUsername = cfg.demoUsername || 'demo';
      },
      error: () => {},
    });
  }

  onSubmit(): void {
    if (!this.loginForm.valid) {
      this.errorMessage = 'Preencha todos os campos obrigatórios.';
      this.snackBar.open(this.errorMessage, 'OK', { duration: 3000 });
      return;
    }
    this.submitLogin(this.loginForm.value.username, this.loginForm.value.senha, true);
  }

  experimentarDemo(): void {
    this.isDemoSubmitting = true;
    this.errorMessage = '';
    this.authService.loginDemo(this.demoUsername).subscribe({
      next: (response) => {
        this.isDemoSubmitting = false;
        if (response?.token) {
          this.snackBar.open(
            'Modo demonstração: seus dados serão apagados ao sair. Credenciais: demo / demo123',
            'OK',
            { duration: 6000 }
          );
        }
      },
      error: () => {
        this.isDemoSubmitting = false;
        this.errorMessage = 'Não foi possível entrar no modo demo.';
      },
    });
  }

  private submitLogin(username: string, senha: string, remember: boolean): void {
    this.isSubmitting = true;
    const loginData: LoginRequest = { username, senha };

    if (remember) {
      localStorage.setItem('savedUsername', username);
      localStorage.setItem('savedPassword', senha);
    } else {
      localStorage.removeItem('savedUsername');
      localStorage.removeItem('savedPassword');
      localStorage.removeItem('savedOrgId');
    }

    this.authService.login(loginData).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        if (!environment.production) {
          console.log('Resposta do login:', response);
        }
        if (!response.token) {
          this.errorMessage = 'Credenciais inválidas.';
          this.snackBar.open(this.errorMessage, 'OK', { duration: 3500 });
        }
      },
      error: () => {
        this.isSubmitting = false;
        this.errorMessage = 'Erro ao fazer login. Tente novamente.';
        this.snackBar.open(this.errorMessage, 'OK', { duration: 4000 });
      },
    });
  }
}
