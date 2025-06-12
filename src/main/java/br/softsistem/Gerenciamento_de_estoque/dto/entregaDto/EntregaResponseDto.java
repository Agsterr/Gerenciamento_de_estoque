package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import java.time.LocalDateTime;

public record EntregaResponseDto(
        Long id,
        String nomeConsumidor,
        String nomeProduto,
        String nomeEntregador,
        Integer quantidade,
        LocalDateTime horarioEntrega,
        Long produtoId,   // Novo campo
        Long consumidorId // Novo campo
) {
    // Método estático para conversão de entidade para DTO
    public static EntregaResponseDto fromEntity(Entrega entrega) {
        return new EntregaResponseDto(
                entrega.getId(),
                entrega.getConsumidor().getNome(),
                entrega.getProduto().getNome(),
                entrega.getEntregador().getUsername(),
                entrega.getQuantidade(),
                entrega.getHorarioEntrega(),
                entrega.getProduto().getId(),  // Atribuindo o produtoId
                entrega.getConsumidor().getId()  // Atribuindo o consumidorId
        );
    }
}
