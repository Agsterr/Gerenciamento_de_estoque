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
  filtroIp = '';
  ipsDisponiveis: string[] = [];
  loading = false;
  mensagemErro = '';
  currentPage = 0;
  pageSize = 20;
  readonly pageSizeOptions = [10, 20, 50];
  totalElements = 0;
  totalPages = 0;
  arquivos: LoginLogExportFile[] = [];

  readonly mesesNomes = ['', 'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'];

  constructor(private loginLogsService: OrgLoginLogsService) {}

  ngOnInit(): void {
    this.carregarPeriodos();
    this.carregarArquivos();
  }

  get diaSelecionado(): boolean {
    return this.filtroAno != null && this.filtroMes != null && this.filtroDia != null;
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
    if (!this.diaSelecionado) {
      this.logs = [];
      this.totalElements = 0;
      this.totalPages = 0;
      return;
    }
    this.loading = true;
    this.currentPage = page;
    this.loginLogsService.listar(
      page,
      this.pageSize,
      this.filtroAno!,
      this.filtroMes!,
      this.filtroDia!,
      this.filtroIp || undefined
    ).subscribe({
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

  carregarIps(): void {
    if (!this.diaSelecionado) {
      this.ipsDisponiveis = [];
      return;
    }
    this.loginLogsService.ips(this.filtroAno!, this.filtroMes!, this.filtroDia!).subscribe({
      next: (data) => { this.ipsDisponiveis = data ?? []; },
      error: () => { this.ipsDisponiveis = []; },
    });
  }

  selecionarAno(ano: number): void {
    this.filtroAno = ano;
    this.filtroMes = undefined;
    this.filtroDia = undefined;
    this.filtroIp = '';
    this.ipsDisponiveis = [];
    this.atualizarMesesDias();
    this.limparLista();
  }

  selecionarMes(mes: number): void {
    this.filtroMes = mes;
    this.filtroDia = undefined;
    this.filtroIp = '';
    this.ipsDisponiveis = [];
    this.atualizarMesesDias();
    this.limparLista();
  }

  selecionarDia(dia: number): void {
    this.filtroDia = dia;
    this.filtroIp = '';
    this.carregarIps();
    this.carregarLogs(0);
  }

  alterarFiltroIp(): void {
    if (this.diaSelecionado) {
      this.carregarLogs(0);
    }
  }

  limparFiltroIp(): void {
    this.filtroIp = '';
    this.carregarLogs(0);
  }

  limparFiltros(): void {
    this.filtroAno = undefined;
    this.filtroMes = undefined;
    this.filtroDia = undefined;
    this.filtroIp = '';
    this.ipsDisponiveis = [];
    this.atualizarMesesDias();
    this.limparLista();
  }

  alterarPageSize(): void {
    if (this.diaSelecionado) {
      this.carregarLogs(0);
    }
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.carregarLogs(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.carregarLogs(this.currentPage + 1);
  }

  irParaPagina(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.carregarLogs(page);
    }
  }

  paginasVisiveis(): number[] {
    const total = this.totalPages;
    if (total <= 1) return [];
    const atual = this.currentPage;
    const inicio = Math.max(0, atual - 2);
    const fim = Math.min(total - 1, inicio + 4);
    const pages: number[] = [];
    for (let i = inicio; i <= fim; i++) pages.push(i);
    return pages;
  }

  totalDoDia(): number {
    if (!this.diaSelecionado) return 0;
    const p = this.periodos.find(
      (x) => x.ano === this.filtroAno && x.mes === this.filtroMes && x.dia === this.filtroDia
    );
    return p?.total ?? 0;
  }

  breadcrumb(): string {
    if (!this.filtroAno) return 'Selecione um período';
    let s = String(this.filtroAno);
    if (this.filtroMes) s += ` / ${this.mesesNomes[this.filtroMes]}`;
    if (this.filtroDia) s += ` / ${String(this.filtroDia).padStart(2, '0')}`;
    return s;
  }

  labelDiaCompleto(): string {
    if (!this.diaSelecionado) return '';
    return `${String(this.filtroDia).padStart(2, '0')}/${String(this.filtroMes).padStart(2, '0')}/${this.filtroAno}`;
  }

  private limparLista(): void {
    this.logs = [];
    this.totalElements = 0;
    this.totalPages = 0;
    this.currentPage = 0;
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

  totalDoDiaChip(dia: number): number {
    return this.periodos.find(
      (p) => p.ano === this.filtroAno && p.mes === this.filtroMes && p.dia === dia
    )?.total ?? 0;
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
        this.limparFiltros();
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
        this.limparFiltros();
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
