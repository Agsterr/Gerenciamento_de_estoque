package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;

@Entity
@Table(name = "estoque_deposito", uniqueConstraints = @UniqueConstraint(columnNames = {"produto_id", "deposito_id"}))
public class EstoqueDeposito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;

    @Column(nullable = false)
    private Integer quantidade = 0;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id")
    private Org org;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Deposito getDeposito() { return deposito; }
    public void setDeposito(Deposito deposito) { this.deposito = deposito; }
    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
}
