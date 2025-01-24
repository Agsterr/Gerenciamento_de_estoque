

 // produto.model.ts


 export class Entrada {
  constructor(
    public id: number,
    public data: Date,
    public quantidade: number
  ) {}
}

export class Saida {
  constructor(
    public id: number,
    public data: Date,
    public quantidade: number
  ) {}
}

export class Categoria {
  constructor(
    public id: number,
    public nome: string,
    public descricao: string | null,
    public criadoEm: string
  ) {}
}

export class Produto {
  constructor(
    public id: number,
    public nome: string,
    public descricao: string,
    public preco: number,
    public quantidade: number,
    public quantidadeMinima: number,
    public categoria: Categoria, // Usa a classe Categoria para validação
    public dateTime: string,
    public entradas: Entrada[] = [],
    public saidas: Saida[] = []
  ) {}

  // Métodos auxiliares
  isEstoqueBaixo(): boolean {
    return this.quantidade <= this.quantidadeMinima;
  }

  adicionarEntrada(entrada: Entrada): void {
    this.entradas.push(entrada);
    this.quantidade += entrada.quantidade;
  }

  adicionarSaida(saida: Saida): void {
    if (saida.quantidade > this.quantidade) {
      throw new Error('Quantidade insuficiente em estoque.');
    }
    this.saidas.push(saida);
    this.quantidade -= saida.quantidade;
  }
}
 