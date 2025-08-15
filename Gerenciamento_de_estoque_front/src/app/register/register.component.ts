import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { RoleService } from '../services/role.service';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, MatIconModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  roles: any[] = [];
  successMessage: string = '';
  errorMessage: string = '';
  showPassword: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private roleService: RoleService,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.fb.group({
      username: ['', Validators.required],
      senha: ['', [Validators.required, Validators.minLength(6)]],
      email: ['', [Validators.required, Validators.email]],
      orgId: ['', Validators.required],
      roles: [[], Validators.required],
    });
  }

  ngOnInit(): void {
    // Verifica se há uma mensagem de erro armazenada no localStorage
    const errorMessage = localStorage.getItem('authErrorMessage');
    if (errorMessage) {
      this.errorMessage = errorMessage;
      localStorage.removeItem('authErrorMessage');
    }

    // Carregar as roles do backend
    this.roleService.listarRoles().subscribe({
      next: (roles: any[]) => {
        this.roles = roles;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Erro ao carregar roles:', err);
        this.errorMessage = 'Erro ao carregar roles.';
        this.snackBar.open(this.errorMessage, 'Fechar', {
          duration: 3000,
          panelClass: ['error-snackbar'],
        });
      }
    });
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      const { username, senha, email, orgId, roles } = this.registerForm.value;

      // Verifica se pelo menos uma role foi selecionada
      if (roles.length === 0) {
        this.errorMessage = 'Você precisa selecionar pelo menos uma role.';
        this.snackBar.open(this.errorMessage, 'Fechar', {
          duration: 3000,
          panelClass: ['error-snackbar'],
        });
        return;
      }

      // Chama o serviço de autenticação para registrar o usuário
      this.authService
        .register({ username, senha, email, orgId, roles })
        .subscribe({
          next: (response) => {
            if (!environment.production) {
              console.log('Registro bem-sucedido:', response);
            }
            this.successMessage = 'Usuário registrado com sucesso!';
            this.snackBar.open(this.successMessage, 'Fechar', {
              duration: 3000,
              panelClass: ['success-snackbar'],
            });
            this.errorMessage = '';
            this.router.navigate(['/login']);
          },
          error: (err: HttpErrorResponse) => {
            console.error('Erro ao registrar:', err);
            this.errorMessage = 'Erro ao registrar. Verifique os dados.';
            this.snackBar.open(this.errorMessage, 'Fechar', {
              duration: 3000,
              panelClass: ['error-snackbar'],
            });
            this.successMessage = '';
          },
        });
    } else {
      this.errorMessage = 'Por favor, preencha todos os campos corretamente.';
      this.snackBar.open(this.errorMessage, 'Fechar', {
        duration: 3000,
        panelClass: ['error-snackbar'],
      });
    }
  }

  cancel(): void {
    this.registerForm.reset();
    this.successMessage = '';
    this.errorMessage = '';
    this.showPassword = false;
  }
}
