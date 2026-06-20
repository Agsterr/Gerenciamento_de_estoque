package br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto;

public record CorrecaoMovimentacaoResultado(
        MovimentacaoProdutoDto movimentacaoOriginal,
        MovimentacaoProdutoDto movimentacaoCorrecao
) {}
