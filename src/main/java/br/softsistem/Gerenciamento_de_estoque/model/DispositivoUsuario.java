package br.softsistem.Gerenciamento_de_estoque.model;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusDispositivo;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispositivos_usuario")
public class DispositivoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @Column(nullable = false, length = 128)
    private String fingerprint;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "nome_dispositivo", length = 150)
    private String nomeDispositivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusDispositivo status = StatusDispositivo.PENDING;

    @Column(name = "solicitado_em", nullable = false)
    private LocalDateTime solicitadoEm = LocalDateTime.now();

    @Column(name = "revisado_em")
    private LocalDateTime revisadoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisado_por_id")
    private Usuario revisadoPor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Org getOrg() { return org; }
    public void setOrg(Org org) { this.org = org; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getNomeDispositivo() { return nomeDispositivo; }
    public void setNomeDispositivo(String nomeDispositivo) { this.nomeDispositivo = nomeDispositivo; }
    public StatusDispositivo getStatus() { return status; }
    public void setStatus(StatusDispositivo status) { this.status = status; }
    public LocalDateTime getSolicitadoEm() { return solicitadoEm; }
    public void setSolicitadoEm(LocalDateTime solicitadoEm) { this.solicitadoEm = solicitadoEm; }
    public LocalDateTime getRevisadoEm() { return revisadoEm; }
    public void setRevisadoEm(LocalDateTime revisadoEm) { this.revisadoEm = revisadoEm; }
    public Usuario getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(Usuario revisadoPor) { this.revisadoPor = revisadoPor; }
}
