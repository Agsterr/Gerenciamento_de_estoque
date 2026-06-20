package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pesquisa_preco")
public class PesquisaPreco {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(name = "valor_min", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMin;

    @Column(name = "valor_max", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMax;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public BigDecimal getValorMin() { return valorMin; }
    public void setValorMin(BigDecimal valorMin) { this.valorMin = valorMin; }
    public BigDecimal getValorMax() { return valorMax; }
    public void setValorMax(BigDecimal valorMax) { this.valorMax = valorMax; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
