import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AdminGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    const user = this.authService.getLoggedUser(); // Recupera os dados do usuário

    console.log('Dados do Usuário Recuperados:', user); // Verifique o que está sendo retornado pelo getLoggedUser()

    if (!user || !user.roles || !Array.isArray(user.roles)) {
      console.log('Usuário não encontrado ou roles não definidas');
      this.router.navigate(['/login']);
      return false; // Redireciona para login se o usuário não estiver autenticado ou não tiver roles
    }

    // Verificando se a role ROLE_ADMIN está presente no array de roles
    const isAdmin = user.roles.includes('ROLE_ADMIN'); // Verifica se a role é 'ROLE_ADMIN'

    console.log('Usuário tem a role ROLE_ADMIN?', isAdmin); // Verifica se a role 'ROLE_ADMIN' está presente

    if (!isAdmin) {
      alert('Você não tem permissão para acessar esta página. Somente administradores podem acessar.');
      this.router.navigate(['/dashboard']);
      return false; // Redireciona para o dashboard se o usuário não for admin
    }

    // Permite o acesso se for admin
    return true;
  }
}
