
// src/app/guards/auth.guard.ts
import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs';
import { AuthService } from '../services/auth.service';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router, private authService: AuthService) {}

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    const loggedUser = this.authService.getLoggedUser(); // Pega os dados do usuário logado

    if (loggedUser) {
      // Verificando se o usuário tem a role 'ROLE_ADMIN'
      const isAdmin = loggedUser.roles.some((role: { nome: string }) => role.nome === 'ROLE_ADMIN');
      if (isAdmin) {
        return true; // Permite o acesso
      } else {
        // Exibe um alert para o usuário informando que ele não tem permissão
        alert('Você não tem permissão para acessar esta página. Somente administradores podem acessar.');

        // Redireciona para o dashboard caso não seja admin
        this.router.navigate(['/dashboard']);
        return false;
      }
    } else {
      this.router.navigate(['/login']); // Redireciona para login se não estiver autenticado
      return false;
    }
  }
}
