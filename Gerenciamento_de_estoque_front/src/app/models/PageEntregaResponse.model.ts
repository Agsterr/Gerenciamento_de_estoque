import { Entrega } from './entrega.model';

export interface PageEntregaResponse {
  content: Entrega[]; // Lista de entregas (itens na página)
  totalPages: number;  // Total de páginas disponíveis
  totalElements: number;  // Total de elementos (entregas) em toda a coleção
  size: number;  // Quantidade de itens por página
  number: number;  // Número da página atual (0-based, ou seja, começa de 0)
}


