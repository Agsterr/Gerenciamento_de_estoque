

import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProdutoComponent } from './produto/produto.component';
import { ConsumersComponent } from './consumidor/consumidor.component';

// Novo import
import { EntregasComponent} from './entregas/entregas.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  {
    path: 'dashboard',
    component: DashboardComponent,
    children: [
      {
        path: 'produtos',
        children: [
          { path: '', component: ProdutoComponent }, // Lista de produtos
          { path: ':id', component: ProdutoComponent }, // Detalhes de um produto
        ],
      },
      {
        path: 'consumidores',
        component: ConsumersComponent,
      },
      {
        path: 'entregas',
        component: EntregasComponent,
      },
    ],
  },
];
