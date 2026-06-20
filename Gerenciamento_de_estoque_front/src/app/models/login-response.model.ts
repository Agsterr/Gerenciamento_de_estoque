
 // src/app/models/login-response.model.ts
export interface Role {
  id: number;
  nome: string;
  org: {
    id: number;
    nome: string;
    ativo: boolean;
  };
}

export interface LoginResponse {
  token: string;
  demo?: boolean;
  ephemeral?: boolean;
  roles?: Role[];
  userId?: number;
  username?: string;
}
 