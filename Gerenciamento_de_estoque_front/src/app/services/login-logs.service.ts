import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface LoginLog {
  id: number;
  usuarioId?: number;
  orgId?: number;
  orgNome?: string;
  username: string;
  ip?: string;
  userAgent?: string;
  dataHora: string;
  sucesso: boolean;
  detalhes?: string;
}

export interface LoginLogPeriodo {
  ano: number;
  mes: number;
  dia: number;
  total: number;
}

export interface LoginLogExportFile {
  filename: string;
  sizeBytes: number;
  createdAt: string;
  orgId?: number;
  periodoLabel: string;
  registros: number;
}

export interface LoginLogActionResult {
  filename?: string;
  registrosExportados: number;
  registrosApagados: number;
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class OrgLoginLogsService {
  private url = `${environment.apiUrl}/api/org/login-logs`;

  constructor(private http: HttpClient) {}

  listar(page = 0, size = 20, ano: number, mes: number, dia: number, ip?: string): Observable<PageResponse<LoginLog>> {
    const params = new URLSearchParams({
      page: String(page),
      size: String(size),
      sort: 'dataHora,desc',
      ano: String(ano),
      mes: String(mes),
      dia: String(dia),
    });
    if (ip) params.set('ip', ip);
    return this.http.get<PageResponse<LoginLog>>(`${this.url}?${params}`);
  }

  ips(ano: number, mes: number, dia: number): Observable<string[]> {
    const params = new URLSearchParams({
      ano: String(ano),
      mes: String(mes),
      dia: String(dia),
    });
    return this.http.get<string[]>(`${this.url}/ips?${params}`);
  }

  periodos(): Observable<LoginLogPeriodo[]> {
    return this.http.get<LoginLogPeriodo[]>(`${this.url}/periodos`);
  }

  exportar(ano: number, mes?: number, dia?: number): Observable<Blob> {
    const params = new URLSearchParams({ ano: String(ano) });
    if (mes != null) params.set('mes', String(mes));
    if (dia != null) params.set('dia', String(dia));
    return this.http.get(`${this.url}/export?${params}`, { responseType: 'blob' });
  }

  compactar(body: { ano: number; mes?: number; dia?: number }): Observable<LoginLogActionResult> {
    return this.http.post<LoginLogActionResult>(`${this.url}/compact`, body);
  }

  apagar(body: { ano: number; mes?: number; dia?: number; confirm: boolean }): Observable<LoginLogActionResult> {
    return this.http.post<LoginLogActionResult>(`${this.url}/delete`, body);
  }

  listarArquivos(): Observable<LoginLogExportFile[]> {
    return this.http.get<LoginLogExportFile[]>(`${this.url}/arquivos`);
  }

  baixarArquivo(filename: string): Observable<Blob> {
    return this.http.get(`${this.url}/arquivos/${encodeURIComponent(filename)}`, { responseType: 'blob' });
  }

  apagarArquivo(filename: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/arquivos/${encodeURIComponent(filename)}`);
  }
}

@Injectable({ providedIn: 'root' })
export class SugestaoService {
  private url = `${environment.apiUrl}/api/sugestoes`;

  constructor(private http: HttpClient) {}

  enviar(texto: string): Observable<Sugestao> {
    return this.http.post<Sugestao>(this.url, { texto });
  }

  listarOrg(page = 0, size = 30): Observable<PageResponse<Sugestao>> {
    return this.http.get<PageResponse<Sugestao>>(`${this.url}?page=${page}&size=${size}&sort=criadoEm,desc`);
  }
}

export interface Sugestao {
  id: number;
  orgId?: number;
  orgNome?: string;
  usuarioId?: number;
  username: string;
  texto: string;
  criadoEm: string;
  status: string;
}

@Injectable({ providedIn: 'root' })
export class AdminSugestaoService {
  private url = `${environment.apiUrl}/admin/sugestoes`;

  constructor(private http: HttpClient) {}

  listar(page = 0, size = 30): Observable<PageResponse<Sugestao>> {
    return this.http.get<PageResponse<Sugestao>>(`${this.url}?page=${page}&size=${size}&sort=criadoEm,desc`);
  }

  atualizarStatus(id: number, status: string): Observable<Sugestao> {
    return this.http.patch<Sugestao>(`${this.url}/${id}/status`, { status });
  }
}
