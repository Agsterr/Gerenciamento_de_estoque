import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimations } from '@angular/platform-browser/animations';
import { routes } from './app.routes';
import { AuthInterceptor } from './interceptors/auth.interceptor';
import { subscriptionInterceptor } from './interceptors/subscription.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([AuthInterceptor, subscriptionInterceptor])
    ),
    provideAnimations() // Necessário para o MatDialog
  ],
};

