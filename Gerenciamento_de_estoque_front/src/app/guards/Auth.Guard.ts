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
    const loggedUser = this.authService.getLoggedUser();

    // Se não há usuário logado ou estrutura inválida, redireciona para login
    if (!loggedUser || !Array.isArray(loggedUser.roles)) {
      this.router.navigate(['/login']);
      return false;
    }

    // Verifica se o usuário possui a role de administrador
    const possuiPermissaoAdmin = loggedUser.roles.some(
      (role: { nome: string }) => role?.nome === 'ROLE_ADMIN'
    );

    if (possuiPermissaoAdmin) {
      return true;
    }

    // Usuário logado, mas sem permissão
    alert('Você não tem permissão para acessar esta página. Somente administradores podem acessar.');
    this.router.navigate(['/dashboard']);
    return false;
  }
}
