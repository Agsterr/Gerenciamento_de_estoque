// src/app/models/EntregaRequest.model.ts

export interface EntregaRequest {
  produtoId: number;
  quantidade: number;
  consumidorId: number;
  consumidor: {
    nome: string;
    cpf: string;
  };
}

