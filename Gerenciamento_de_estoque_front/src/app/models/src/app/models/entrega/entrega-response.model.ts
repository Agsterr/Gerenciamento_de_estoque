// src/app/models/entrega-response.model.ts
export interface EntregaResponse {
  id: number;
  nomeConsumidor: string;
  nomeProduto: string;
  nomeEntregador: string;
  quantidade: number;
  horarioEntrega: string;
  produtoId: number;    
  consumidorId: number;  
}
