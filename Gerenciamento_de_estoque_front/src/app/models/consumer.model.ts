
// src/app/models/consumer.model.ts
export interface Consumer {
  id: number;
  nome: string;
  cpf: string;
  orgId: number | null;  // Permite que o orgId seja número ou null
  endereco: string;  // Adicionado o campo endereco
}
