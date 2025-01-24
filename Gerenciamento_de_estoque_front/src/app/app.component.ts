
// Componente Principal: AppComponent
import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html', // Template separado
  styleUrls: ['./app.component.scss'], // Estilo separado
})
export class AppComponent {
  title = 'Gerenciamento De Estoque'; // Título do aplicativo
}

