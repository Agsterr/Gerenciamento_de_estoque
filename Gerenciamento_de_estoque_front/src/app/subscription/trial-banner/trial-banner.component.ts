import { Component, OnDestroy, OnInit } from '@angular/core';

import { CommonModule } from '@angular/common';

import { AuthService } from '../../services/auth.service';
import { SubscriptionService } from '../../services/subscription.service';

import { Subscription } from '../../models/subscription.model';

import { RouterLink } from '@angular/router';

import { interval, startWith, switchMap, catchError, of, Subscription as RxSubscription } from 'rxjs';



@Component({

  selector: 'app-trial-banner',

  standalone: true,

  imports: [CommonModule, RouterLink],

  templateUrl: './trial-banner.component.html',

  styleUrl: './trial-banner.component.scss'

})

export class TrialBannerComponent implements OnInit, OnDestroy {

  subscription: Subscription | null = null;

  private pollSub?: RxSubscription;

  private readonly defaultTrialDays = 15;



  constructor(
    private subscriptionService: SubscriptionService,
    private authService: AuthService
  ) {}



  ngOnInit(): void {

    this.pollSub = interval(60_000)

      .pipe(

        startWith(0),

        switchMap(() =>

          this.subscriptionService.getCurrent().pipe(catchError(() => of(null)))

        )

      )

      .subscribe((sub) => (this.subscription = sub));

  }



  ngOnDestroy(): void {

    this.pollSub?.unsubscribe();

  }



  get showBanner(): boolean {
    if (this.authService.hasSubscriptionBypass()) {
      return false;
    }
    return this.isTrialActive(this.subscription);
  }



  get daysRemaining(): number {

    const fromApi = this.subscription?.trialDaysRemaining;

    if (fromApi != null && fromApi >= 0) {

      return fromApi;

    }

    return this.computeDaysRemaining();

  }



  get daysElapsed(): number {

    const fromApi = this.subscription?.trialDaysElapsed;

    if (fromApi != null && fromApi >= 0) {

      return fromApi;

    }

    return this.computeDaysElapsed();

  }



  get totalDays(): number {

    return this.subscription?.trialDaysTotal ?? this.defaultTrialDays;

  }



  get progressPercent(): number {

    if (!this.totalDays) return 0;

    return Math.min(100, Math.round((this.daysElapsed / this.totalDays) * 100));

  }



  get urgencyClass(): string {

    if (this.daysRemaining <= 3) return 'urgent';

    if (this.daysRemaining <= 7) return 'warning';

    return 'normal';

  }



  private isTrialActive(sub: Subscription | null): boolean {

    if (!sub) return false;

    if (sub.isInTrial === true) return true;

    if (sub.status !== 'TRIAL') return false;

    if (!sub.trialEnd) return true;

    return new Date(sub.trialEnd).getTime() > Date.now();

  }



  private computeDaysRemaining(): number {

    const end = this.parseDate(this.subscription?.trialEnd);

    if (!end) return this.defaultTrialDays;

    const diffMs = end.getTime() - Date.now();

    return Math.max(0, Math.ceil(diffMs / (1000 * 60 * 60 * 24)));

  }



  private computeDaysElapsed(): number {

    const start = this.parseDate(this.subscription?.trialStart);

    if (!start) return 0;

    const diffMs = Date.now() - start.getTime();

    return Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));

  }



  private parseDate(value?: string): Date | null {

    if (!value) return null;

    const parsed = new Date(value);

    return Number.isNaN(parsed.getTime()) ? null : parsed;

  }

}


