// src/app/login/login.component.ts
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';
import { LoginRequest } from '../models/login-request.model';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule
  ],
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage: string = '';
  hidePassword: boolean = true;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      senha: ['', Validators.required],
      orgId: ['', [Validators.required, Validators.pattern('^[0-9]+$')]],
      lembrarCredenciais: [false]
    });

    // Recuperar credenciais salvas
    const savedUsername = localStorage.getItem('savedUsername');
    const savedPassword = localStorage.getItem('savedPassword');
    const savedOrgId = localStorage.getItem('savedOrgId');
    
    if (savedUsername && savedPassword && savedOrgId) {
      this.loginForm.patchValue({
        username: savedUsername,
        senha: savedPassword,
        orgId: savedOrgId,
        lembrarCredenciais: true
      });
    }
  }

  ngOnInit(): void {
    // Qualquer lógica de inicialização adicional pode ser colocada aqui
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      const loginData: LoginRequest = this.loginForm.value;

      // Salvar ou remover credenciais do localStorage
      if (this.loginForm.get('lembrarCredenciais')?.value) {
        localStorage.setItem('savedUsername', loginData.username);
        localStorage.setItem('savedPassword', loginData.senha);
        localStorage.setItem('savedOrgId', loginData.orgId.toString());
      } else {
        localStorage.removeItem('savedUsername');
        localStorage.removeItem('savedPassword');
        localStorage.removeItem('savedOrgId');
      }

      this.authService.login(loginData).subscribe({
        next: (response) => {
          if (!environment.production) {
            console.log('Resposta do login:', response);
          }
          if (!response.token) {
            this.errorMessage = 'Credenciais inválidas.';
          }
        },
        error: (err) => {
          this.errorMessage = 'Erro ao fazer login. Tente novamente.';
          console.error('Erro de login:', err);
        },
      });
    } else {
      this.errorMessage = 'Preencha todos os campos obrigatórios.';
    }
  }
}
