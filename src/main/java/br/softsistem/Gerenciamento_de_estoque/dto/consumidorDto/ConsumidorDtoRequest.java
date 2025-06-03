package br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Org;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConsumidorDtoRequest(
        Long id,

        @NotNull(message = "Nome não pode ser nulo")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String nome,

        @NotNull(message = "CPF não pode ser nulo")
        @Pattern(regexp = "\\d{11}", message = "CPF inválido. Deve ter 11 dígitos.")
        String cpf,

        @NotNull(message = "Endereço não pode ser nulo")
        String endereco,

        @NotNull(message = "ID da organização não pode ser nulo")
        Long orgId
) {

    // Converte de DTO para Entidade
    public Consumidor toEntity() {
        Consumidor consumidor = new Consumidor();
        consumidor.setId(this.id);
        consumidor.setNome(this.nome);
        consumidor.setCpf(this.cpf);
        consumidor.setEndereco(this.endereco);

        // cria um Org apenas com o ID, para relacionar
        if (this.orgId != null) {
            Org o = new Org();
            o.setId(this.orgId);
            consumidor.setOrg(o);
        }
        return consumidor;
    }

    // Converte de Entidade para DTO
    public static ConsumidorDtoRequest fromEntity(Consumidor consumidor) {
        return new ConsumidorDtoRequest(
                consumidor.getId(),
                consumidor.getNome(),
                consumidor.getCpf(),
                consumidor.getEndereco(),
                consumidor.getOrg() != null ? consumidor.getOrg().getId() : null
        );
    }
}
