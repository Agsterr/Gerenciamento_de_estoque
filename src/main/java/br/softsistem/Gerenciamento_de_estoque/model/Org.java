package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
@Table(name = "orgs")
public class Org {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O nome da organização é obrigatório.")
    @Size(max = 100, message = "O nome da organização deve ter no máximo 100 caracteres.")
    @Column(nullable = false, unique = true)
    private String nome;

    @Column(nullable = false)
    private Boolean ativo = true;

    @Column(name = "max_dispositivos", nullable = false)
    private Integer maxDispositivos = 3;

    @Column(name = "max_usuarios")
    private Integer maxUsuarios;

    @Column(nullable = false)
    private Boolean ephemeral = false;

    @Column(name = "demo_last_access")
    private java.time.LocalDateTime demoLastAccess;

    public Org() {}

    public Org(String nome) {
        this.nome = nome;
        this.ativo = true;
    }

    public Org(Long orgId) {
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Integer getMaxDispositivos() {
        return maxDispositivos;
    }

    public void setMaxDispositivos(Integer maxDispositivos) {
        this.maxDispositivos = maxDispositivos;
    }

    public Integer getMaxUsuarios() {
        return maxUsuarios;
    }

    public void setMaxUsuarios(Integer maxUsuarios) {
        this.maxUsuarios = maxUsuarios;
    }

    public Boolean getEphemeral() {
        return ephemeral;
    }

    public void setEphemeral(Boolean ephemeral) {
        this.ephemeral = ephemeral;
    }

    public java.time.LocalDateTime getDemoLastAccess() {
        return demoLastAccess;
    }

    public void setDemoLastAccess(java.time.LocalDateTime demoLastAccess) {
        this.demoLastAccess = demoLastAccess;
    }

    public boolean isEphemeralOrg() {
        return Boolean.TRUE.equals(ephemeral);
    }

    /** 0 ou null tratado como sem limite na org (usa plano). */
    public boolean hasUnlimitedUsers() {
        return maxUsuarios == null || maxUsuarios <= 0;
    }

    /** 0 ou null tratado como ilimitado. */
    public boolean hasUnlimitedDevices() {
        return maxDispositivos == null || maxDispositivos <= 0;
    }

    // equals, hashCode e toString

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Org)) return false;
        Org org = (Org) o;
        return Objects.equals(id, org.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Org{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", ativo=" + ativo +
                '}';
    }
}
