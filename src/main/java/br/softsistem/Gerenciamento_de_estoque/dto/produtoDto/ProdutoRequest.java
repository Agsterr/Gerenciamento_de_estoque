package br.softsistem.Gerenciamento_de_estoque.dto.produtoDto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @NotNull(message = "A quantidade de entrada é obrigatória.")
    @Min(value = 0, message = "A quantidade de entrada não pode ser negativa.")
    private Integer quantidadeEntrada;

    @NotNull(message = "A quantidade de saída é obrigatória.")
    @Min(value = 0, message = "A quantidade de saída não pode ser negativa.")
    private Integer quantidadeSaida;

    private LocalDateTime dataEntrada;

    private LocalDateTime dataSaida;

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

    public Integer getQuantidadeEntrada() {
        return quantidadeEntrada;
    }

    public void setQuantidadeEntrada(Integer quantidadeEntrada) {
        this.quantidadeEntrada = quantidadeEntrada;
    }

    public Integer getQuantidadeSaida() {
        return quantidadeSaida;
    }

    public void setQuantidadeSaida(Integer quantidadeSaida) {
        this.quantidadeSaida = quantidadeSaida;
    }

    public LocalDateTime getDataEntrada() {
        return dataEntrada;
    }

    public void setDataEntrada(LocalDateTime dataEntrada) {
        this.dataEntrada = dataEntrada;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDateTime dataSaida) {
        this.dataSaida = dataSaida;
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
