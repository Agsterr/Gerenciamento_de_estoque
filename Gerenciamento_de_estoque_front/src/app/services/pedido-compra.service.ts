import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface PedidoCompraItem { id?: number; produtoId: number; produtoNome?: string; quantidade: number; precoUnitario: number; subtotal?: number; }
export interface PedidoCompra {
  id: number; numero: string; fornecedorId: number; fornecedorNome?: string;
  depositoId?: number; depositoNome?: string; status: string; observacao?: string;
  valorTotal: number; itens: PedidoCompraItem[];
}

@Injectable({ providedIn: 'root' })
export class PedidoCompraService {
  private url = `${environment.apiUrl}/pedidos-compra`;
  constructor(private http: HttpClient) {}
  listar(page = 0, size = 20): Observable<{ content: PedidoCompra[]; totalElements: number }> {
    return this.http.get<{ content: PedidoCompra[]; totalElements: number }>(`${this.url}?page=${page}&size=${size}`);
  }
  buscar(id: number): Observable<PedidoCompra> { return this.http.get<PedidoCompra>(`${this.url}/${id}`); }
  criar(data: { fornecedorId: number; depositoId?: number; observacao?: string; itens: PedidoCompraItem[] }): Observable<PedidoCompra> {
    return this.http.post<PedidoCompra>(this.url, data);
  }
  receber(id: number): Observable<PedidoCompra> { return this.http.post<PedidoCompra>(`${this.url}/${id}/receber`, {}); }
  cancelar(id: number): Observable<PedidoCompra> { return this.http.post<PedidoCompra>(`${this.url}/${id}/cancelar`, {}); }
}
