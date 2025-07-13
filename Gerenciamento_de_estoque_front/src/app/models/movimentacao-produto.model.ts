export interface MovimentacaoProdutoDto {
  id: number;
  produtoId: number;
  tipo: TipoMovimentacao; // Usando o enum TipoMovimentacao
  quantidade: number;
  dataHora: string; // Data e hora da movimentação
  orgId: number; // ID da organização
  nomeProduto: string; // Nome do produto
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
}

export enum TipoMovimentacao {
  ENTRADA = 'ENTRADA',  // Movimento de entrada
  SAIDA = 'SAIDA'      // Movimento de saída
}
