package br.softsistem.Gerenciamento_de_estoque.model;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusContagemInventario;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contagens_inventario")
public class ContagemInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deposito_id")
    private Deposito deposito;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusContagemInventario status = StatusContagemInventario.ABERTA;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "org_id")
    private Org org;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @OneToMany(mappedBy = "contagem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContagemInventarioItem> itens = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Deposito getDeposito() { return deposito; }
    public void setDeposito(Deposito deposito) { this.deposito = deposito; }
    public StatusContagemInventario getStatus() { return status; }
    public void setStatus(StatusContagemInventario status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public LocalDateTime getFinalizadoEm() { return finalizadoEm; }
    public void setFinalizadoEm(LocalDateTime finalizadoEm) { this.finalizadoEm = finalizadoEm; }
    public List<ContagemInventarioItem> getItens() { return itens; }
    public void setItens(List<ContagemInventarioItem> itens) { this.itens = itens; }
}
