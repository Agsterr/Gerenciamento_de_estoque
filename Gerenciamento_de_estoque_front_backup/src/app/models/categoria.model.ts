export class Categoria {
    constructor(
      public id: number,
      public nome: string,
      public descricao: string | null,
      public criadoEm: string
    ) {}
  }
  