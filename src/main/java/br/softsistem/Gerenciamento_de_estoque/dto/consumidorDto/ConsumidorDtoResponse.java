package br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;

public record ConsumidorDtoResponse(
        Long id,
        String nome,
        String cpf,
        String endereco,
        Long orgId
) {
    public static ConsumidorDtoResponse fromEntity(Consumidor consumidor) {
        return new ConsumidorDtoResponse(
                consumidor.getId(),
                consumidor.getNome(),
                consumidor.getCpf(),
                consumidor.getEndereco(),
                consumidor.getOrg() != null ? consumidor.getOrg().getId() : null
        );
    }
}
