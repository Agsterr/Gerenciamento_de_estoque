

import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProdutoComponent } from './produto/produto.component';
import { ConsumersComponent } from './consumidor/consumidor.component';
import { EntregasComponent } from './entregas/entregas.component';
import { CategoriaComponent } from './categoria/categoria.component';
import { HomeComponent } from './home/home.component'; // Importe o HomeComponent
import { AuthGuard } from './guards/Auth.Guard';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' }, // Redireciona para home por padrão
  { path: 'home', component: HomeComponent }, // Rota para o componente Home
  
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent, canActivate: [AuthGuard] },

  {
    path: 'dashboard',
    component: DashboardComponent,
    children: [
      { path: 'produtos', component: ProdutoComponent },
      { path: 'categorias', component: CategoriaComponent },
      { path: 'consumidores', component: ConsumersComponent },
      { path: 'entregas', component: EntregasComponent },
    ],
  },
];
