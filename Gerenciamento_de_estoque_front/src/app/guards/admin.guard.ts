import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean {
    const user = this.authService.getLoggedUser();

    if (!environment.production) {
      // Logs somente em desenvolvimento para depuração
      console.log('Dados do Usuário Recuperados:', user);
    }

    if (!user || !user.roles) {
      if (!environment.production) {
        console.log('Usuário não encontrado ou roles não definidas');
      }
      this.router.navigate(['/login']);
      return false;
    }

    const isAdmin = user.roles.includes('ROLE_ADMIN');

    if (!environment.production) {
      console.log('Usuário tem a role ROLE_ADMIN?', isAdmin);
    }

    if (!isAdmin) {
      this.router.navigate(['/home']);
      return false;
    }

    return true;
  }
}
