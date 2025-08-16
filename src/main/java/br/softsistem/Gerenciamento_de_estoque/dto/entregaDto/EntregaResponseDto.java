package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record EntregaResponseDto(
        Long id,
        String nomeConsumidor,
        String nomeProduto,
        String nomeEntregador,
        Integer quantidade,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime horarioEntrega,
        Long produtoId,
        Long consumidorId
) {
    public static EntregaResponseDto fromEntity(Entrega e) {
        var consumidor = e.getConsumidor();
        var produto    = e.getProduto();
        var entregador = e.getEntregador();

        return new EntregaResponseDto(
                e.getId(),
                consumidor != null ? consumidor.getNome() : null,
                produto != null ? produto.getNome() : null,
                entregador != null ? entregador.getUsername() : null,
                e.getQuantidade(),
                e.getHorarioEntrega(),
                produto != null ? produto.getId() : null,
                consumidor != null ? consumidor.getId() : null
        );
    }

    // Getters estilo JavaBean para compatibilidade com motores que usam Introspector (ex: JasperReports)
    public Long getId() { return id; }
    public String getNomeConsumidor() { return nomeConsumidor; }
    public String getNomeProduto() { return nomeProduto; }
    public String getNomeEntregador() { return nomeEntregador; }
    public Integer getQuantidade() { return quantidade; }
    public LocalDateTime getHorarioEntrega() { return horarioEntrega; }
    public Long getProdutoId() { return produtoId; }
    public Long getConsumidorId() { return consumidorId; }
}
