package br.softsistem.Gerenciamento_de_estoque.dto.entregaDto;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;

public class EntregaRequestDto {
    private Long produtoId;
    private int quantidade;
    private Consumidor consumidor;

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

    public Consumidor getConsumidor() {
        return consumidor;
    }

    public void setConsumidor(Consumidor consumidor) {
        this.consumidor = consumidor;
    }
}

