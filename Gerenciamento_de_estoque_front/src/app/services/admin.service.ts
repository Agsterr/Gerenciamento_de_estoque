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

@Injectable({ providedIn: 'root' })
export class AdminLoginLogsService {
  private url = `${environment.apiUrl}/admin/login-logs`;
  constructor(private http: HttpClient) {}

  listar(page = 0, size = 30, ano?: number, mes?: number, dia?: number, orgId?: number): Observable<{ content: LoginLog[]; totalElements: number; totalPages: number }> {
    const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'dataHora,desc' });
    if (ano != null) params.set('ano', String(ano));
    if (mes != null) params.set('mes', String(mes));
    if (dia != null) params.set('dia', String(dia));
    if (orgId != null) params.set('orgId', String(orgId));
    return this.http.get<{ content: LoginLog[]; totalElements: number; totalPages: number }>(`${this.url}?${params}`);
  }

  periodos(orgId?: number): Observable<LoginLogPeriodo[]> {
    const q = orgId != null ? `?orgId=${orgId}` : '';
    return this.http.get<LoginLogPeriodo[]>(`${this.url}/periodos${q}`);
  }

  exportar(ano: number, mes?: number, dia?: number, orgId?: number): Observable<Blob> {
    const params = new URLSearchParams({ ano: String(ano) });
    if (mes != null) params.set('mes', String(mes));
    if (dia != null) params.set('dia', String(dia));
    if (orgId != null) params.set('orgId', String(orgId));
    return this.http.get(`${this.url}/export?${params}`, { responseType: 'blob' });
  }

  compactar(body: { ano: number; mes?: number; dia?: number; orgId?: number }): Observable<LoginLogActionResult> {
    return this.http.post<LoginLogActionResult>(`${this.url}/compact`, body);
  }

  apagar(body: { ano: number; mes?: number; dia?: number; orgId?: number; confirm: boolean }): Observable<LoginLogActionResult> {
    return this.http.post<LoginLogActionResult>(`${this.url}/delete`, body);
  }

  listarArquivos(orgId?: number): Observable<LoginLogExportFile[]> {
    const q = orgId != null ? `?orgId=${orgId}` : '';
    return this.http.get<LoginLogExportFile[]>(`${this.url}/arquivos${q}`);
  }

  baixarArquivo(filename: string): Observable<Blob> {
    return this.http.get(`${this.url}/arquivos/${encodeURIComponent(filename)}`, { responseType: 'blob' });
  }

  apagarArquivo(filename: string): Observable<void> {
    return this.http.delete<void>(`${this.url}/arquivos/${encodeURIComponent(filename)}`);
  }
}

@Injectable({ providedIn: 'root' })
export class AdminWebhookService {
  private url = `${environment.apiUrl}/admin/webhooks/failed`;
  constructor(private http: HttpClient) {}
  listar(): Observable<any[]> { return this.http.get<any[]>(this.url); }
  reprocessar(eventId: string): Observable<any> { return this.http.post<any>(`${this.url}/${eventId}/reprocess`, {}); }
  reprocessarLote(eventType?: string): Observable<any> {
    const q = eventType ? `?eventType=${eventType}` : '';
    return this.http.post<any>(`${this.url}/reprocess/batch${q}`, {});
  }
}

@Injectable({ providedIn: 'root' })
export class AdminCacheService {
  private url = `${environment.apiUrl}/api/cache`;
  constructor(private http: HttpClient) {}
  info(): Observable<any> { return this.http.get<any>(`${this.url}/info`); }
  stats(): Observable<any> { return this.http.get<any>(`${this.url}/stats`); }
  limpar(cacheName: string): Observable<string> { return this.http.delete(`${this.url}/${cacheName}`, { responseType: 'text' }); }
  limparTodos(): Observable<string> { return this.http.delete(`${this.url}/all`, { responseType: 'text' }); }
}

export interface AdminUserSubscription {
  userId: number;
  username: string;
  email?: string;
  ativo?: boolean;
  bypassSubscription?: boolean;
  subscriptionId?: number;
  status?: string;
  trialEnd?: string;
  accessBlocked?: boolean;
  inTrial?: boolean;
  paymentMode?: string;
}

export interface AdminOrgSubscription {
  orgId: number;
  orgNome: string;
  orgAtivo?: boolean;
  totalUsuarios: number;
  usuariosComBypass: number;
  statusResumo: string;
  trialEndMaisRecente?: string;
  usuarios: AdminUserSubscription[];
}

@Injectable({ providedIn: 'root' })
export class AdminSubscriptionAdminService {
  private url = `${environment.apiUrl}/admin/subscriptions`;
  private orgUrl = `${environment.apiUrl}/api/orgs`;
  constructor(private http: HttpClient) {}

  overview(): Observable<AdminOrgSubscription[]> {
    return this.http.get<AdminOrgSubscription[]>(`${this.url}/overview`);
  }

