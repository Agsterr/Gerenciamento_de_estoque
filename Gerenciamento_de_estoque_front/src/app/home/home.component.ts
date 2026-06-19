import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { RouterModule } from '@angular/router';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-home',
  standalone: true,
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss'],
  imports: [CommonModule, RouterModule, MatSnackBarModule],
})
export class HomeComponent implements OnInit {
  title = 'Bem-vindo à Página Inicial!';

  constructor(
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private authService: AuthService,
    private router: Router
  ) {
    this.route.queryParamMap.subscribe((params) => {
      const notice = params.get('notice');
      if (notice === 'admin_required') {
        this.snackBar.open('Acesso restrito. Somente administradores podem acessar essa página.', 'OK', {
          duration: 4000,
          panelClass: ['snackbar-warning']
        });
      }
    });
  }

  ngOnInit(): void {
    const target = this.authService.resolveEntryRoute();
    this.authService.markVisited();
    if (target !== '/home') {
      this.router.navigateByUrl(target, { replaceUrl: true });
    }
  }
}
