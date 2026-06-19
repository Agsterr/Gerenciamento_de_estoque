package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;

@Entity
@Table(name = "contagem_inventario_itens")
public class ContagemInventarioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "contagem_id")
    private ContagemInventario contagem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produto_id")
    private Produto produto;

    @Column(name = "quantidade_sistema", nullable = false)
    private Integer quantidadeSistema;

    @Column(name = "quantidade_contada")
    private Integer quantidadeContada;

    @Column
    private Integer diferenca = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ContagemInventario getContagem() { return contagem; }
    public void setContagem(ContagemInventario contagem) { this.contagem = contagem; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Integer getQuantidadeSistema() { return quantidadeSistema; }
    public void setQuantidadeSistema(Integer quantidadeSistema) { this.quantidadeSistema = quantidadeSistema; }
    public Integer getQuantidadeContada() { return quantidadeContada; }
    public void setQuantidadeContada(Integer quantidadeContada) { this.quantidadeContada = quantidadeContada; }
    public Integer getDiferenca() { return diferenca; }
    public void setDiferenca(Integer diferenca) { this.diferenca = diferenca; }
}
