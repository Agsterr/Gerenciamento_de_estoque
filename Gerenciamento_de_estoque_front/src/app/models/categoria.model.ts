// src/app/models/categoria.model.ts
export class Categoria {
  constructor(
    public id: number,
    public nome: string,
    public descricao: string | null = null,
    public criadoEm: string = '',
    public orgId: number = 0
  ) {}
}
