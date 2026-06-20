import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { AdminWebhookService, AdminCacheService, AdminLoginLogsService, AdminSubscriptionAdminService, AdminUsersService, AdminPesquisaPrecoService, LoginLog, LoginLogPeriodo, LoginLogExportFile, AdminOrgSubscription, AdminUser, AdminOrgSummary, PesquisaPrecoStats } from '../services/admin.service';
import { AdminSugestaoService, Sugestao } from '../services/login-logs.service';

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
  abaAtiva: 'webhooks' | 'cache' | 'acessos' | 'assinaturas' | 'sugestoes' | 'usuarios' | 'pesquisa-preco' = 'webhooks';

  webhooks: any[] = [];
  loginLogs: LoginLog[] = [];
  orgsAssinatura: AdminOrgSubscription[] = [];
  orgExpandidaId: number | null = null;
  trialDataCustom: Record<number, string> = {};
  loginLogsPage = 0;
  loginLogsTotal = 0;
  loginLogsTotalPages = 0;
  readonly loginLogsPageSize = 30;
  loginLogPeriodos: LoginLogPeriodo[] = [];
  loginLogAnos: number[] = [];
  loginLogMeses: number[] = [];
  loginLogDias: number[] = [];
  filtroLoginAno?: number;
  filtroLoginMes?: number;
  filtroLoginDia?: number;
  filtroLoginOrgId?: number;
  loginLogArquivos: LoginLogExportFile[] = [];
  readonly mesesNomes = ['', 'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];
  sugestoes: Sugestao[] = [];
  sugestoesPage = 0;
  sugestoesTotalPages = 0;
  adminUsers: AdminUser[] = [];
  adminOrgs: AdminOrgSummary[] = [];
  novoUsuario = { username: '', email: '', orgId: 0 as number, roles: 'ROLE_USER', bypassSubscription: true };
  senhaTemporaria = '';
  orgLimites: Record<number, number> = {};
  pesquisaStats: PesquisaPrecoStats | null = null;
  cacheInfo: any = null;
  cacheStats: any = null;
  cacheName = '';
  eventTypeLote = '';

  get cacheNomes(): string[] {
    const names = this.cacheStats?.cacheNames;
    return Array.isArray(names) ? names : [];
  }

  constructor(
    private authService: AuthService,
    private webhookService: AdminWebhookService,
    private cacheService: AdminCacheService,
    private loginLogsService: AdminLoginLogsService,
    private subscriptionAdminService: AdminSubscriptionAdminService,
    private sugestaoAdminService: AdminSugestaoService,
    private adminUsersService: AdminUsersService,
    private adminPesquisaPrecoService: AdminPesquisaPrecoService
  ) {}

  ngOnInit(): void {
    this.isMasterAdmin = this.authService.isMasterAdmin();
    if (this.isMasterAdmin) {
      this.carregarWebhooks();
    } else {
      this.errorMessage = 'Você não tem permissão para acessar esta página.';
    }
  }

  trocarAba(aba: 'webhooks' | 'cache' | 'acessos' | 'assinaturas' | 'sugestoes' | 'usuarios' | 'pesquisa-preco'): void {
    this.abaAtiva = aba;
    this.mensagem = '';
    this.mensagemErro = '';
    if (aba === 'webhooks') {
      this.carregarWebhooks();
    } else if (aba === 'cache') {
      this.carregarCache();
    } else if (aba === 'acessos') {
      this.carregarPeriodosLoginLogs();
      this.carregarLoginLogs();
      this.carregarArquivosLoginLogs();
    } else if (aba === 'sugestoes') {
      this.carregarSugestoes();
    } else if (aba === 'usuarios') {
      this.carregarAdminUsuarios();
    } else if (aba === 'pesquisa-preco') {
      this.carregarPesquisaPreco();
    } else {
      this.carregarAssinaturas();
    }
  }

  carregarPesquisaPreco(): void {
    this.loading = true;
    this.adminPesquisaPrecoService.stats().subscribe({
      next: (data) => {
        this.pesquisaStats = data;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar pesquisa de preço.');
      },
    });
  }

  resetSenhaUsuario(user: AdminUser): void {
    if (!confirm(`Gerar nova senha temporária para ${user.username}?`)) return;
    this.loading = true;
    this.adminUsersService.resetSenha(user.id).subscribe({
      next: (res) => {
        this.loading = false;
        this.senhaTemporaria = res.temporaryPassword;
        this.onSuccess(`Nova senha para ${user.username}: ${res.temporaryPassword}`);
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao resetar senha.');
      },
    });
  }

  toggleBypassUsuario(user: AdminUser): void {
    const novo = !user.bypassSubscription;
    this.loading = true;
    this.adminUsersService.setBypass(user.id, novo).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(novo ? 'Usuário isento de assinatura.' : 'Isenção removida.');
        this.carregarAdminUsuarios();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao alterar isenção.');
      },
    });
  }

  carregarAdminUsuarios(): void {
    this.loading = true;
    this.senhaTemporaria = '';
    this.adminUsersService.listarOrgs().subscribe({
      next: (orgs) => {
        this.adminOrgs = orgs ?? [];
        for (const o of this.adminOrgs) {
          this.orgLimites[o.orgId] = o.maxDispositivos;
        }
        if (this.adminOrgs.length && !this.novoUsuario.orgId) {
          this.novoUsuario.orgId = this.adminOrgs.find(o => !o.ephemeral)?.orgId ?? this.adminOrgs[0].orgId;
        }
        this.adminUsersService.listar(0, 50).subscribe({
          next: (data) => {
            this.adminUsers = data.content ?? [];
            this.loading = false;
          },
          error: () => {
            this.loading = false;
            this.onError('Erro ao carregar usuários.');
          },
        });
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar organizações.');
      },
    });
  }

  criarUsuarioAdmin(): void {
    if (!this.novoUsuario.username.trim() || !this.novoUsuario.orgId) {
      this.onError('Informe usuário e organização.');
      return;
    }
    this.loading = true;
    this.adminUsersService.criar({
      username: this.novoUsuario.username.trim(),
      email: this.novoUsuario.email.trim() || undefined,
      orgId: this.novoUsuario.orgId,
      roles: [this.novoUsuario.roles],
      bypassSubscription: this.novoUsuario.bypassSubscription,
    }).subscribe({
      next: (res) => {
        this.loading = false;
        this.senhaTemporaria = res.temporaryPassword;
        this.novoUsuario.username = '';
        this.novoUsuario.email = '';
        this.onSuccess(`Usuário criado. Senha temporária: ${res.temporaryPassword}`);
        this.carregarAdminUsuarios();
      },
      error: (err) => {
        this.loading = false;
        this.onError(err?.error?.error || 'Erro ao criar usuário.');
      },
    });
  }

  toggleUsuarioAtivo(user: AdminUser): void {
    this.loading = true;
    const req = user.ativo ? this.adminUsersService.desativar(user.id) : this.adminUsersService.ativar(user.id);
    req.subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(user.ativo ? 'Usuário desativado.' : 'Usuário ativado.');
        this.carregarAdminUsuarios();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao alterar status do usuário.');
      },
    });
  }

  salvarLimiteDispositivos(org: AdminOrgSummary): void {
    const max = this.orgLimites[org.orgId];
    if (max == null || max < 0) {
      this.onError('Informe um limite válido (0 = ilimitado).');
      return;
    }
    this.loading = true;
    this.adminUsersService.setMaxDispositivos(org.orgId, max).subscribe({
      next: () => {
        this.loading = false;
        this.onSuccess(`Limite de dispositivos atualizado para ${org.orgNome}.`);
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao salvar limite de dispositivos.');
      },
    });
  }

  carregarPeriodosLoginLogs(): void {
    this.loginLogsService.periodos(this.filtroLoginOrgId).subscribe({
      next: (data) => {
        this.loginLogPeriodos = data ?? [];
        this.loginLogAnos = [...new Set(this.loginLogPeriodos.map((p) => p.ano))].sort((a, b) => b - a);
        this.atualizarMesesDiasLogin();
      },
      error: () => {},
    });
  }

  selecionarAnoLogin(ano: number): void {
    this.filtroLoginAno = ano;
    this.filtroLoginMes = undefined;
    this.filtroLoginDia = undefined;
    this.atualizarMesesDiasLogin();
    this.carregarLoginLogs(0);
  }

  selecionarMesLogin(mes: number): void {
    this.filtroLoginMes = mes;
    this.filtroLoginDia = undefined;
    this.atualizarMesesDiasLogin();
    this.carregarLoginLogs(0);
  }

  selecionarDiaLogin(dia: number): void {
    this.filtroLoginDia = dia;
    this.carregarLoginLogs(0);
  }

  limparFiltrosLogin(): void {
    this.filtroLoginAno = undefined;
    this.filtroLoginMes = undefined;
    this.filtroLoginDia = undefined;
    this.atualizarMesesDiasLogin();
    this.carregarLoginLogs(0);
  }

  breadcrumbLogin(): string {
    if (!this.filtroLoginAno) return 'Todos os períodos';
    let s = String(this.filtroLoginAno);
    if (this.filtroLoginMes) s += ` / ${this.mesesNomes[this.filtroLoginMes]}`;
    if (this.filtroLoginDia) s += ` / ${this.filtroLoginDia}`;
    return s;
  }

  private atualizarMesesDiasLogin(): void {
    const filtrados = this.loginLogPeriodos.filter((p) =>
      (this.filtroLoginAno == null || p.ano === this.filtroLoginAno) &&
      (this.filtroLoginMes == null || p.mes === this.filtroLoginMes)
    );
    this.loginLogMeses = this.filtroLoginAno != null
      ? [...new Set(filtrados.filter((p) => p.ano === this.filtroLoginAno).map((p) => p.mes))].sort((a, b) => b - a)
      : [];
    this.loginLogDias = this.filtroLoginAno != null && this.filtroLoginMes != null
      ? [...new Set(filtrados.filter((p) => p.ano === this.filtroLoginAno && p.mes === this.filtroLoginMes).map((p) => p.dia))].sort((a, b) => b - a)
      : [];
  }

  carregarLoginLogs(page = 0): void {
    this.loading = true;
    this.loginLogsPage = page;
    this.loginLogsService.listar(
      page,
      this.loginLogsPageSize,
      this.filtroLoginAno,
      this.filtroLoginMes,
      this.filtroLoginDia,
      this.filtroLoginOrgId
    ).subscribe({
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

  private periodoLoginBody(): { ano: number; mes?: number; dia?: number; orgId?: number } | null {
    if (this.filtroLoginAno == null) {
      this.onError('Selecione ao menos o ano do período.');
      return null;
    }
    return {
      ano: this.filtroLoginAno,
      mes: this.filtroLoginMes,
      dia: this.filtroLoginDia,
      orgId: this.filtroLoginOrgId,
    };
  }

  exportarLoginLogs(): void {
    const body = this.periodoLoginBody();
    if (!body) return;
    this.loading = true;
    this.loginLogsService.exportar(body.ano, body.mes, body.dia, body.orgId).subscribe({
      next: (blob) => {
        this.downloadBlob(blob, `login-logs_${body.ano}.json.gz`);
        this.loading = false;
        this.mensagem = 'Exportação concluída.';
        this.carregarArquivosLoginLogs();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao exportar logs.');
      },
    });
  }

  apagarLoginLogs(): void {
    const body = this.periodoLoginBody();
    if (!body) return;
    const label = this.breadcrumbLogin();
    if (!confirm(`Apagar permanentemente os logs de login do período "${label}"? Esta ação não pode ser desfeita.`)) {
      return;
    }
    this.loading = true;
    this.loginLogsService.apagar({ ...body, confirm: true }).subscribe({
      next: (res) => {
        this.loading = false;
        this.mensagem = res.message;
        this.carregarPeriodosLoginLogs();
        this.carregarLoginLogs(0);
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao apagar logs.');
      },
    });
  }

  compactarLoginLogs(): void {
    const body = this.periodoLoginBody();
    if (!body) return;
    const label = this.breadcrumbLogin();
    if (!confirm(`Compactar "${label}"? Os logs serão exportados para arquivo e removidos do banco.`)) {
      return;
    }
    this.loading = true;
    this.loginLogsService.compactar(body).subscribe({
      next: (res) => {
        this.loading = false;
        this.mensagem = res.message;
        this.carregarPeriodosLoginLogs();
        this.carregarLoginLogs(0);
        this.carregarArquivosLoginLogs();
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao compactar logs.');
      },
    });
  }

  carregarArquivosLoginLogs(): void {
    this.loginLogsService.listarArquivos(this.filtroLoginOrgId).subscribe({
      next: (data) => { this.loginLogArquivos = data ?? []; },
      error: () => {},
    });
  }

  baixarArquivoLoginLog(filename: string): void {
    this.loginLogsService.baixarArquivo(filename).subscribe({
      next: (blob) => this.downloadBlob(blob, filename),
      error: () => this.onError('Erro ao baixar arquivo.'),
    });
  }

  apagarArquivoLoginLog(filename: string): void {
    if (!confirm(`Remover o arquivo "${filename}" do disco?`)) return;
    this.loginLogsService.apagarArquivo(filename).subscribe({
      next: () => {
        this.mensagem = 'Arquivo removido.';
        this.carregarArquivosLoginLogs();
      },
      error: () => this.onError('Erro ao remover arquivo.'),
    });
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  carregarSugestoes(page = 0): void {
    this.loading = true;
    this.sugestoesPage = page;
    this.sugestaoAdminService.listar(page, 30).subscribe({
      next: (data) => {
        this.sugestoes = data.content ?? [];
        this.sugestoesTotalPages = data.totalPages ?? 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.onError('Erro ao carregar sugestões.');
      },
    });
  }

  marcarSugestaoLida(id: number): void {
    this.sugestaoAdminService.atualizarStatus(id, 'LIDA').subscribe({
      next: () => {
        this.onSuccess('Sugestão marcada como lida.');
        this.carregarSugestoes(this.sugestoesPage);
      },
      error: () => this.onError('Erro ao atualizar sugestão.'),
    });
  }

  arquivarSugestao(id: number): void {
    this.sugestaoAdminService.atualizarStatus(id, 'ARQUIVADA').subscribe({
      next: () => {
        this.onSuccess('Sugestão arquivada.');
        this.carregarSugestoes(this.sugestoesPage);
      },
      error: () => this.onError('Erro ao arquivar sugestão.'),
    });
  }

  paginaAnteriorSugestoes(): void {
    if (this.sugestoesPage > 0) this.carregarSugestoes(this.sugestoesPage - 1);
  }

  proximaPaginaSugestoes(): void {
    if (this.sugestoesPage + 1 < this.sugestoesTotalPages) this.carregarSugestoes(this.sugestoesPage + 1);
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
