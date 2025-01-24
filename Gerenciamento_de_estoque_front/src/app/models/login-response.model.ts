// src/app/models/login-response.model.ts
export interface LoginResponse {
    token: string; // Token JWT retornado pelo backend
    userId?: number; // ID do usuário autenticado (opcional)
    username?: string; // Nome do usuário (opcional)
  }
  