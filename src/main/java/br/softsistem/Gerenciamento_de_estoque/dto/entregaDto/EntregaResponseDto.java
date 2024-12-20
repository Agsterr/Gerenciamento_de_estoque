package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.model.Entrega;

import java.time.LocalDateTime;

public class EntregaResponseDto {
    private Long id;
    private String nomeConsumidor;
    private String nomeProduto;
    private String nomeEntregador;
    private Integer quantidade;
    private LocalDateTime horarioEntrega;

    // Construtor
    public EntregaResponseDto(Long id, String nomeConsumidor, String nomeProduto, String nomeEntregador, Integer quantidade, LocalDateTime horarioEntrega) {
        this.id = id;
        this.nomeConsumidor = nomeConsumidor;
        this.nomeProduto = nomeProduto;
        this.nomeEntregador = nomeEntregador;
        this.quantidade = quantidade;
        this.horarioEntrega = horarioEntrega;
    }

    // Método estático para conversão
    public static EntregaResponseDto fromEntity(Entrega entrega) {
        return new EntregaResponseDto(
                entrega.getId(),
                entrega.getConsumidor().getNome(),
                entrega.getProduto().getNome(),
                entrega.getEntregador().getUsername(),
                entrega.getQuantidade(),
                entrega.getHorarioEntrega()
        );
    }
}