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

@Injectable({ providedIn: 'root' })
export class AdminLoginLogsService {
  private url = `${environment.apiUrl}/admin/login-logs`;
  constructor(private http: HttpClient) {}

  listar(page = 0, size = 30): Observable<{ content: LoginLog[]; totalElements: number; totalPages: number }> {
    return this.http.get<{ content: LoginLog[]; totalElements: number; totalPages: number }>(
      `${this.url}?page=${page}&size=${size}&sort=dataHora,desc`
    );
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
