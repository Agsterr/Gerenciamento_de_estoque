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
    private String nomeConsumidor;
    private Long consumidorId; // novo
    private Long usuarioId;
    private String nomeUsuario;
    private Long entregaId;
    // novo campo para relatório
    private String nomeEntregador;

    public MovimentacaoProdutoDto() {}

    public MovimentacaoProdutoDto(MovimentacaoProduto movimentacao) {
        this.id = movimentacao.getId();
        this.produtoId = movimentacao.getProduto().getId();
        this.nomeProduto = movimentacao.getProduto().getNome();
        this.quantidade = movimentacao.getQuantidade();
        this.dataHora = movimentacao.getDataHora();
        this.tipo = movimentacao.getTipo();
        this.orgId = movimentacao.getOrg().getId();
        if (movimentacao.getEntrega() != null) {
            this.entregaId = movimentacao.getEntrega().getId();
            if (movimentacao.getEntrega().getConsumidor() != null) {
                this.nomeConsumidor = movimentacao.getEntrega().getConsumidor().getNome();
                this.consumidorId = movimentacao.getEntrega().getConsumidor().getId();
            }
            if (movimentacao.getEntrega().getEntregador() != null) {
                this.nomeEntregador = movimentacao.getEntrega().getEntregador().getUsername();
            }
        }
        if (movimentacao.getConsumidor() != null) { // consumidor direto (sem entrega)
            this.nomeConsumidor = movimentacao.getConsumidor().getNome();
            this.consumidorId = movimentacao.getConsumidor().getId();
        }
        // usuario opcional (se existir no model)
        // if (movimentacao.getUsuario() != null) { ... }
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

    public String getNomeConsumidor() {
        return nomeConsumidor;
    }

    public void setNomeConsumidor(String nomeConsumidor) {
        this.nomeConsumidor = nomeConsumidor;
    }

    public Long getConsumidorId() { return consumidorId; }
    public void setConsumidorId(Long consumidorId) { this.consumidorId = consumidorId; }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNomeUsuario() {
        return nomeUsuario;
    }

    public void setNomeUsuario(String nomeUsuario) {
        this.nomeUsuario = nomeUsuario;
    }

    public Long getEntregaId() {
        return entregaId;
    }

    public void setEntregaId(Long entregaId) {
        this.entregaId = entregaId;
    }

    public String getNomeEntregador() { return nomeEntregador; }
    public void setNomeEntregador(String nomeEntregador) { this.nomeEntregador = nomeEntregador; }
}
