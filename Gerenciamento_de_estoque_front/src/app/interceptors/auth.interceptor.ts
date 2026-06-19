import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

export const AuthInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const token = localStorage.getItem('jwtToken');

  const authReq =
    token && !req.url.includes('/auth/login') && !req.url.includes('/auth/register')
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 402 || error.status === 423) {
        const message =
          error.status === 423
            ? 'Seu acesso foi bloqueado. Entre em contato com o suporte.'
            : 'Sua assinatura expirou ou está inativa. Assine um plano para continuar.';
        sessionStorage.setItem('subscriptionBlockMessage', message);
        router.navigate(['/assinatura']);
      }
      return throwError(() => error);
    })
  );
};
