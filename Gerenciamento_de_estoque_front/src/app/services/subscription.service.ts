import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { CheckoutResponse, Subscription, AsaasPaymentMode } from '../models/subscription.model';

@Injectable({
  providedIn: 'root',
})
export class SubscriptionService {
  private apiUrl = `${environment.apiUrl}/api/subscription`;

  constructor(private http: HttpClient) {}

  getCurrent(): Observable<Subscription> {
    return this.http.get<Subscription>(`${this.apiUrl}/current`);
  }

  getHistory(): Observable<Subscription[]> {
    return this.http.get<Subscription[]>(`${this.apiUrl}/history`);
  }

  startCheckout(planId: string | number, backUrl?: string): Observable<CheckoutResponse> {
    const params = new URLSearchParams();
    params.set('planId', String(planId));
    if (backUrl) params.set('backUrl', backUrl);
    return this.http.post<CheckoutResponse>(`${this.apiUrl}/checkout?${params.toString()}`, {});
  }

  startTransparentCheckout(planId: string | number, cardTokenId: string, payerEmail: string): Observable<CheckoutResponse> {
    return this.http.post<CheckoutResponse>(`${this.apiUrl}/checkout`, {
      planId: String(planId),
      cardTokenId,
      payerEmail,
    });
  }

  cancel(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/cancel`, {});
  }

  checkoutAsaas(planId: string | number, cpfCnpj?: string, paymentMode: AsaasPaymentMode = 'RECURRING'): Observable<CheckoutResponse> {
    const body = {
      planId: String(planId),
      cpfCnpj: cpfCnpj?.trim() || undefined,
      paymentMode,
    };
    if (paymentMode === 'PIX') {
      return this.http.post<CheckoutResponse>(`${this.apiUrl}/checkout/asaas/pix`, body);
    }
    if (paymentMode === 'BOLETO') {
      return this.http.post<CheckoutResponse>(`${this.apiUrl}/checkout/asaas/boleto`, body);
    }
    return this.http.post<CheckoutResponse>(`${this.apiUrl}/checkout/asaas`, body);
  }

  /** Consulta Asaas e ativa assinatura se pagamento confirmado (fallback ao webhook). */
  syncPayment(): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.apiUrl}/sync-payment`, {});
  }

  /** Sandbox: confirma pagamento via API Asaas e ativa assinatura. */
  simulateSandboxPayment(): Observable<Subscription> {
    return this.http.post<Subscription>(`${this.apiUrl}/simulate-payment`, {});
  }

  checkFeatureAccess(feature: string): Observable<{ hasAccess: boolean }> {
    return this.http.get<{ hasAccess: boolean }>(`${this.apiUrl}/feature-access`, {
      params: { feature },
    });
  }

  checkUsageLimits(limitType: string, currentCount: number): Observable<{ withinLimits: boolean }> {
    return this.http.get<{ withinLimits: boolean }>(`${this.apiUrl}/usage-limits`, {
      params: { limitType, currentCount: String(currentCount) },
    });
  }
}
