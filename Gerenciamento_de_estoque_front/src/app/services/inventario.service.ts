import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface ContagemItem {
  id: number; produtoId: number; produtoNome: string;
  quantidadeSistema: number; quantidadeContada?: number; diferenca?: number;
}
export interface ContagemInventario {
  id: number; depositoId: number; depositoNome: string; status: string;
  observacao?: string; criadoEm?: string; finalizadoEm?: string; itens: ContagemItem[];
}

@Injectable({ providedIn: 'root' })
export class InventarioService {
  private url = `${environment.apiUrl}/inventario/contagens`;
  constructor(private http: HttpClient) {}
  listar(page = 0, size = 20): Observable<{ content: ContagemInventario[]; totalElements: number }> {
    return this.http.get<{ content: ContagemInventario[]; totalElements: number }>(`${this.url}?page=${page}&size=${size}`);
  }
  buscar(id: number): Observable<ContagemInventario> { return this.http.get<ContagemInventario>(`${this.url}/${id}`); }
  iniciar(depositoId: number, observacao?: string): Observable<ContagemInventario> {
    return this.http.post<ContagemInventario>(this.url, { depositoId, observacao });
  }
  registrarItens(id: number, updates: { itemId: number; quantidadeContada: number }[]): Observable<ContagemInventario> {
    return this.http.put<ContagemInventario>(`${this.url}/${id}/itens`, updates);
  }
  finalizar(id: number): Observable<ContagemInventario> { return this.http.post<ContagemInventario>(`${this.url}/${id}/finalizar`, {}); }
  cancelar(id: number): Observable<ContagemInventario> { return this.http.post<ContagemInventario>(`${this.url}/${id}/cancelar`, {}); }
}
