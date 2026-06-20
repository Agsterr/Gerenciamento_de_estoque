package br.softsistem.Gerenciamento_de_estoque.dto.erp;

public record EstoqueRetiradaDto(
        Long produtoId,
        String produtoNome,
        int quantidade,
        String depositoNome
) {}
