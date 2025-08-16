package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class EntregaRequestDto {

    @NotNull(message = "O ID do produto não pode ser nulo.")
    private Long produtoId;

    @Min(value = 1, message = "A quantidade deve ser maior que zero.")
    private int quantidade;

    @NotNull(message = "O ID do consumidor não pode ser nulo.")
    private Long consumidorId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime horarioEntrega; // opcional; se null, o Service define

    public Long getProdutoId() { return produtoId; }
    public void setProdutoId(Long produtoId) { this.produtoId = produtoId; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
    public Long getConsumidorId() { return consumidorId; }
    public void setConsumidorId(Long consumidorId) { this.consumidorId = consumidorId; }
    public LocalDateTime getHorarioEntrega() { return horarioEntrega; }
    public void setHorarioEntrega(LocalDateTime horarioEntrega) { this.horarioEntrega = horarioEntrega; }

    /**
     * Converte em entidade sem definir entregador nem timezone/data default.
     * O Service injeta entregador e resolve data (org timezone) se null.
     */
    public Entrega toEntity(Produto produto, Consumidor consumidor) {
        Entrega entrega = new Entrega();
        entrega.setProduto(produto);
        entrega.setConsumidor(consumidor);
        entrega.setQuantidade(this.quantidade);
        entrega.setHorarioEntrega(this.horarioEntrega); // pode ser null -> Service resolve
        return entrega;
    }
}
