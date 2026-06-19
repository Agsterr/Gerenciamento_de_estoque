import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SubscriptionService } from '../../services/subscription.service';
import { Subscription } from '../../models/subscription.model';
import { interval, startWith, switchMap, takeWhile, catchError, of, Subscription as RxSub } from 'rxjs';

@Component({
  selector: 'app-subscription-success',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './subscription-success.component.html',
  styleUrl: './subscription-success.component.scss',
})
export class SubscriptionSuccessComponent implements OnInit, OnDestroy {
  subscription: Subscription | null = null;
  loading = true;
  confirmed = false;
  attempts = 0;
  private pollSub?: RxSub;

  constructor(private subscriptionService: SubscriptionService) {}

  ngOnInit(): void {
    this.pollSub = interval(3000)
      .pipe(
        startWith(0),
        takeWhile(() => !this.confirmed && this.attempts < 40),
        switchMap(() => {
          this.attempts++;
          return this.subscriptionService.simulateSandboxPayment().pipe(
            catchError(() => this.subscriptionService.syncPayment().pipe(catchError(() => of(null))))
          );
        })
      )
      .subscribe((sub) => {
        this.loading = false;
        if (!sub) return;
        this.subscription = sub;
        if (sub.status === 'ACTIVE' || sub.isActive) {
          this.confirmed = true;
          this.pollSub?.unsubscribe();
        }
      });
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  retrySync(): void {
    this.loading = true;
    this.subscriptionService.simulateSandboxPayment().subscribe({
      next: (sub) => {
        this.loading = false;
        this.subscription = sub;
        this.confirmed = sub.status === 'ACTIVE' || !!sub.isActive;
      },
      error: () => {
        this.subscriptionService.syncPayment().subscribe({
          next: (sub) => {
            this.loading = false;
            this.subscription = sub;
            this.confirmed = sub.status === 'ACTIVE' || !!sub.isActive;
          },
          error: () => {
            this.loading = false;
          },
        });
      },
    });
  }
}
