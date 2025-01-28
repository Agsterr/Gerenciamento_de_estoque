// src/app/models/entrega.model.ts


export interface Entrega {
  id: number; // ID único da entrega
  nomeConsumidor: string; // Nome do consumidor
  nomeProduto: string; // Nome do produto
  nomeEntregador: string; // Nome do entregador
  quantidade: number; // Quantidade do produto entregue
  horarioEntrega: string; // Horário da entrega
}
