// src/app/models/page-entrega-response.model.ts
import { EntregaResponse } from '../entrega/entrega-response.model';

export interface PageEntregaResponse {
  content: EntregaResponse[];       // Lista de entregas
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}