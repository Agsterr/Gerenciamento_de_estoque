package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public class EntregaRequestDto {

    @NotNull(message = "O ID do produto não pode ser nulo.")
    private Long produtoId;      // ID do Produto

    @Min(value = 1, message = "A quantidade deve ser maior que zero.")
    private int quantidade;      // Quantidade de produtos

    @NotNull(message = "O ID do consumidor não pode ser nulo.")
    private Long consumidorId;   // ID do Consumidor

    @NotNull(message = "O ID do entregador não pode ser nulo.")
    private Long entregadorId;   // ID do Entregador

    private LocalDateTime horarioEntrega;  // Horário da entrega

    // Getters e Setters
    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public Long getConsumidorId() {
        return consumidorId;
    }

    public void setConsumidorId(Long consumidorId) {
        this.consumidorId = consumidorId;
    }

    public Long getEntregadorId() {
        return entregadorId;
    }

    public void setEntregadorId(Long entregadorId) {
        this.entregadorId = entregadorId;
    }

    public LocalDateTime getHorarioEntrega() {
        return horarioEntrega;
    }

    public void setHorarioEntrega(LocalDateTime horarioEntrega) {
        this.horarioEntrega = horarioEntrega;
    }

    // Método para converter o DTO em uma entidade Entrega
    public Entrega toEntity(Produto produto, Consumidor consumidor, Usuario entregador) {
        Entrega entrega = new Entrega();
        entrega.setProduto(produto);
        entrega.setConsumidor(consumidor);
        entrega.setEntregador(entregador);
        entrega.setQuantidade(this.quantidade);
        entrega.setHorarioEntrega(horarioEntrega != null ? horarioEntrega : LocalDateTime.now()); // Se não passar horário, pega o horário atual
        return entrega;
    }
}
