package br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;

public record ConsumidorDtoRequest(Long id, String nome, String cpf) {

    // Converte de DTO para Entidade
    public Consumidor toEntity() {
        Consumidor consumidor = new Consumidor();
        consumidor.setId(this.id);
        consumidor.setNome(this.nome);
        consumidor.setCpf(this.cpf);
        return consumidor;
    }

    // Converte de Entidade para DTO
    public static ConsumidorDtoRequest fromEntity(Consumidor consumidor) {
        return new ConsumidorDtoRequest(consumidor.getId(), consumidor.getNome(), consumidor.getCpf());
    }
}

