package br.softsistem.Gerenciamento_de_estoque.dto.erp;

public record ContagemInventarioItemDto(
        Long id, Long produtoId, String produtoNome,
        Integer quantidadeSistema, Integer quantidadeContada, Integer diferenca
) {}
