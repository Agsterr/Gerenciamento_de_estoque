import { Injectable } from '@angular/core';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from '@angular/router';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root',
})
export class AuthGuard implements CanActivate {
  constructor(private router: Router) {}

  canActivate(
    next: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    // Verifique se o usuário está autenticado (exemplo: se o token está presente no localStorage)
    if (localStorage.getItem('auth_token')) {
      return true;  // Permite acesso ao dashboard
    } else {
      this.router.navigate(['/login']);  // Redireciona para o login caso não esteja autenticado
      return false;
    }
  }
}
