import { Injectable } from '@angular/core';
import {
  CanActivate,
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  Router
} from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    // Permite acesso livre à rota de login
    if (route.routeConfig && route.routeConfig.path === 'login') {
      return true;
    }


    // Se não está logado, redireciona imediatamente para login
    const token = localStorage.getItem('jwtToken');
    if (!this.authService.isLoggedIn() || !token) {
      this.authService.logout();
      return false;
    }

    // Verifica validade do token JWT
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      if (payload.exp && Date.now() >= payload.exp * 1000) {
        // Token expirado
        this.authService.logout();
        return false;
      }
    } catch (e) {
      // Token inválido
      this.authService.logout();
      return false;
    }

    const loggedUser = this.authService.getLoggedUser();

    // Se não há usuário logado ou estrutura inválida, redireciona para login
    if (!loggedUser || !Array.isArray(loggedUser.roles)) {
      this.router.navigate(['/login']);
      return false;
    }


    // Verifica se o usuário possui a role de administrador
    // Suporta tanto array de string quanto array de objetos
    const possuiPermissaoAdmin = Array.isArray(loggedUser.roles) && loggedUser.roles.some((role: any) => {
      if (typeof role === 'string') {
        return role === 'ROLE_ADMIN';
      }
      if (role && typeof role === 'object' && 'nome' in role) {
        return role.nome === 'ROLE_ADMIN';
      }
      return false;
    });

    if (possuiPermissaoAdmin) {
      return true;
    }

    // Usuário logado, mas sem permissão
    alert('Você não tem permissão para acessar esta página. Somente administradores podem acessar.');
    this.router.navigate(['/dashboard']);
    return false;
  }
}
