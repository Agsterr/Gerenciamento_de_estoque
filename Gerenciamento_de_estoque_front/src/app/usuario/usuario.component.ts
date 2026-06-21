import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { UsuarioService } from '../services/usuario.service';
import { Usuario } from '../models/usuario.model';

interface CredencialUsuario {
  username: string;
  senha: string;
}

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
  credencial: CredencialUsuario | null = null;
  copiadoMsg = '';

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

  gerarSenha(usuario: Usuario): void {
    if (!confirm(`Gerar nova senha temporária para ${usuario.username}? A senha atual será substituída.`)) {
      return;
    }
    this.loading = true;
    this.usuarioService.resetSenha(usuario.id).subscribe({
      next: (res) => {
        this.loading = false;
        this.credencial = { username: res.username, senha: res.temporaryPassword };
        this.mensagem = '';
        this.errorMessage = '';
        this.scrollToCredencial();
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Erro ao gerar nova senha.';
      },
    });
  }

  fecharCredencial(): void {
    this.credencial = null;
  }

  copiar(texto: string, label: string): void {
    if (!texto) return;
    navigator.clipboard.writeText(texto).then(() => {
      this.copiadoMsg = `${label} copiado!`;
      setTimeout(() => (this.copiadoMsg = ''), 2500);
    });
  }

  copiarCredenciais(): void {
    if (!this.credencial) return;
    const texto = `Usuário: ${this.credencial.username}\nSenha: ${this.credencial.senha}`;
    this.copiar(texto, 'Credenciais');
  }

  private scrollToCredencial(): void {
    setTimeout(() => {
      document.getElementById('credencial-usuario')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 100);
  }
}
