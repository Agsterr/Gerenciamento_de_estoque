package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sugestoes")
public class Sugestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false, length = 20)
    private String status = "NOVA";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
