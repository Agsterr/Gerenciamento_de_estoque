package br.softsistem.Gerenciamento_de_estoque.dto.dashboard;

import java.math.BigDecimal;

public record ProdutoRankingDto(String nome, long quantidade, BigDecimal valor) {}
