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
