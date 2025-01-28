// src/app/models/entrega-request.model.ts

export interface EntregaRequest {
  produtoId: number | undefined; // ID do produto (pode ser undefined caso não esteja preenchido)
  quantidade: number | undefined; // Quantidade (pode ser undefined caso não esteja preenchido)
  consumidor: { 
    nome: string; 
    cpf: string; 
    endereco: string; 
  }; // Dados do consumidor
}
 