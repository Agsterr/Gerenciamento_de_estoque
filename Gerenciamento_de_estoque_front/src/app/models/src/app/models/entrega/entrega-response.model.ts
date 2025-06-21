export interface EntregaResponse {
  id: number;
  nomeConsumidor: string;
  nomeProduto: string;
  nomeEntregador: string;
  quantidade: number;
  horarioEntrega: string;    // Horário de entrega em formato de string ISO
  produtoId: number;         // ID do produto
  consumidorId: number;      // ID do consumidor
}
