import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { DashboardService, DashboardResumo } from '../services/dashboard.service';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-dashboard-inicio',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard-inicio.component.html',
  styleUrls: ['./dashboard-inicio.component.scss'],
})
export class DashboardInicioComponent implements OnInit {
  resumo: DashboardResumo | null = null;
  loading = true;
  maxFaturamento = 1;
  maxVendidos = 1;
  maxEstoque = 1;
  readonly userName: string;
  readonly saudacao: string;

  constructor(private dashboardService: DashboardService, authService: AuthService) {
    this.userName = authService.getLoggedUser()?.username ?? '';
    const hora = new Date().getHours();
    this.saudacao = hora < 12 ? 'Bom dia' : hora < 18 ? 'Boa tarde' : 'Boa noite';
  }

  ngOnInit(): void {
    this.dashboardService.resumo().subscribe({
      next: (r) => {
        this.resumo = r;
        this.maxFaturamento = Math.max(...(r.faturamentoPorMes?.map((m) => Number(m.valor)) || [1]), 1);
        this.maxVendidos = Math.max(...(r.topProdutosVendidos?.map((p) => p.quantidade) || [1]), 1);
        this.maxEstoque = Math.max(...(r.topProdutosEstoque?.map((p) => p.quantidade) || [1]), 1);
        this.loading = false;
      },
      error: () => { this.loading = false; },
    });
  }

  barHeight(valor: number, max: number): number {
    return Math.max(8, Math.round((valor / max) * 100));
  }

  formatMes(mes: string): string {
    if (!mes || mes.length < 7) return mes;
    const [y, m] = mes.split('-');
    const meses = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
    return `${meses[parseInt(m, 10) - 1]}/${y.slice(2)}`;
  }

  /** Valor compacto para caber embaixo das barras (ex.: 31,2 mil); o valor completo fica no title. */
  valorCurto(v: number): string {
    if (Math.abs(v) >= 1_000_000) return `${(v / 1_000_000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mi`;
    if (Math.abs(v) >= 1_000) return `${(v / 1_000).toLocaleString('pt-BR', { maximumFractionDigits: 1 })} mil`;
    return v.toLocaleString('pt-BR', { maximumFractionDigits: 0 });
  }

  asNumber(v: unknown): number {
    return Number(v) || 0;
  }
}
