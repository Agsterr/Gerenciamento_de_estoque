


import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { HomeComponent } from './home/home.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ConsumersComponent } from './consumidor/consumidor.component';
import { AuthGuard } from './guards/Auth.Guard';  // Certifique-se de importar o AuthGuard

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'home', component: HomeComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard],  // Protege a rota do dashboard
    children: [
      { path: 'estoque', component: HomeComponent },
      { path: 'saidas-entradas', component: HomeComponent },
      { path: 'consumers', component: ConsumersComponent },
    ]
  }
];
