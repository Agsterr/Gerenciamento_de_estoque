// src/app/models/consumer-paged-response.model.ts
import { Consumer } from './consumer.model';

export interface ConsumerPagedResponse {
  content: Consumer[];
  page: {
    size:          number;
    number:        number;         // página atual (0-based)
    totalElements: number;
    totalPages:    number;
  };
}
