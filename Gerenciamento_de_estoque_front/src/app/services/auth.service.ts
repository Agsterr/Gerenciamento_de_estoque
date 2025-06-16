import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, of, tap, catchError } from 'rxjs';
import { ApiService } from './api.service';
import { LoginRequest } from '../models/login-request.model';
import { LoginResponse } from '../models/login-response.model';
import { jwtDecode } from 'jwt-decode';  // Importando jwt-decode corretamente

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  constructor(private apiService: ApiService, private router: Router) {}

  /**
   * Realiza o login e armazena os dados recebidos (token, usuário e roles).
   */
  login(data: LoginRequest): Observable<LoginResponse> {
    return this.apiService.post<LoginResponse>('/auth/login', data).pipe(
      tap((response: LoginResponse) => {
        if (response && response.token) {
          // Armazena o token JWT
          localStorage.setItem('jwtToken', response.token);

          // Decodificar o token JWT
          const decodedToken: any = jwtDecode(response.token);
          
          // Adicionando log para verificar o conteúdo do decodedToken
          console.log('Token Decodificado:', decodedToken);  // Verifique se as roles estão presentes no decodedToken
          
          const userInfo = {
            username: decodedToken.sub,  // 'sub' é o nome do usuário no JWT
            userId: decodedToken.user_id,  // user_id extraído do JWT
            orgId: decodedToken.org_id,  // org_id extraído do JWT
            roles: decodedToken.roles || []  // Certifique-se de que as roles sejam extraídas e armazenadas
          };

          // Logando as informações do usuário
          console.log('Informações do Usuário:', userInfo); // Verifique as informações armazenadas

          // Armazenando o usuário no localStorage
          localStorage.setItem('loggedUser', JSON.stringify(userInfo));

          // Redireciona para o dashboard
          this.router.navigate(['/dashboard']);
        }
      }),
      catchError((err) => {
        console.error('Erro ao fazer login:', err);
        window.alert('Erro ao tentar fazer login. Verifique suas credenciais.');
        this.router.navigate(['/login']);
        return of({ token: '', roles: [] });  // resposta vazia válida
      })
    );
  }

  /**
   * Realiza o registro de um novo usuário.
   */
  register(data: any): Observable<any> {
  return this.apiService.postWithAuth('/auth/register', data);
}


  /**
   * Retorna os dados do usuário logado a partir do localStorage.
   */
  getLoggedUser(): any {
    const userData = localStorage.getItem('loggedUser');
    // Adicionando log para verificar o que está sendo recuperado do localStorage
    console.log('Dados do Usuário Recuperados do localStorage:', userData);

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
    const loggedIn = !!localStorage.getItem('jwtToken');
    // Log para verificar se o usuário está autenticado
    console.log('Usuário Logado:', loggedIn);

    return loggedIn;
  }
}
