import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../services/auth.service';
import { UsuarioService } from '../services/usuario.service';
import { Usuario } from '../models/usuario.model';

interface CredencialArmazenada {
  username: string;
  senha: string;
  geradoEm: number;
}

const CREDENCIAIS_STORAGE_KEY = 'org_credenciais_recentes';

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
  novoUsuario = { username: '', email: '' };
  credencial: CredencialArmazenada | null = null;
  credenciaisRecentes: Record<number, CredencialArmazenada> = {};
  copiadoMsg = '';
  limiteUsuarios: { ativos: number; maximo: number | null; ilimitado: boolean } | null = null;

  get podeCriarUsuario(): boolean {
    if (!this.limiteUsuarios) return true;
    if (this.limiteUsuarios.ilimitado || this.limiteUsuarios.maximo == null) return true;
    return this.limiteUsuarios.ativos < this.limiteUsuarios.maximo;
  }

  get textoLimiteUsuarios(): string {
    if (!this.limiteUsuarios) return '';
    if (this.limiteUsuarios.ilimitado || this.limiteUsuarios.maximo == null) {
      return `Usuários ativos: ${this.limiteUsuarios.ativos} (sem limite definido)`;
    }
    return `Usuários ativos: ${this.limiteUsuarios.ativos} / ${this.limiteUsuarios.maximo}`;
  }

  constructor(
    private authService: AuthService,
    private usuarioService: UsuarioService
  ) {}

  ngOnInit(): void {
    this.isAdmin = this.authService.isAdmin();
    if (this.isAdmin) {
      this.carregarCredenciaisRecentes();
      this.loadLimites();
      this.loadUsuarios();
    } else {
      this.errorMessage = 'Você não tem permissão para acessar esta página.';
    }
  }

  loadLimites(): void {
    this.usuarioService.consultarLimites().subscribe({
      next: (limite) => {
        this.limiteUsuarios = limite;
      },
      error: () => {},
    });
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

  criarUsuario(): void {
    if (!this.novoUsuario.username.trim()) {
      this.errorMessage = 'Informe o nome de usuário.';
      return;
    }
    if (!this.podeCriarUsuario) {
      this.errorMessage = 'Limite de usuários atingido. Contate o suporte para aumentar.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.usuarioService.criar(this.novoUsuario.username.trim(), this.novoUsuario.email).subscribe({
      next: (res) => {
        this.loading = false;
        this.mostrarCredencial(res.usuario.id, res.usuario.username, res.temporaryPassword);
        this.novoUsuario = { username: '', email: '' };
        this.loadLimites();
        this.loadUsuarios();
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.error || err?.error?.message || 'Erro ao criar usuário.';
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
    if (!confirm(`Gerar nova senha para ${usuario.username}?\n\nA senha atual será substituída.`)) {
      return;
    }
    this.loading = true;
    this.usuarioService.resetSenha(usuario.id).subscribe({
      next: (res) => {
        this.loading = false;
        this.mostrarCredencial(usuario.id, res.username, res.temporaryPassword);
      },
      error: () => {
        this.loading = false;
        this.errorMessage = 'Erro ao gerar nova senha.';
      },
    });
  }

  credencialDoUsuario(userId: number): CredencialArmazenada | null {
    return this.credenciaisRecentes[userId] ?? null;
  }

  mostrarCredencial(userId: number, username: string, senha: string): void {
    this.credencial = { username, senha, geradoEm: Date.now() };
    this.credenciaisRecentes[userId] = this.credencial;
    this.salvarCredenciaisRecentes();
    this.mensagem = '';
    this.scrollToCredencial();
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

  copiarCredenciaisDe(userId: number): void {
    const cred = this.credenciaisRecentes[userId];
    if (!cred) return;
    this.copiar(`Usuário: ${cred.username}\nSenha: ${cred.senha}`, 'Login e senha');
  }

  private carregarCredenciaisRecentes(): void {
    try {
      const raw = sessionStorage.getItem(CREDENCIAIS_STORAGE_KEY);
      this.credenciaisRecentes = raw ? JSON.parse(raw) : {};
    } catch {
      this.credenciaisRecentes = {};
    }
  }

  private salvarCredenciaisRecentes(): void {
    try {
      sessionStorage.setItem(CREDENCIAIS_STORAGE_KEY, JSON.stringify(this.credenciaisRecentes));
    } catch {
      // ignore
    }
  }

  private scrollToCredencial(): void {
    setTimeout(() => {
      document.getElementById('credencial-usuario')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }, 100);
  }
}
