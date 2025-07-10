export interface MovimentacaoProdutoDto {
  id: number;
  produtoId: number;
  tipo: TipoMovimentacao; // Usando o enum TipoMovimentacao
  quantidade: number;
  dataHora: string; // Data e hora da movimentação
  orgId: number; // ID da organização
  nomeProduto: string; // Nome do produto
}

export enum TipoMovimentacao {
  ENTRADA = 'ENTRADA',  // Movimento de entrada
  SAIDA = 'SAIDA'      // Movimento de saída
}
