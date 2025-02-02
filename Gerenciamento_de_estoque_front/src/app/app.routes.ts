

// app.routes.ts
import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProdutoComponent } from './produto/produto.component';
import { ConsumersComponent } from './consumidor/consumidor.component';
import { EntregasComponent } from './entregas/entregas.component';
import { CategoriaComponent } from './categoria/categoria.component'; // Importando o CategoriaComponent

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    children: [
      { path: 'produtos', component: ProdutoComponent },
      { path: 'categorias', component: CategoriaComponent }, // Rota para as Categorias
      { path: 'consumidores', component: ConsumersComponent },
      { path: 'entregas', component: EntregasComponent },
    ],
  },
];

