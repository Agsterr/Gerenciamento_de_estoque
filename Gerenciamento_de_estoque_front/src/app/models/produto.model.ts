
// produto.model.ts
export class Produto {
  constructor(
    public id: number,
    public nome: string,
    public descricao: string,
    public preco: number,
    public quantidade: number,
    public quantidadeMinima: number,
    public categoriaId: number,  // Adicionando categoriaId
    public categoriaNome: string,  // Adicionando categoriaNome
    public orgId: number,
    public ativo: boolean,
    public criadoEm: string,  // Mantendo o criadoEm como data
    public status: string  // Adicionando o status
  ) {}
}


