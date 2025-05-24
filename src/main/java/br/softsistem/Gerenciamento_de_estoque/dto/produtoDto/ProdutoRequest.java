package br.softsistem.Gerenciamento_de_estoque.dto.produtoDto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ProdutoRequest {

    @NotNull(message = "O nome do produto é obrigatório.")
    @Size(min = 1, max = 100, message = "O nome deve ter entre 1 e 100 caracteres.")
    private String nome;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
    private String descricao;

    @NotNull(message = "O preço é obrigatório.")
    @DecimalMin(value = "0.00", inclusive = false, message = "O preço deve ser maior que zero.")
    @Digits(integer = 10, fraction = 2, message = "O preço deve ser um valor decimal válido.")
    private BigDecimal preco;

    @NotNull(message = "A quantidade é obrigatória.")
    @Min(value = 0, message = "A quantidade não pode ser negativa.")
    private Integer quantidade;

    @NotNull(message = "A quantidade mínima é obrigatória.")
    @Min(value = 0, message = "A quantidade mínima não pode ser negativa.")
    private Integer quantidadeMinima;

    @NotNull(message = "A categoria vinculada é obrigatória.")
    private Long categoriaId;

    @NotNull(message = "O ID da organização é obrigatório.")
    private Long orgId;

    // Getters e Setters

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public Integer getQuantidadeMinima() {
        return quantidadeMinima;
    }

    public void setQuantidadeMinima(Integer quantidadeMinima) {
        this.quantidadeMinima = quantidadeMinima;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public void setCategoriaId(Long categoriaId) {
        this.categoriaId = categoriaId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }
}
