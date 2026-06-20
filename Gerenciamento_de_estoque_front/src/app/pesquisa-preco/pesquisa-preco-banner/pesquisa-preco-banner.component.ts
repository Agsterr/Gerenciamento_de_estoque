import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { PesquisaPrecoService } from '../../services/pesquisa-preco.service';

const SESSION_DISMISS_KEY = 'pesquisaPrecoBannerDismissed';

@Component({
  selector: 'app-pesquisa-preco-banner',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './pesquisa-preco-banner.component.html',
  styleUrl: './pesquisa-preco-banner.component.scss',
})
export class PesquisaPrecoBannerComponent implements OnInit, OnDestroy {
  showBanner = false;
  private sub?: Subscription;

  constructor(
    private pesquisaPrecoService: PesquisaPrecoService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (this.authService.isMasterAdmin() || sessionStorage.getItem(SESSION_DISMISS_KEY) === 'true') {
      return;
    }

    this.sub = this.pesquisaPrecoService.hasResponded$.subscribe((responded) => {
      if (responded === true) {
        this.showBanner = false;
      } else if (responded === false) {
        this.showBanner = true;
      }
    });

    this.pesquisaPrecoService.refreshStatus();
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  dismiss(): void {
    sessionStorage.setItem(SESSION_DISMISS_KEY, 'true');
    this.showBanner = false;
  }
}
