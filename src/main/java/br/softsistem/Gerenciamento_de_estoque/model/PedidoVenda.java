package br.softsistem.Gerenciamento_de_estoque.model;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.CondicaoPagamento;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.FormaPagamento;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoVenda;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoPedidoVenda;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos_venda", uniqueConstraints = @UniqueConstraint(columnNames = {"numero", "org_id"}))
public class PedidoVenda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pedido", nullable = false, length = 20)
    private TipoPedidoVenda tipoPedido = TipoPedidoVenda.VENDA;

    @ManyToOne(optional = true)
    @JoinColumn(name = "consumidor_id")
    private Consumidor consumidor;

    @ManyToOne(optional = true)
    @JoinColumn(name = "funcionario_id")
    private Usuario funcionario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "forma_pagamento", length = 30)
    private FormaPagamento formaPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicao_pagamento", length = 30)
    private CondicaoPagamento condicaoPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPedidoVenda status = StatusPedidoVenda.RASCUNHO;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "valor_total", precision = 12, scale = 2)
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id")
    private Org org;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PedidoVendaItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public TipoPedidoVenda getTipoPedido() { return tipoPedido; }
    public void setTipoPedido(TipoPedidoVenda tipoPedido) { this.tipoPedido = tipoPedido; }
    public Consumidor getConsumidor() { return consumidor; }
    public void setConsumidor(Consumidor consumidor) { this.consumidor = consumidor; }
    public Usuario getFuncionario() { return funcionario; }
    public void setFuncionario(Usuario funcionario) { this.funcionario = funcionario; }
    public Usuario getVendedor() { return vendedor; }
    public void setVendedor(Usuario vendedor) { this.vendedor = vendedor; }
    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
    public FormaPagamento getFormaPagamento() { return formaPagamento; }
    public void setFormaPagamento(FormaPagamento formaPagamento) { this.formaPagamento = formaPagamento; }
    public CondicaoPagamento getCondicaoPagamento() { return condicaoPagamento; }
    public void setCondicaoPagamento(CondicaoPagamento condicaoPagamento) { this.condicaoPagamento = condicaoPagamento; }
    public StatusPedidoVenda getStatus() { return status; }
    public void setStatus(StatusPedidoVenda status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public void setValorTotal(BigDecimal valorTotal) { this.valorTotal = valorTotal; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public List<PedidoVendaItem> getItens() { return itens; }
    public void setItens(List<PedidoVendaItem> itens) { this.itens = itens; }

    public boolean isInterno() {
        return tipoPedido == TipoPedidoVenda.INTERNO;
    }
}
