import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, Subscription } from 'rxjs';

export interface DashboardNavItem {
  path: string;
  icon: string;
  label: string;
  external?: boolean;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
  imports: [RouterModule, CommonModule],
})
export class DashboardComponent implements OnInit, OnDestroy {
  isMobile = false;
  isHubRoute = true;
  moreMenuOpen = false;
  pageTitle = 'Início';

  readonly primaryMobile: DashboardNavItem[] = [
    { path: 'inicio', icon: 'fa-home', label: 'Início' },
    { path: 'produtos', icon: 'fa-box', label: 'Produtos' },
    { path: 'consumidores', icon: 'fa-users', label: 'Clientes' },
    { path: 'pedidos-venda', icon: 'fa-file-invoice-dollar', label: 'Vendas' },
  ];

  readonly secondaryMenu: DashboardNavItem[] = [
    { path: 'categorias', icon: 'fa-list-alt', label: 'Categorias' },
    { path: 'movimentacoes', icon: 'fa-exchange-alt', label: 'Movimentações' },
    { path: 'relatorios', icon: 'fa-chart-bar', label: 'Relatórios' },
    { path: 'fornecedores', icon: 'fa-truck-loading', label: 'Fornecedores' },
    { path: 'depositos', icon: 'fa-warehouse', label: 'Depósitos' },
    { path: 'pedidos-compra', icon: 'fa-shopping-cart', label: 'Compras' },
    { path: 'inventario', icon: 'fa-clipboard-check', label: 'Inventário' },
    { path: 'auditoria', icon: 'fa-history', label: 'Auditoria' },
    { path: 'usuarios', icon: 'fa-user-cog', label: 'Usuários', adminOnly: true },
    { path: '/assinatura', icon: 'fa-credit-card', label: 'Assinatura', external: true },
    { path: 'ajuda', icon: 'fa-life-ring', label: 'Ajuda' },
  ];

  readonly desktopMenu: DashboardNavItem[] = [
    { path: 'inicio', icon: 'fa-chart-pie', label: 'Início' },
    ...this.primaryMobile.filter((i) => i.path !== 'inicio'),
    ...this.secondaryMenu,
  ];

  private readonly titleByPath: Record<string, string> = {
    inicio: 'Início',
    produtos: 'Produtos',
    consumidores: 'Clientes',
    'pedidos-venda': 'Vendas',
    categorias: 'Categorias',
    movimentacoes: 'Movimentações',
    relatorios: 'Relatórios',
    fornecedores: 'Fornecedores',
    depositos: 'Depósitos',
    'pedidos-compra': 'Compras',
    inventario: 'Inventário',
    auditoria: 'Auditoria',
    usuarios: 'Usuários',
    ajuda: 'Ajuda',
  };

  private routerSub?: Subscription;

  constructor(private router: Router) {}

  ngOnInit(): void {
    this.onResize();
    this.updateLayoutFlags(this.router.url);
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe((e) => {
        this.updateLayoutFlags(e.urlAfterRedirects);
        this.moreMenuOpen = false;
      });
  }

  ngOnDestroy(): void {
    this.routerSub?.unsubscribe();
  }

  @HostListener('window:resize')
  onResize(): void {
    this.isMobile = window.matchMedia('(max-width: 768px)').matches;
  }

  toggleMoreMenu(): void {
    this.moreMenuOpen = !this.moreMenuOpen;
  }

  closeMoreMenu(): void {
    this.moreMenuOpen = false;
  }

  navLink(item: DashboardNavItem): string {
    return item.external ? item.path : `/dashboard/${item.path}`;
  }

  private updateLayoutFlags(url: string): void {
    this.isMobile = window.matchMedia('(max-width: 768px)').matches;
    const segment = this.extractSegment(url);
    this.isHubRoute = !segment || segment === 'inicio';
    this.pageTitle = this.titleByPath[segment] ?? 'Painel';
  }

  private extractSegment(url: string): string {
    const match = url.match(/\/dashboard\/?([^/?#]+)?/);
    return match?.[1] ?? '';
  }
}
