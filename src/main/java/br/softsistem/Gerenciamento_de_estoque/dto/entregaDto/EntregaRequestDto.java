package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;

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

    private LocalDateTime horarioEntrega;  // Horário da entrega (opcional)

    // ======================
    // GETTERS E SETTERS
    // ======================
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

    public LocalDateTime getHorarioEntrega() {
        return horarioEntrega;
    }

    public void setHorarioEntrega(LocalDateTime horarioEntrega) {
        this.horarioEntrega = horarioEntrega;
    }

    // ==================================
    // MÉTODO PARA CONVERTER EM ENTIDADE
    // ==================================
    /**
     * Converte este DTO em entidade Entrega, mas ainda não seta o entregador.
     * O entregador será definido no Service, usando o usuário atualmente logado.
     */
    public Entrega toEntity(Produto produto, Consumidor consumidor) {
        Entrega entrega = new Entrega();
        entrega.setProduto(produto);
        entrega.setConsumidor(consumidor);
        entrega.setQuantidade(this.quantidade);
        entrega.setHorarioEntrega(
                horarioEntrega != null
                        ? horarioEntrega
                        : LocalDateTime.now()
        );
        return entrega;
    }
}
