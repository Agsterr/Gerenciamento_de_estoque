import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProdutoComponent } from './produto/produto.component';
import { ConsumersComponent } from './consumidor/consumidor.component';
import { CategoriaComponent } from './categoria/categoria.component';
import { HomeComponent } from './home/home.component';
import { MovimentacaoProdutoComponent } from './movimentacao/movimentacao.produto.component';
import { RelatoriosComponent } from './relatorios/relatorios.component';
import { AdminGuard } from './guards/admin.guard';
import { MasterAdminGuard } from './guards/master-admin.guard';
import { AuthGuard } from './guards/Auth.Guard';
import { OrgComponent } from './org/org.component';
import { AssinaturaComponent } from './assinatura/assinatura.component';
import { SubscriptionBlockedComponent } from './subscription/subscription-blocked/subscription-blocked.component';
import { SubscriptionSuccessComponent } from './subscription/subscription-success/subscription-success.component';
import { UsuarioComponent } from './usuario/usuario.component';
import { FornecedorComponent } from './fornecedor/fornecedor.component';
import { DepositoComponent } from './deposito/deposito.component';
import { PedidoCompraComponent } from './pedido-compra/pedido-compra.component';
import { PedidoVendaComponent } from './pedido-venda/pedido-venda.component';
import { DashboardInicioComponent } from './dashboard/dashboard-inicio.component';
import { InventarioComponent } from './inventario/inventario.component';
import { AuditoriaComponent } from './auditoria/auditoria.component';
import { AdminComponent } from './admin/admin.component';
import { AjudaComponent } from './ajuda/ajuda.component';
import { LoginLogsComponent } from './login-logs/login-logs.component';
import { SugestaoComponent } from './sugestao/sugestao.component';
import { PesquisaPrecoComponent } from './pesquisa-preco/pesquisa-preco.component';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: 'home', component: HomeComponent },
  { path: 'login', component: LoginComponent },
  { path: 'register', redirectTo: 'login', pathMatch: 'full' },
  { path: 'orgs', component: OrgComponent, canActivate: [MasterAdminGuard] },
  { path: 'assinatura', component: AssinaturaComponent, canActivate: [AuthGuard] },
  { path: 'subscription/blocked', component: SubscriptionBlockedComponent, canActivate: [AuthGuard] },
  { path: 'subscription/success', component: SubscriptionSuccessComponent, canActivate: [AuthGuard] },
  { path: 'admin', component: AdminComponent, canActivate: [MasterAdminGuard] },

  // Dashboard com rotas filhas
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'inicio', pathMatch: 'full' },
      { path: 'inicio', component: DashboardInicioComponent },
      { path: 'produtos', component: ProdutoComponent },
      { path: 'categorias', component: CategoriaComponent },
      { path: 'consumidores', component: ConsumersComponent },
      { path: 'pedidos-venda', component: PedidoVendaComponent },
      { path: 'entregas', redirectTo: 'pedidos-venda', pathMatch: 'full' },
      { path: 'movimentacoes', component: MovimentacaoProdutoComponent },
      { path: 'relatorios', component: RelatoriosComponent },
      { path: 'usuarios', component: UsuarioComponent, canActivate: [AdminGuard] },
      { path: 'fornecedores', component: FornecedorComponent },
      { path: 'depositos', component: DepositoComponent },
      { path: 'pedidos-compra', component: PedidoCompraComponent },
      { path: 'inventario', component: InventarioComponent },
      { path: 'auditoria', component: AuditoriaComponent },
      { path: 'acessos-login', component: LoginLogsComponent, canActivate: [AdminGuard] },
      { path: 'sugestao', component: SugestaoComponent },
      { path: 'pesquisa-preco', component: PesquisaPrecoComponent },
      { path: 'ajuda', component: AjudaComponent },
    ],
  },
];
