import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Redireciona para tela de assinatura quando o backend retorna 402 (trial expirado / assinatura inativa).
 */
export const subscriptionInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (authService.hasSubscriptionBypass()) {
        return throwError(() => error);
      }
      if (error.status === 402 && !req.url.includes('/api/subscription/')) {
        const reason = error.error?.reason;
        if (reason === 'TRIAL_ENDED' || reason === 'EXPIRED' || reason === 'NO_SUBSCRIPTION') {
          router.navigate(['/subscription/blocked'], { queryParams: { reason } });
        } else {
          router.navigate(['/assinatura'], { queryParams: { reason } });
        }
      }
      return throwError(() => error);
    })
  );
};
