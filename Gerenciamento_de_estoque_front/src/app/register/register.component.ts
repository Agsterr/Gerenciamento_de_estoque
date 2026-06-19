import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { environment } from '../../environments/environment';
import { trigger, transition, style, animate } from '@angular/animations';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, MatIconModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'],
  animations: [
    trigger('slideInUp', [
      transition(':enter', [
        style({ transform: 'translateY(50px)', opacity: 0 }),
        animate('600ms cubic-bezier(0.35, 0, 0.25, 1)', style({ transform: 'translateY(0)', opacity: 1 }))
      ])
    ]),
    trigger('slideInLeft', [
      transition(':enter', [
        style({ transform: 'translateX(-30px)', opacity: 0 }),
        animate('400ms 200ms cubic-bezier(0.35, 0, 0.25, 1)', style({ transform: 'translateX(0)', opacity: 1 }))
      ])
    ]),
    trigger('slideInRight', [
      transition(':enter', [
        style({ transform: 'translateX(30px)', opacity: 0 }),
        animate('400ms 300ms cubic-bezier(0.35, 0, 0.25, 1)', style({ transform: 'translateX(0)', opacity: 1 }))
      ])
    ]),
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: 0, transform: 'scale(0.95)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'scale(1)' }))
      ]),
      transition(':leave', [
        animate('200ms ease-in', style({ opacity: 0, transform: 'scale(0.95)' }))
      ])
    ])
  ]
})
export class RegisterComponent implements OnInit {
  registerForm: FormGroup;
  successMessage: string = '';
  errorMessage: string = '';
  showPassword: boolean = false;
  isSubmitting: boolean = false;
  focusedField: string = '';
  passwordStrength = {
    percentage: 0,
    text: '',
    class: ''
  };

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {
    this.registerForm = this.fb.group({
      username: ['', Validators.required],
      senha: ['', [Validators.required, Validators.minLength(6)]],
      email: ['', [Validators.required, Validators.email]],
      orgNome: ['', [Validators.required, Validators.minLength(2)]],
    });
  }

  ngOnInit(): void {
    this.authService.markVisited();
    if (this.authService.isTokenValid()) {
      this.router.navigate(['/dashboard'], { replaceUrl: true });
      return;
    }

    const errorMessage = localStorage.getItem('authErrorMessage');
    if (errorMessage) {
      this.errorMessage = errorMessage;
      localStorage.removeItem('authErrorMessage');
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onFieldFocus(fieldName: string): void {
    this.focusedField = fieldName;
  }

  onFieldBlur(fieldName: string): void {
    if (this.focusedField === fieldName) {
      this.focusedField = '';
    }
  }

  onPasswordChange(): void {
    const password = this.registerForm.get('senha')?.value || '';
    this.passwordStrength = this.calculatePasswordStrength(password);
  }

  calculatePasswordStrength(password: string): any {
    let score = 0;
    let text = '';
    let className = '';

    if (password.length >= 6) score += 20;
    if (password.length >= 8) score += 20;
    if (/[a-z]/.test(password)) score += 20;
    if (/[A-Z]/.test(password)) score += 20;
    if (/[0-9]/.test(password)) score += 10;
    if (/[^A-Za-z0-9]/.test(password)) score += 10;

    if (score < 40) {
      text = 'Fraca';
      className = 'weak';
    } else if (score < 70) {
      text = 'Média';
      className = 'medium';
    } else {
      text = 'Forte';
      className = 'strong';
    }

    return {
      percentage: Math.min(score, 100),
      text,
      class: className
    };
  }

  getFormProgress(): number {
    const fields = ['username', 'senha', 'email', 'orgNome'];
    let filledFields = 0;

    fields.forEach(field => {
      const control = this.registerForm.get(field);
      if (control && control.value && control.valid) {
        filledFields++;
      }
    });

    return Math.round((filledFields / fields.length) * 100);
  }

  onSubmit(): void {
    if (this.registerForm.valid && !this.isSubmitting) {
      const { username, senha, email, orgNome } = this.registerForm.value;

      this.isSubmitting = true;
      this.errorMessage = '';

      const payload = {
        username: String(username).trim(),
        senha,
        email: String(email).trim(),
        orgNome: String(orgNome).trim(),
      };

      this.authService.register(payload).subscribe({
        next: (response) => {
          if (!environment.production) {
            console.log('Registro bem-sucedido:', response);
          }
          this.successMessage = 'Conta criada com sucesso! Redirecionando...';
          this.snackBar.open(this.successMessage, 'Fechar', {
            duration: 3000,
            panelClass: ['success-snackbar'],
          });
          this.errorMessage = '';

          setTimeout(() => {
            this.router.navigate(['/login']);
          }, 2000);
        },
        error: (err: HttpErrorResponse) => {
          console.error('Erro ao registrar:', err);
          const apiMessage = err.error?.error || err.error?.message;
          if (err.status === 400 || err.status === 409) {
            this.errorMessage = apiMessage || 'Dados inválidos. Verifique os campos e tente novamente.';
          } else {
            this.errorMessage = apiMessage || 'Erro ao criar conta. Verifique os dados e tente novamente.';
          }
          this.snackBar.open(this.errorMessage, 'Fechar', {
            duration: 4000,
            panelClass: ['error-snackbar'],
          });
          this.successMessage = '';
          this.isSubmitting = false;
        },
      });
    } else if (!this.registerForm.valid) {
      this.errorMessage = 'Por favor, preencha todos os campos corretamente.';
      this.snackBar.open(this.errorMessage, 'Fechar', {
        duration: 3000,
        panelClass: ['error-snackbar'],
      });

      Object.keys(this.registerForm.controls).forEach(key => {
        this.registerForm.get(key)?.markAsTouched();
      });
    }
  }

  cancel(): void {
    if (!this.isSubmitting) {
      this.registerForm.reset();
      this.successMessage = '';
      this.errorMessage = '';
      this.showPassword = false;
      this.focusedField = '';
      this.passwordStrength = { percentage: 0, text: '', class: '' };
      this.router.navigate(['/login']);
    }
  }
}
