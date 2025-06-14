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

  // Getter para o estado do login (evita chamar método diretamente no template)
  get isLoggedIn(): boolean {
    return this.authService.isLoggedIn();
  }

  // Getter para o nome do usuário
  get userName(): string {
    const user = this.authService.getLoggedUser();
    // Certifique-se de que o objeto retornado está correto
    return user?.username || 'Usuário';
  }

  logout(): void {
    this.authService.logout();
  }
}
