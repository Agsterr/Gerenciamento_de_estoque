import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http'; // Import do HttpClient
import { routes } from './app/app.routes';

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),          // Configura as rotas
    provideHttpClient(),            // Configura o HttpClient
  ],
}).catch((err) => console.error(err));  // Tratamento de erro
