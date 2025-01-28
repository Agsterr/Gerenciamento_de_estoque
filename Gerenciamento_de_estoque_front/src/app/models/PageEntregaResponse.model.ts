// src/app/models/page-entrega-response.model.ts
import { Entrega } from './entrega.model';

export interface PageEntregaResponse {
  content: Entrega[]; // Lista de entregas
  totalPages: number; // Total de páginas disponíveis
  number: number; // Número da página atual (0-based)
}

