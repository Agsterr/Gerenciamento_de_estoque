import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { PlanService } from '../services/plan.service';
import { SubscriptionService } from '../services/subscription.service';
import { AuthService } from '../services/auth.service';
import { Plan } from '../models/plan.model';
import { AsaasPaymentMode, CheckoutResponse, Subscription } from '../models/subscription.model';
import { environment } from '../../environments/environment';

@Component({
  selector: 'app-assinatura',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './assinatura.component.html',
  styleUrls: ['./assinatura.component.scss'],
})
export class AssinaturaComponent implements OnInit {
  planos: Plan[] = [];
  assinaturaAtual: Subscription | null = null;
  historico: Subscription[] = [];
  loading = false;
  mensagem = '';
  mensagemErro = '';
  blockMessage = '';
  cpfCnpj = '';
  planoSelecionado: Plan | null = null;
  sandboxMode = true;
  modoPagamento: AsaasPaymentMode = 'RECURRING';
  pagamentoPendente: CheckoutResponse | null = null;
  pixCopiado = false;

  readonly modosPagamento: { id: AsaasPaymentMode; titulo: string; descricao: string; icone: string }[] = [
    {
      id: 'RECURRING',
      titulo: 'Assinatura recorrente',
      descricao: 'Cartão ou Pix recorrente na página segura do Asaas. Renovação automática.',
      icone: 'fa-sync-alt',
    },
    {
      id: 'PIX',
      titulo: 'Pix mensal',
      descricao: 'Cobrança avulsa com QR Code. Pague manualmente a cada mês.',
      icone: 'fa-qrcode',
    },
    {
      id: 'BOLETO',
      titulo: 'Boleto mensal',
      descricao: 'Cobrança avulsa com boleto. Pague manualmente a cada mês.',
      icone: 'fa-barcode',
    },
  ];

  get showSandboxSimulator(): boolean {
    if (!this.sandboxMode) return false;
    if (!this.assinaturaAtual) return true;
    return this.assinaturaAtual.status !== 'ACTIVE';
  }

  constructor(
    private planService: PlanService,
    private subscriptionService: SubscriptionService,
    private authService: AuthService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.blockMessage = sessionStorage.getItem('subscriptionBlockMessage') || '';
    sessionStorage.removeItem('subscriptionBlockMessage');
    this.carregarDados();
    this.http.get<{ testMode?: boolean }>(`${environment.apiUrl}/api/asaas/config`).subscribe({
      next: (cfg) => (this.sandboxMode = !!cfg.testMode),
      error: () => (this.sandboxMode = false),
    });
  }

  carregarDados(): void {
    this.loading = true;
    this.planService.listarAtivos().subscribe({
      next: (planos) => {
        this.planos = planos;
        this.loading = false;
      },
      error: () => {
        this.mensagemErro = 'Erro ao carregar planos.';
        this.loading = false;
      },
    });
    this.subscriptionService.getCurrent().subscribe({
      next: (sub) => {
        this.assinaturaAtual = sub;
        if (sub?.pendingPayment) {
          this.verificarPagamento();
        }
      },
      error: () => (this.assinaturaAtual = null),
    });
    this.subscriptionService.getHistory().subscribe({
      next: (hist) => (this.historico = hist),
      error: () => (this.historico = []),
    });
  }

  selecionarModo(modo: AsaasPaymentMode): void {
    this.modoPagamento = modo;
    this.pagamentoPendente = null;
    this.mensagemErro = '';
  }

  assinarPlano(plano: Plan): void {
    this.planoSelecionado = plano;
    this.assinarComAsaas(plano);
  }

