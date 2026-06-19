import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { AdminWebhookService, AdminCacheService, AdminLoginLogsService, AdminSubscriptionAdminService, LoginLog, AdminOrgSubscription } from '../services/admin.service';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent implements OnInit {
  isMasterAdmin = false;
  errorMessage = '';
  mensagem = '';
  mensagemErro = '';
  loading = false;
  abaAtiva: 'webhooks' | 'cache' | 'acessos' | 'assinaturas' = 'webhooks';

  webhooks: any[] = [];
  loginLogs: LoginLog[] = [];
  orgsAssinatura: AdminOrgSubscription[] = [];
  orgExpandidaId: number | null = null;
  trialDataCustom: Record<number, string> = {};
  loginLogsPage = 0;
  loginLogsTotal = 0;
  loginLogsTotalPages = 0;
  readonly loginLogsPageSize = 30;
  cacheInfo: any = null;
  cacheStats: any = null;
  cacheName = '';
  eventTypeLote = '';

  constructor(
    private authService: AuthService,
    private webhookService: AdminWebhookService,
    private cacheService: AdminCacheService,
    private loginLogsService: AdminLoginLogsService,
    private subscriptionAdminService: AdminSubscriptionAdminService
  ) {}

  ngOnInit(): void {
    this.isMasterAdmin = this.authService.isMasterAdmin();
    if (this.isMasterAdmin) {
      this.carregarWebhooks();
    } else {
      this.errorMessage = 'Você não tem permissão para acessar esta página.';
    }
  }

  trocarAba(aba: 'webhooks' | 'cache' | 'acessos' | 'assinaturas'): void {
    this.abaAtiva = aba;
    this.mensagem = '';
    this.mensagemErro = '';
    if (aba === 'webhooks') {
      this.carregarWebhooks();
    } else if (aba === 'cache') {
      this.carregarCache();
    } else if (aba === 'acessos') {
      this.carregarLoginLogs();
    } else {
      this.carregarAssinaturas();
    }
  }

  carregarLoginLogs(page = 0): void {
    this.loading = true;
    this.loginLogsPage = page;
    this.loginLogsService.listar(page, this.loginLogsPageSize).subscribe({
      next: (data) => {
        this.loginLogs = data.content ?? [];
        this.loginLogsTotal = data.totalElements ?? 0;
        this.loginLogsTotalPages = data.totalPages ?? 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar logs de login.');
      },
    });
  }

  paginaAnteriorLoginLogs(): void {
    if (this.loginLogsPage > 0) {
      this.carregarLoginLogs(this.loginLogsPage - 1);
    }
  }

  proximaPaginaLoginLogs(): void {
    if (this.loginLogsPage + 1 < this.loginLogsTotalPages) {
      this.carregarLoginLogs(this.loginLogsPage + 1);
    }
  }

  carregarWebhooks(): void {
    this.loading = true;
    this.webhookService.listar().subscribe({
      next: (data) => {
        this.webhooks = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar webhooks com falha.');
      },
    });
  }

  carregarCache(): void {
    this.loading = true;
    this.cacheService.info().subscribe({
      next: (info) => {
        this.cacheInfo = info;
        this.cacheService.stats().subscribe({
          next: (stats) => {
            this.cacheStats = stats;
            this.loading = false;
          },
          error: () => {
            this.loading = false;
            this.onError('Erro ao carregar estatísticas do cache.');
          },
        });
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar informações do cache.');
      },
    });
  }

  reprocessar(eventId: string): void {
    this.loading = true;
    this.webhookService.reprocessar(eventId).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Evento reprocessado!');
        this.carregarWebhooks();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao reprocessar evento.');
      },
    });
  }

  reprocessarLote(): void {
    this.loading = true;
    this.webhookService.reprocessarLote(this.eventTypeLote || undefined).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Lote reprocessado!');
        this.carregarWebhooks();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao reprocessar lote.');
      },
    });
  }

  limparCache(): void {
    if (!this.cacheName.trim()) {
      this.onError('Informe o nome do cache.');
      return;
    }
    if (!confirm(`Limpar cache "${this.cacheName}"?`)) return;
    this.loading = true;
    this.cacheService.limpar(this.cacheName.trim()).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(`Cache "${this.cacheName}" limpo!`);
        this.carregarCache();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao limpar cache.');
      },
    });
  }

  limparTodosCaches(): void {
    if (!confirm('Limpar TODOS os caches?')) return;
    this.loading = true;
    this.cacheService.limparTodos().subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Todos os caches foram limpos!');
        this.carregarCache();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao limpar todos os caches.');
      },
    });
  }

  carregarAssinaturas(): void {
    this.loading = true;
    this.subscriptionAdminService.overview().subscribe({
      next: (data) => {
        this.orgsAssinatura = data ?? [];
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar assinaturas.');
      },
    });
  }

  toggleOrg(orgId: number): void {
    this.orgExpandidaId = this.orgExpandidaId === orgId ? null : orgId;
  }

  statusLabel(status: string): string {
    const map: Record<string, string> = {
      ISENTO: 'Isento',
      TRIAL: 'Em trial',
      ATIVO: 'Pago ativo',
      MISTO: 'Misto',
      BLOQUEADO: 'Bloqueado',
      ORG_INATIVA: 'Org inativa',
      SEM_USUARIOS: 'Sem usuários',
    };
    return map[status] ?? status;
  }

  paymentModeLabel(modo?: string): string {
    const map: Record<string, string> = {
      RECURRING: 'Recorrente',
      PIX: 'Pix mensal',
      BOLETO: 'Boleto mensal',
    };
    return modo ? (map[modo] ?? modo) : '—';
  }

  toggleBypass(user: { userId: number; bypassSubscription?: boolean }): void {
    const novo = !user.bypassSubscription;
    this.loading = true;
    this.subscriptionAdminService.setBypass(user.userId, novo).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(novo ? 'Usuário isento de cobrança.' : 'Cobrança reativada para o usuário.');
        this.carregarAssinaturas();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao alterar isenção.');
      },
    });
  }

  estenderTrialUsuario(userId: number, days: number): void {
    this.loading = true;
    this.subscriptionAdminService.extendTrialUser(userId, days).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(`Trial estendido +${days} dias.`);
        this.carregarAssinaturas();
      },
      error: (err) => {
        this.loading = false;
        this.onError(err?.error?.message || 'Erro ao estender trial.');
      },
    });
  }

  estenderTrialOrg(orgId: number, days: number): void {
    this.loading = true;
    this.subscriptionAdminService.extendTrialOrg(orgId, days).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(`Trial da organização estendido +${days} dias.`);
        this.carregarAssinaturas();
      },
      error: (err) => {
        this.loading = false;
        this.onError(err?.error?.message || 'Erro ao estender trial da org.');
      },
    });
  }

  estenderTrialData(userId: number): void {
    const data = this.trialDataCustom[userId];
    if (!data) {
      this.onError('Informe uma data.');
      return;
    }
    const trialEnd = `${data}T23:59:59`;
    this.loading = true;
    this.subscriptionAdminService.extendTrialUserDate(userId, trialEnd).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Trial atualizado para a data informada.');
        this.carregarAssinaturas();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao definir data do trial.');
      },
    });
  }

  forcarCobrancaUsuario(userId: number): void {
    if (!confirm('Remover isenção e encerrar trial deste usuário?')) return;
    this.loading = true;
    this.subscriptionAdminService.forcePayUser(userId).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Usuário deve escolher plano para continuar.');
        this.carregarAssinaturas();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao forçar cobrança.');
      },
    });
  }

  forcarCobrancaOrg(orgId: number): void {
    if (!confirm('Forçar cobrança para TODOS os usuários desta organização?')) return;
    this.loading = true;
    this.subscriptionAdminService.forcePayOrg(orgId).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess('Cobrança forçada para a organização.');
        this.carregarAssinaturas();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao forçar cobrança da org.');
      },
    });
  }

  alternarOrgAtiva(org: AdminOrgSubscription): void {
    const ativar = !org.orgAtivo;
    const msg = ativar ? 'Ativar organização?' : 'Desativar organização?';
    if (!confirm(msg)) return;
    this.loading = true;
    const req = ativar
      ? this.subscriptionAdminService.ativarOrg(org.orgId)
      : this.subscriptionAdminService.desativarOrg(org.orgId);
    req.subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(ativar ? 'Organização ativada.' : 'Organização desativada.');
        this.carregarAssinaturas();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao alterar status da organização.');
      },
    });
  }

  private onSuccess(msg: string): void {
    this.mensagem = msg;
    setTimeout(() => (this.mensagem = ''), 3000);
  }

  private onError(msg: string): void {
    this.mensagemErro = msg;
    setTimeout(() => (this.mensagemErro = ''), 3000);
  }
}
