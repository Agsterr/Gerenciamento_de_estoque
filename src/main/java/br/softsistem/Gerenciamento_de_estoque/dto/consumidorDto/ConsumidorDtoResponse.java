package br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;

public record ConsumidorDtoResponse(Long id, String nome, String cpf) {

    // Converte de Entidade para DTO
    public static ConsumidorDtoResponse fromEntity(Consumidor consumidor) {
        return new ConsumidorDtoResponse(consumidor.getId(), consumidor.getNome(), consumidor.getCpf());
    }
}
