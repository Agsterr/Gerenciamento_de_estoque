import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuditoriaService, AuditoriaLog } from '../services/auditoria.service';

@Component({
  selector: 'app-auditoria',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './auditoria.component.html',
  styleUrls: ['./auditoria.component.scss'],
})
export class AuditoriaComponent implements OnInit {
  logs: AuditoriaLog[] = [];
  filtroEntidade = '';
  mensagemErro = '';
  loading = false;
  currentPage = 0;
  pageSize = 30;
  totalElements = 0;
  totalPages = 0;

  entidadesSugeridas = [
    'PRODUTO', 'FORNECEDOR', 'DEPOSITO', 'PEDIDO_COMPRA',
    'INVENTARIO', 'USUARIO', 'ORGANIZACAO', 'ENTREGA',
  ];

  constructor(private auditoriaService: AuditoriaService) {}

  ngOnInit(): void {
    this.fetchLogs();
  }

  fetchLogs(page = 0): void {
    this.loading = true;
    const entidade = this.filtroEntidade.trim() || undefined;
    this.auditoriaService.listar(page, this.pageSize, entidade).subscribe({
      next: (resp) => {
        this.currentPage = page;
        this.totalElements = resp.totalElements;
        this.totalPages = Math.ceil(resp.totalElements / this.pageSize) || 1;
        this.logs = resp.content;
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.mensagemErro = 'Erro ao buscar logs de auditoria!';
        setTimeout(() => (this.mensagemErro = ''), 3000);
      },
    });
  }

  aplicarFiltro(): void {
    this.currentPage = 0;
    this.fetchLogs(0);
  }

  limparFiltro(): void {
    this.filtroEntidade = '';
    this.aplicarFiltro();
  }

  paginaAnterior(): void {
    if (this.currentPage > 0) this.fetchLogs(this.currentPage - 1);
  }

  proximaPagina(): void {
    if (this.currentPage + 1 < this.totalPages) this.fetchLogs(this.currentPage + 1);
  }

  irParaPagina(p: number): void {
    if (p >= 0 && p < this.totalPages) this.fetchLogs(p);
  }

  onPageSizeChange(): void {
    this.currentPage = 0;
    this.fetchLogs(0);
  }
}