  setBypass(userId: number, bypass: boolean): Observable<AdminUserSubscription> {
    return this.http.patch<AdminUserSubscription>(`${this.url}/users/${userId}/bypass`, { bypass });
  }

  extendTrialUser(userId: number, days: number): Observable<AdminUserSubscription> {
    return this.http.patch<AdminUserSubscription>(`${this.url}/users/${userId}/extend-trial`, { days });
  }

  extendTrialOrg(orgId: number, days: number): Observable<AdminUserSubscription[]> {
    return this.http.patch<AdminUserSubscription[]>(`${this.url}/orgs/${orgId}/extend-trial`, { days });
  }

  extendTrialUserDate(userId: number, trialEnd: string): Observable<AdminUserSubscription> {
    return this.http.patch<AdminUserSubscription>(`${this.url}/users/${userId}/extend-trial`, { trialEnd });
  }

  forcePayUser(userId: number): Observable<AdminUserSubscription> {
    return this.http.patch<AdminUserSubscription>(`${this.url}/users/${userId}/force-pay`, {});
  }

  forcePayOrg(orgId: number): Observable<AdminUserSubscription[]> {
    return this.http.patch<AdminUserSubscription[]>(`${this.url}/orgs/${orgId}/force-pay`, {});
  }

  ativarOrg(orgId: number): Observable<void> {
    return this.http.put<void>(`${this.orgUrl}/${orgId}/ativar`, {});
  }

  desativarOrg(orgId: number): Observable<void> {
    return this.http.put<void>(`${this.orgUrl}/${orgId}/desativar`, {});
  }
}

export interface AdminUser {
  id: number;
  username: string;
  email?: string;
  ativo?: boolean;
  orgId: number;
  orgNome: string;
  bypassSubscription?: boolean;
  roles: string[];
}

export interface AdminOrgSummary {
  orgId: number;
  orgNome: string;
  orgAtivo?: boolean;
  ephemeral?: boolean;
  maxDispositivos: number;
  totalUsuarios: number;
  dispositivosAprovados: number;
}

export interface AdminUserCreated {
  user: AdminUser;
  temporaryPassword: string;
}

@Injectable({ providedIn: 'root' })
export class AdminUsersService {
  private url = `${environment.apiUrl}/admin/users`;
  constructor(private http: HttpClient) {}

  listar(page = 0, size = 30, orgId?: number): Observable<{ content: AdminUser[]; totalElements: number; totalPages: number }> {
    const params = new URLSearchParams({ page: String(page), size: String(size), sort: 'username,asc' });
    if (orgId != null) params.set('orgId', String(orgId));
    return this.http.get<{ content: AdminUser[]; totalElements: number; totalPages: number }>(`${this.url}?${params}`);
  }

  listarOrgs(): Observable<AdminOrgSummary[]> {
    return this.http.get<AdminOrgSummary[]>(`${this.url}/orgs`);
  }

  criar(body: { username: string; email?: string; orgId: number; roles?: string[]; bypassSubscription?: boolean }): Observable<AdminUserCreated> {
    return this.http.post<AdminUserCreated>(this.url, body);
  }

  ativar(userId: number): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.url}/${userId}/ativar`, {});
  }

  desativar(userId: number): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.url}/${userId}/desativar`, {});
  }

  setMaxDispositivos(orgId: number, maxDispositivos: number): Observable<{ id: number; maxDispositivos: number }> {
    return this.http.patch<{ id: number; maxDispositivos: number }>(`${this.url}/orgs/${orgId}/max-dispositivos`, { maxDispositivos });
  }

  resetSenha(userId: number): Observable<AdminUserCreated> {
    return this.http.post<AdminUserCreated>(`${this.url}/${userId}/reset-password`, {});
  }

  setBypass(userId: number, bypass: boolean): Observable<AdminUser> {
    return this.http.patch<AdminUser>(`${this.url}/${userId}/bypass`, { bypass });
  }
}

export interface PesquisaPrecoResposta {
  id: number;
  usuarioId?: number;
  orgId?: number;
  orgNome?: string;
  username: string;
  valorMin: number;
  valorMax: number;
  comentario?: string;
  criadoEm?: string;
  atualizadoEm?: string;
}

export interface PesquisaPrecoStats {
  totalRespostas: number;
  mediaValorMin?: number;
  mediaValorMax?: number;
  medianaValorMin?: number;
  medianaValorMax?: number;
  precoJustoSugerido?: number;
  analiseTexto?: string;
  respostas: PesquisaPrecoResposta[];
}

@Injectable({ providedIn: 'root' })
export class AdminPesquisaPrecoService {
  private url = `${environment.apiUrl}/admin/pesquisa-preco`;
  constructor(private http: HttpClient) {}

  stats(): Observable<PesquisaPrecoStats> {
    return this.http.get<PesquisaPrecoStats>(`${this.url}/stats`);
  }
}
