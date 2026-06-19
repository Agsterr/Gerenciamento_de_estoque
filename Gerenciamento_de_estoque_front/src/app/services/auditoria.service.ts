import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface AuditoriaLog {
  id: number; entidade: string; entidadeId?: number; acao: string;
  usuario?: string; detalhes?: string; criadoEm: string;
}

@Injectable({ providedIn: 'root' })
export class AuditoriaService {
  private url = `${environment.apiUrl}/auditoria`;
  constructor(private http: HttpClient) {}
  listar(page = 0, size = 30, entidade?: string): Observable<{ content: AuditoriaLog[]; totalElements: number }> {
    let q = `${this.url}?page=${page}&size=${size}`;
    if (entidade) q += `&entidade=${entidade}`;
    return this.http.get<{ content: AuditoriaLog[]; totalElements: number }>(q);
  }
}
