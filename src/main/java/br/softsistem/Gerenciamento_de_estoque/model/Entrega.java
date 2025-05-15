package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Entrega {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Consumidor consumidor;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Usuario entregador;

    @ManyToOne
    @JoinColumn(nullable = false)
    private Produto produto;

    @Column(nullable = false)
    private int quantidade;

    @Column(nullable = false)
    private LocalDateTime horarioEntrega;

    // Relacionamento com a entidade Org (Organização)
    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false) // Chave estrangeira para a organização
    private Org org;

    // Novo campo para armazenar o valor total da entrega
    @Column(nullable = false)
    private BigDecimal valor;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Consumidor getConsumidor() {
        return consumidor;
    }

    public void setConsumidor(Consumidor consumidor) {
        this.consumidor = consumidor;
    }

    public Usuario getEntregador() {
        return entregador;
    }

    public void setEntregador(Usuario entregador) {
        this.entregador = entregador;
    }

    public Produto getProduto() {
        return produto;
    }

    public void setProduto(Produto produto) {
        this.produto = produto;
    }

    public int getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(int quantidade) {
        this.quantidade = quantidade;
    }

    public LocalDateTime getHorarioEntrega() {
        return horarioEntrega;
    }

    public void setHorarioEntrega(LocalDateTime horarioEntrega) {
        this.horarioEntrega = horarioEntrega;
    }

    public Org getOrg() {
        return org;
    }

    public void setOrg(Org org) {
        this.org = org;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    // Método para calcular o valor total da entrega
    public void calcularValor() {
        if (produto != null) {
            // Assume-se que Produto tem um campo 'preco' que contém o preço unitário
            this.valor = produto.getPreco().multiply(BigDecimal.valueOf(quantidade));
        }
    }
}
