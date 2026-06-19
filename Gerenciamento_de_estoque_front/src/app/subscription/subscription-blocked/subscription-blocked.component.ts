import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { SubscriptionService } from '../../services/subscription.service';
import { environment } from '../../../environments/environment';

interface SaasPlan {
  id: number;
  name: string;
  price: number;
  description?: string;
}

@Component({
  selector: 'app-subscription-blocked',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './subscription-blocked.component.html',
  styleUrl: './subscription-blocked.component.scss'
})
export class SubscriptionBlockedComponent implements OnInit {
  planId = '';
  planName = 'Gerenciamento de Estoque';
  planPrice = 69.9;
  cpfCnpj = '';
  loading = false;
  error = '';

  constructor(
    private subscriptionService: SubscriptionService,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    this.http.get<SaasPlan>(`${environment.apiUrl}/api/plans/default`).subscribe({
      next: (plan) => {
        this.planId = String(plan.id);
        this.planName = plan.name;
        this.planPrice = Number(plan.price);
      },
      error: () => {
        this.planId = '1';
      }
    });
  }

  payWithAsaas(): void {
    this.loading = true;
    this.error = '';
    this.subscriptionService.checkoutAsaas(this.planId, this.cpfCnpj || undefined).subscribe({
      next: (res) => {
        this.loading = false;
        const url = res.paymentUrl || res.initPoint;
        if (url) {
          window.location.href = url;
        } else {
          this.error = 'Link de pagamento não retornado pelo servidor.';
        }
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error || 'Não foi possível gerar o link de pagamento.';
      }
    });
  }
}