  assinarComAsaas(plano: Plan): void {
    const planId = String(plano.id);
    this.loading = true;
    this.mensagemErro = '';
    this.pagamentoPendente = null;
    this.pixCopiado = false;

    sessionStorage.setItem('postPaymentReturn', '/subscription/success');

    this.subscriptionService.checkoutAsaas(planId, this.cpfCnpj || undefined, this.modoPagamento).subscribe({
      next: (response) => {
        this.loading = false;

        if (this.modoPagamento === 'RECURRING') {
          const url = response.paymentUrl || response.initPoint;
          if (url) {
            window.location.href = url;
            return;
          }
          this.mensagemErro = 'Nenhum link de pagamento retornado pelo Asaas.';
          return;
        }

        this.pagamentoPendente = response;
        this.mensagem =
          this.modoPagamento === 'PIX'
            ? 'Pix gerado! Escaneie o QR Code ou copie o código. Após pagar, clique em "Verificar pagamento".'
            : 'Boleto gerado! Pague até o vencimento e clique em "Verificar pagamento" após a compensação.';
        this.carregarDados();
      },
      error: (err) => {
        this.loading = false;
        this.mensagemErro = err?.error?.error || err?.error?.message || 'Erro ao iniciar pagamento no Asaas.';
      },
    });
  }

  copiarPix(): void {
    const codigo = this.pagamentoPendente?.pixCopyPaste;
    if (!codigo) return;
    navigator.clipboard.writeText(codigo).then(() => {
      this.pixCopiado = true;
      setTimeout(() => (this.pixCopiado = false), 2500);
    });
  }

  copiarLinhaDigitavel(): void {
    const linha = this.pagamentoPendente?.identificationField;
    if (!linha) return;
    navigator.clipboard.writeText(linha).then(() => {
      this.pixCopiado = true;
      setTimeout(() => (this.pixCopiado = false), 2500);
    });
  }

  verificarPagamento(): void {
    this.subscriptionService.syncPayment().subscribe({
      next: (sub) => {
        this.assinaturaAtual = sub;
        if (sub.status === 'ACTIVE' || sub.isActive) {
          this.mensagem = 'Pagamento confirmado! Sua assinatura está ativa.';
          this.pagamentoPendente = null;
        }
      },
      error: (err) => {
        this.mensagemErro = err?.error?.error || 'Não foi possível verificar o pagamento.';
      },
    });
  }

  simularPagamentoSandbox(): void {
    this.loading = true;
    this.mensagemErro = '';
    this.subscriptionService.simulateSandboxPayment().subscribe({
      next: (sub) => {
        this.loading = false;
        this.assinaturaAtual = sub;
        this.pagamentoPendente = null;
        this.mensagem = 'Pagamento simulado com sucesso! Assinatura ativa.';
      },
      error: (err) => {
        this.loading = false;
        this.mensagemErro = err?.error?.error || 'Não foi possível simular o pagamento.';
      },
    });
  }

  cancelar(): void {
    if (!confirm('Deseja realmente cancelar sua assinatura?')) return;
    this.loading = true;
    this.subscriptionService.cancel().subscribe({
      next: (res) => {
        this.mensagem = res.message || 'Assinatura cancelada.';
        this.loading = false;
        this.carregarDados();
      },
      error: (err) => {
        this.mensagemErro = err?.error?.message || err?.error?.error || 'Erro ao cancelar assinatura.';
        this.loading = false;
      },
    });
  }

  formatarPreco(preco: number): string {
    return preco?.toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' }) || '—';
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      TRIAL: 'Período de teste',
      ACTIVE: 'Ativa',
      CANCELED: 'Cancelada',
      PAUSED: 'Pausada',
      PAST_DUE: 'Pagamento pendente',
      EXPIRED: 'Expirada',
      INCOMPLETE: 'Aguardando pagamento',
    };
    return labels[status] || status;
  }

  modoPagamentoLabel(modo?: string): string {
    const labels: Record<string, string> = {
      RECURRING: 'Recorrente (Asaas)',
      PIX: 'Pix mensal',
      BOLETO: 'Boleto mensal',
    };
    return labels[modo || ''] || modo || '—';
  }

  provedorLabel(provedor?: string): string {
    if (!provedor) return 'Asaas';
    return provedor.toUpperCase() === 'ASAAS' ? 'Asaas' : provedor;
  }

  botaoAssinarLabel(): string {
    switch (this.modoPagamento) {
      case 'PIX':
        return 'Gerar Pix';
      case 'BOLETO':
        return 'Gerar boleto';
      default:
        return 'Assinar (recorrente)';
    }
  }
}
