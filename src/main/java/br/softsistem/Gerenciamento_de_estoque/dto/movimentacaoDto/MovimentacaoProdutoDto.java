package br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto;

import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;

import java.time.LocalDateTime;

public class MovimentacaoProdutoDto {

    private Long id;
    private Long produtoId;
    private String nomeProduto;
    private Integer quantidade;
    private LocalDateTime dataHora;
    private TipoMovimentacao tipo;
    private Long orgId;

    public MovimentacaoProdutoDto() {}

    public MovimentacaoProdutoDto(MovimentacaoProduto movimentacao) {
        this.id = movimentacao.getId();
        this.produtoId = movimentacao.getProduto().getId();
        this.nomeProduto = movimentacao.getProduto().getNome();
        this.quantidade = movimentacao.getQuantidade();
        this.dataHora = movimentacao.getDataHora();
        this.tipo = movimentacao.getTipo();
        this.orgId = movimentacao.getOrg().getId();
    }

    // Getters e setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(Long produtoId) {
        this.produtoId = produtoId;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public TipoMovimentacao getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimentacao tipo) {
        this.tipo = tipo;
    }

    public Long getOrgId() {
        return orgId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }
}
