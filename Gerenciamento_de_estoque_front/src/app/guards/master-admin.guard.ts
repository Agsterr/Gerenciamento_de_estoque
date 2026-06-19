import { Injectable } from '@angular/core';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Injectable({ providedIn: 'root' })
export class MasterAdminGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) {}

  canActivate(): boolean | UrlTree {
    const user = this.authService.getLoggedUser();

    if (!user) {
      return this.router.createUrlTree(['/login']);
    }

    if (!this.authService.isMasterAdmin()) {
      return this.router.createUrlTree(['/home'], { queryParams: { notice: 'master_admin_required' } });
    }

    return true;
  }
}
