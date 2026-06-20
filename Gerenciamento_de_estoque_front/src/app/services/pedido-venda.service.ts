import { Injectable } from '@angular/core';

import { HttpClient } from '@angular/common/http';

import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';



export interface PedidoVendaItem {

  id?: number;

  produtoId: number;

  produtoNome?: string;

  quantidade: number;

  precoUnitario: number;

  subtotal?: number;

}



export interface PedidoVenda {

  id: number;

  numero: string;

  tipoPedido: 'VENDA' | 'INTERNO';

  consumidorId?: number;

  consumidorNome?: string;

  funcionarioId?: number;

  funcionarioNome?: string;

  vendedorId: number;

  vendedorNome?: string;

  dataHora: string;

  formaPagamento?: string;

  condicaoPagamento?: string;

  status: string;

  observacao?: string;

  valorTotal: number;

  itens: PedidoVendaItem[];

}



export const TIPOS_PEDIDO = [

  { value: 'VENDA', label: 'Venda (cliente)' },

  { value: 'INTERNO', label: 'Retirada interna (funcionário)' },

];



export const FORMAS_PAGAMENTO = [

  { value: 'DINHEIRO', label: 'Dinheiro' },

  { value: 'PIX', label: 'PIX' },

  { value: 'CARTAO_CREDITO', label: 'Cartão de crédito' },

  { value: 'CARTAO_DEBITO', label: 'Cartão de débito' },

  { value: 'BOLETO', label: 'Boleto' },

  { value: 'TRANSFERENCIA', label: 'Transferência' },

  { value: 'OUTRO', label: 'Outro' },

];



export const CONDICOES_PAGAMENTO = [

  { value: 'A_VISTA', label: 'À vista' },

  { value: 'PARCELADO_30', label: '30 dias' },

  { value: 'PARCELADO_60', label: '60 dias' },

  { value: 'PARCELADO_90', label: '90 dias' },

  { value: 'CREDIARIO', label: 'Crediário' },

];



export interface EstoqueRetirada {
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  depositoNome: string;
}



@Injectable({ providedIn: 'root' })

export class PedidoVendaService {

  private url = `${environment.apiUrl}/pedidos-venda`;



  constructor(private http: HttpClient) {}



  listar(page = 0, size = 20): Observable<{ content: PedidoVenda[]; totalElements: number }> {

    return this.http.get<{ content: PedidoVenda[]; totalElements: number }>(`${this.url}?page=${page}&size=${size}`);

  }



  estoqueRetirada(): Observable<EstoqueRetirada[]> {
    return this.http.get<EstoqueRetirada[]>(`${this.url}/estoque-retirada`);
  }



  criar(data: {

    tipoPedido?: 'VENDA' | 'INTERNO';

    consumidorId?: number;

    funcionarioId?: number;

    vendedorId?: number;

    dataHora?: string;

    formaPagamento?: string;

    condicaoPagamento?: string;

    observacao?: string;

    confirmar?: boolean;

    itens: PedidoVendaItem[];

  }): Observable<PedidoVenda> {

    return this.http.post<PedidoVenda>(this.url, data);

  }



  confirmar(id: number): Observable<PedidoVenda> {

    return this.http.post<PedidoVenda>(`${this.url}/${id}/confirmar`, {});

  }



  cancelar(id: number): Observable<PedidoVenda> {

    return this.http.post<PedidoVenda>(`${this.url}/${id}/cancelar`, {});

  }

}


