import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { LoginLog, LoginLogPeriodo, LoginLogExportFile, OrgLoginLogsService } from '../services/login-logs.service';

@Component({
  selector: 'app-login-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-logs.component.html',
  styleUrls: ['./login-logs.component.scss'],
})
export class LoginLogsComponent implements OnInit {
  logs: LoginLog[] = [];
  periodos: LoginLogPeriodo[] = [];
  anos: number[] = [];
  meses: number[] = [];
  dias: number[] = [];
  filtroAno?: number;
  filtroMes?: number;
  filtroDia?: number;
  loading = false;
  mensagemErro = '';
  currentPage = 0;
  pageSize = 30;
  totalElements = 0;
  totalPages = 0;
  arquivos: LoginLogExportFile[] = [];

  readonly mesesNomes = ['', 'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

  constructor(private loginLogsService: OrgLoginLogsService) {}

  ngOnInit(): void {
    this.carregarPeriodos();
    this.carregarLogs();
    this.carregarArquivos();
  }

  carregarPeriodos(): void {
    this.loginLogsService.periodos().subscribe({
      next: (data) => {
        this.periodos = data ?? [];
        this.anos = [...new Set(this.periodos.map((p) => p.ano))].sort((a, b) => b - a);
        this.atualizarMesesDias();
      },
      error: () => {},
    });
  }

  carregarLogs(page = 0): void {
    this.loading = true;
    this.currentPage = page;
    this.loginLogsService.listar(page, this.pageSize, this.filtroAno, this.filtroMes, this.filtroDia).subscribe({
      next: (data) => {
        this.logs = data.content ?? [];
        this.totalElements = data.totalElements ?? 0;
        this.totalPages = data.totalPages ?? 0;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao carregar logs de acesso.';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  selecionarAno(ano: number): void {
    this.filtroAno = ano;
    this.filtroMes = undefined;
    this.filtroDia = undefined;
    this.atualizarMesesDias();
    this.carregarLogs(0);
  }

  selecionarMes(mes: number): void {
    this.filtroMes = mes;
    this.filtroDia = undefined;
    this.atualizarMesesDias();
    this.carregarLogs(0);
  }

  selecionarDia(dia: number): void {
    this.filtroDia = dia;
    this.carregarLogs(0);
  }

  limparFiltros(): void {
    this.filtroAno = undefined;
    this.filtroMes = undefined;
    this.filtroDia = undefined;
    this.atualizarMesesDias();
    this.carregarLogs(0);
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.carregarLogs(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.carregarLogs(this.currentPage + 1);
  }

  breadcrumb(): string {
    if (!this.filtroAno) return 'Todos os períodos';
    let s = String(this.filtroAno);
    if (this.filtroMes) s += ` / ${this.mesesNomes[this.filtroMes]}`;
    if (this.filtroDia) s += ` / ${this.filtroDia}`;
    return s;
  }

  private atualizarMesesDias(): void {
    const filtrados = this.periodos.filter((p) =>
      (this.filtroAno == null || p.ano === this.filtroAno) &&
      (this.filtroMes == null || p.mes === this.filtroMes)
    );
    this.meses = this.filtroAno != null
      ? [...new Set(filtrados.filter((p) => p.ano === this.filtroAno).map((p) => p.mes))].sort((a, b) => b - a)
      : [];
    this.dias = this.filtroAno != null && this.filtroMes != null
      ? [...new Set(filtrados.filter((p) => p.ano === this.filtroAno && p.mes === this.filtroMes).map((p) => p.dia))].sort((a, b) => b - a)
      : [];
  }

  private periodoBody(): { ano: number; mes?: number; dia?: number } | null {
    if (this.filtroAno == null) {
      this.mensagemErro = 'Selecione ao menos o ano do período.';
      setTimeout(() => (this.mensagemErro = ''), 3000);
      return null;
    }
    return { ano: this.filtroAno, mes: this.filtroMes, dia: this.filtroDia };
  }

  exportarPeriodo(): void {
    const body = this.periodoBody();
    if (!body) return;
    this.loading = true;
    this.loginLogsService.exportar(body.ano, body.mes, body.dia).subscribe({
      next: (blob) => {
        this.downloadBlob(blob, `login-logs_${body.ano}.json.gz`);
        this.loading = false;
        this.carregarArquivos();
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao exportar logs.';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  apagarPeriodo(): void {
    const body = this.periodoBody();
    if (!body) return;
    if (!confirm(`Apagar logs do período "${this.breadcrumb()}"? Esta ação não pode ser desfeita.`)) return;
    this.loading = true;
    this.loginLogsService.apagar({ ...body, confirm: true }).subscribe({
      next: () => {
        this.loading = false;
        this.carregarPeriodos();
        this.carregarLogs(0);
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao apagar logs.';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  compactarPeriodo(): void {
    const body = this.periodoBody();
    if (!body) return;
    if (!confirm(`Compactar "${this.breadcrumb()}"? Exporta para arquivo e remove do banco.`)) return;
    this.loading = true;
    this.loginLogsService.compactar(body).subscribe({
      next: () => {
        this.loading = false;
        this.carregarPeriodos();
        this.carregarLogs(0);
        this.carregarArquivos();
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao compactar logs.';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  carregarArquivos(): void {
    this.loginLogsService.listarArquivos().subscribe({
      next: (data) => { this.arquivos = data ?? []; },
      error: () => {},
    });
  }

  baixarArquivo(filename: string): void {
    this.loginLogsService.baixarArquivo(filename).subscribe({
      next: (blob) => this.downloadBlob(blob, filename),
      error: () => {
        this.mensagemErro = 'Erro ao baixar arquivo.';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  apagarArquivo(filename: string): void {
    if (!confirm(`Remover o arquivo "${filename}"?`)) return;
    this.loginLogsService.apagarArquivo(filename).subscribe({
      next: () => this.carregarArquivos(),
      error: () => {
        this.mensagemErro = 'Erro ao remover arquivo.';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  formatBytes(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    window.URL.revokeObjectURL(url);
  }
}
