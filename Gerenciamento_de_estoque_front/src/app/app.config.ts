import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes'; // Importa as rotas definidas

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes), // Configura as rotas aqui
  ],
};

