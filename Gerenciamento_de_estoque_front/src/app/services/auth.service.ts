
import { Injectable } from '@angular/core';
import { ApiService } from './api.service'; // Serviço genérico para comunicação com a API
import { Observable } from 'rxjs';
import { LoginResponse } from '../models/login-response.model'; // Importe a interface LoginResponse
import { RegisterRequest } from '../models/register-request.model'; // Importe a interface RegisterRequest
import { RegisterResponse } from '../models/register-response.model'; // Importe a interface RegisterResponse

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
    return this.apiService.post<LoginResponse>('/auth/login', loginData); // Requisição POST para /auth/login
  }

  /**
   * Realiza logout do usuário (se necessário, apenas limpa o token local).
   */
  logout(): void {
    localStorage.removeItem('jwtToken'); // Remove o token JWT do armazenamento local
  }

  /**
   * Registro de um novo usuário.
   * @param data - Dados para registro.
   * @returns Observable com a resposta da API.
   */
  register(data: RegisterRequest): Observable<RegisterResponse> {
    return this.apiService.post<RegisterResponse>('/auth/register', data); // Requisição POST para /auth/register
  }
}
