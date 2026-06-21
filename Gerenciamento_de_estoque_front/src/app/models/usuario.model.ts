export interface Usuario {
  id: number;
  username: string;
  email: string;
  ativo: boolean;
  orgId: number;
  roles: string[];
  senhaRegistrada?: string | null;
}
