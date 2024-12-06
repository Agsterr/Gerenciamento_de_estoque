package br.softsistem.Gerenciamento_de_estoque.dto;

import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoDto(String nome, BigDecimal preco, Long id, String descricao,
                         Categoria categoria, LocalDateTime dateTime, Integer quantidade) {

    public ProdutoDto (Produto produto){
        this(produto.getNome(),produto.getPreco(),produto.getId(),
                produto.getDescricao(),produto.getCategoria(),produto.getCriadoEm(),produto.getQuantidade());
    }
}
