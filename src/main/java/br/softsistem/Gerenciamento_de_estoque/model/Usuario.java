package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;


    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean ativo = true; // Adiciona o campo "ativo"

    // Getters e Setters


    public String getNome() {
        return username;
    }

    public void setNome(String nome) {
        this.username = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id) && Objects.equals(username, usuario.username) && Objects.equals(email, usuario.email) && Objects.equals(senha, usuario.senha) && Objects.equals(criadoEm, usuario.criadoEm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email, senha, criadoEm);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", nome='" + username + '\'' +
                ", email='" + email + '\'' +
                ", senha='" + senha + '\'' +
                ", criadoEm=" + criadoEm +
                '}';
    }
}
