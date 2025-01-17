

import { Injectable } from '@angular/core';
import { ApiService } from './api.service';
import { Router } from '@angular/router'; // Importando o Router
import { Observable, tap, catchError } from 'rxjs';
import { LoginResponse } from '../models/login-response.model'; // Importando a interface LoginResponse
import { of } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(
    private apiService: ApiService,
    private router: Router  // Injetando o Router
  ) {}

  login(username: string, senha: string): Observable<LoginResponse> {
    const loginData = { username, senha };

    return this.apiService.post<LoginResponse>('/auth/login', loginData).pipe(
      tap((response: LoginResponse) => {
        if (response && response.token) {
          // Armazenando o token JWT no localStorage
          localStorage.setItem('jwtToken', response.token);
          localStorage.setItem('loggedUser', JSON.stringify({ username }));

          // Redirecionando para o Dashboard
          this.router.navigate(['/dashboard']); // Redirecionando para o Dashboard
        }
      }),
      catchError((err) => {
        console.error('Erro ao fazer login', err);
        return of(err); // Retorna um Observable vazio em caso de erro
      })
    );
  }

  getLoggedUser(): any {
    const userData = localStorage.getItem('loggedUser');
    return userData ? JSON.parse(userData) : null;
  }

  logout(): void {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('loggedUser');
    this.router.navigate(['/login']);  // Redireciona para a página de login
  }

  register(data: any): Observable<any> {
    return this.apiService.post('/auth/register', data);
  }
}
