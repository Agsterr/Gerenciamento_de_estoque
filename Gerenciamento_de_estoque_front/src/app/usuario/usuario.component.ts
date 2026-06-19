import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { UsuarioService } from '../services/usuario.service';
import { Usuario } from '../models/usuario.model';

@Component({
  selector: 'app-usuario',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './usuario.component.html',
  styleUrls: ['./usuario.component.scss'],
})
export class UsuarioComponent implements OnInit {
  usuarios: Usuario[] = [];
  isAdmin = false;
  errorMessage = '';
  mensagem = '';
  loading = false;
  reativarUsername = '';

  constructor(
    private authService: AuthService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    if (this.isAdmin) {
      this.loadUsuarios();
    } else {
      this.errorMessage = 'Você não tem permissão para acessar esta página.';
    }
  }

  loadUsuarios(): void {
    this.loading = true;
    this.usuarioService.listarAtivos().subscribe({
      next: (page) => {
        this.usuarios = page.content;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'Erro ao carregar usuários.';
        this.loading = false;
      },
    });
  }

  desativar(usuario: Usuario): void {
    if (!confirm(`Desativar o usuário ${usuario.username}?`)) return;
    this.usuarioService.desativar(usuario.id).subscribe({
      next: (res) => {
        this.mensagem = res.message;
        this.loadUsuarios();
      },
      error: () => (this.errorMessage = 'Erro ao desativar usuário.'),
    });
  }

  ativar(usuario: Usuario): void {
    this.usuarioService.ativar(usuario.id).subscribe({
      next: (res) => {
        this.mensagem = res.message;
        this.loadUsuarios();
      },
      error: () => (this.errorMessage = 'Erro ao ativar usuário.'),
    });
  }

  reativar(): void {
    if (!this.reativarUsername.trim()) return;
    this.usuarioService.reativar(this.reativarUsername.trim()).subscribe({
      next: (res) => {
        this.mensagem = res.message;
        this.reativarUsername = '';
        this.loadUsuarios();
      },
      error: () => (this.errorMessage = 'Erro ao reativar usuário.'),
    });
  }
}
