package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

public record EntregaComAvisoResponseDto(
        EntregaResponseDto entrega,
        String avisoEstoqueBaixo
) {}
