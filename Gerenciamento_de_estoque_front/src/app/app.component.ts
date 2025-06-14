import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatMenuModule,
    MatButtonModule,
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
})
export class AppComponent {
  title = 'Gerenciamento De Estoque';
  menuAberto = false;

  constructor(public authService: AuthService) {}

  // Verifica se o usuário está logado
  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  // Obtém o nome do usuário logado, com fallback
  get userName(): string {
    const user = this.authService.getLoggedUser();
    return user?.username || 'Usuário';
  }

  // Faz logout
  logout(): void {
    this.authService.logout();
  }
}
