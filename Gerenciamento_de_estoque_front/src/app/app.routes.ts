import { Routes } from '@angular/router';
import { LoginComponent } from './login/login.component';
import { RegisterComponent } from './register/register.component';
import { HomeComponent } from './home/home.component';

export const routes: Routes = [
  { path: '', component: HomeComponent },        // Página inicial
  { path: 'login', component: LoginComponent },  // Página de Login
  { path: 'register', component: RegisterComponent } // Página de Cadastro
];


