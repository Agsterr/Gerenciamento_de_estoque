import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProdutoComponent } from './produto/produto.component';
import { ConsumersComponent } from './consumidor/consumidor.component';
import { EntregasComponent } from './entregas/entregas.component';
import { CategoriaComponent } from './categoria/categoria.component';
import { HomeComponent } from './home/home.component';
import { MovimentacaoProdutoComponent } from './movimentacao/movimentacao.produto.component';
import { AdminGuard } from './guards/admin.guard';
import { AuthGuard } from './guards/Auth.Guard';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent, canActivate: [AdminGuard] },

  // Dashboard com rotas filhas
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'produtos', pathMatch: 'full' },
      { path: 'produtos', component: ProdutoComponent },
      { path: 'categorias', component: CategoriaComponent },
      { path: 'consumidores', component: ConsumersComponent },
      { path: 'entregas', component: EntregasComponent },
      { path: 'movimentacoes', component: MovimentacaoProdutoComponent },
    ],
  },
];
