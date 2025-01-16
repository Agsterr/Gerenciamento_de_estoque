

 // produto.model.ts

// Classe Entrada
export class Entrada {
    constructor(
      public id: number,
      public data: Date,
      public quantidade: number
    ) {}
  }
  
  // Classe Saída
  export class Saida {
    constructor(
      public id: number,
      public data: Date,
      public quantidade: number
    ) {}
  }
  
  // Classe Produto
  export class Produto {
    constructor(
      public id: number,
      public nome: string,
      public descricao: string,
      public preco: number,
      public quantidade: number,
      public entradas: Entrada[] = [], // Entradas associadas ao produto
      public saidas: Saida[] = [] // Saídas associadas ao produto
    ) {}
  }
  