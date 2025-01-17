

import { Injectable } from '@angular/core';
import { ApiService } from './api.service'; // Serviço genérico para comunicação com a API
import { Observable, tap, catchError } from 'rxjs';
import { LoginResponse } from '../models/login-response.model'; // Importe a interface LoginResponse
import { of } from 'rxjs';  // Para retornar um Observable vazio em caso de erro

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private apiService: ApiService) {}

  /**
   * Realiza o login do usuário.
   * @param username - Nome de usuário do cliente
   * @param senha - Senha do cliente
   * @returns Observable com a resposta da API (contendo o token JWT ou erro)
   */
  login(username: string, senha: string): Observable<LoginResponse> {
    const loginData = { username, senha };

    return this.apiService.post<LoginResponse>('/auth/login', loginData).pipe(
      tap((response: LoginResponse) => {
        if (response && response.token) {
          // Armazena o JWT no localStorage
          localStorage.setItem('jwtToken', response.token);  // Armazena o JWT no localStorage
          localStorage.setItem('loggedUser', JSON.stringify({ username }));
        }
      }),
      catchError((err) => {
        console.error('Erro ao fazer login', err); // Logando o erro no console
        // Aqui você pode lançar um erro específico ou retornar uma resposta amigável
        return of(err); // Retorna um Observable vazio para que o fluxo continue
      })
    );
  }

  /**
   * Retorna os dados do usuário logado a partir do localStorage.
   * @returns objeto com os dados do usuário ou null se não estiver logado.
   */
  getLoggedUser(): any {
    const userData = localStorage.getItem('loggedUser');
    return userData ? JSON.parse(userData) : null;
  }

  /**
   * Realiza logout do usuário (limpa o token e os dados do usuário).
   */
  logout(): void {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('loggedUser');
  }

  /**
   * Registro de um novo usuário.
   * @param data - Dados para registro.
   * @returns Observable com a resposta da API.
   */
  register(data: any): Observable<any> {
    return this.apiService.post('/auth/register', data);
  }
}
