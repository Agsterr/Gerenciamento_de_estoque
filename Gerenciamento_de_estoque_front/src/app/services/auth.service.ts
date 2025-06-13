// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError } from 'rxjs';
import { ApiService } from './api.service';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse, Role } from '../models/login-response.model';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(
    private apiService: ApiService,
    private router: Router
  ) {}

  /**
   * Realiza o login e armazena os dados recebidos (token, usuário e roles).
   */
  login(data: LoginRequest): Observable<LoginResponse> {
    return this.apiService.post<LoginResponse>('/auth/login', data).pipe(
      tap((response: LoginResponse) => {
        if (response && response.token) {
          // Armazena o token JWT
          localStorage.setItem('jwtToken', response.token);

          // Garante que roles é array antes de mapear
          const rolesArray = Array.isArray(response.roles) ? response.roles : [];

          // Armazena dados do usuário logado, incluindo roles e username
          const userInfo = {
            username: response.username || '',
            userId: response.userId || null,
            roles: rolesArray.map((role: Role) => ({
              id: role.id,
              nome: role.nome,
              orgId: role.org.id,
              orgNome: role.org.nome,
              orgAtivo: role.org.ativo
            }))
          };

          localStorage.setItem('loggedUser', JSON.stringify(userInfo));

          // Redireciona para o dashboard
          this.router.navigate(['/dashboard']);
        }
      }),
      catchError((err) => {
        console.error('Erro ao fazer login:', err);
        window.alert('Erro ao tentar fazer login. Verifique suas credenciais.');
        this.router.navigate(['/login']);
        return of({ token: '', roles: [] }); // resposta vazia válida
      })
    );
  }

  /**
   * Retorna os dados do usuário logado a partir do localStorage.
   */
  getLoggedUser(): any {
    const userData = localStorage.getItem('loggedUser');
    return userData ? JSON.parse(userData) : null;
  }

  /**
   * Remove os dados de autenticação e redireciona para a tela de login.
   */
  logout(): void {
    localStorage.removeItem('jwtToken');
    localStorage.removeItem('loggedUser');
    this.router.navigate(['/login']);
  }

  /**
   * Verifica se o usuário está autenticado.
   */
  isLoggedIn(): boolean {
    return !!localStorage.getItem('jwtToken');
  }

  /**
   * Registra um novo usuário.
   */
  register(data: any): Observable<any> {
    return this.apiService.post('/auth/register', data);
  }

  /**
   * Retorna a organização ativa do usuário logado, se houver.
   */
  getActiveOrg(): { id: number; nome: string } | null {
    const user = this.getLoggedUser();

    if (user && Array.isArray(user.roles)) {
      const activeRole = user.roles.find((role: any) => role.orgAtivo === true);

      if (activeRole) {
        return {
          id: activeRole.orgId,
          nome: activeRole.orgNome
        };
      }
    }

    return null;
  }
}
