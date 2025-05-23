package br.softsistem.Gerenciamento_de_estoque.model;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.transaction.Transactional;

import java.util.List;

@Entity
@Transactional
@Table(name = "role")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nome;

    // Relacionamento com a entidade Org (Organização)
    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false) // Chave estrangeira para a organização
    private Org org;

    @JsonIgnore
    @ManyToMany(mappedBy = "roles")
    private List<Usuario> usuarios; // Relacionamento bidirecional com Usuario

    public Role() {}

    public Role(String nome) {
        this.nome = nome;
    }

    public Role(String nome, Org org) {
        this.nome = nome;
        this.org = org;
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

    public Org getOrg() {
        return org;
    }

    public void setOrg(Org org) {
        this.org = org;
    }

    public List<Usuario> getUsuarios() {
        return usuarios;
    }

    public void setUsuarios(List<Usuario> usuarios) {
        this.usuarios = usuarios;
    }
}
