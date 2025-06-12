// src/app/models/page-categoria-response.model.ts
import { Categoria } from './categoria.model';

export interface PageCategoriaResponse {
  content: Categoria[];
  page: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
}
