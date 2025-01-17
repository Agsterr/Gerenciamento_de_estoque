



import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { HomeComponent } from './home/home.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { EstoqueComponent } from './estoque/estoque.component'; // Componente Estoque
import { EntradasComponent } from './estoque/entradas/entradas.component'; // Componente Entradas
import { SaidasComponent } from './estoque/saidas/saidas.component'; // Componente Saídas

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    children: [
      {
        path: 'estoque',
        component: EstoqueComponent,
        children: [
          { path: 'entradas', component: EntradasComponent }, // Rota de Entradas
          { path: 'saidas', component: SaidasComponent }, // Rota de Saídas
        ],
      },
    ],
  },
];
