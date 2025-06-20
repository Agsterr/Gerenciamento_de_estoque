package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Consumidor {
    
    public Consumidor(){}

    public Consumidor(Long id, String nome, String cpf, String endereco, Org org) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.endereco = endereco;
        this.org = org; // Opcional, se você quiser incluir a organização diretamente
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Column(nullable = false)
    private String endereco;

    // Relacionamento com a entidade Org (Organização)
    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false) // Chave estrangeira para a organização
    private Org org;

    @OneToMany(mappedBy = "consumidor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Entrega> entregas;

    public Consumidor(long l, String carlos, String number, String ruaAbc) {
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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Org getOrg() {
        return org;
    }

    public void setOrg(Org org) {
        this.org = org;
    }

    public List<Entrega> getEntregas() {
        return entregas;
    }

    public void setEntregas(List<Entrega> entregas) {
        this.entregas = entregas;
    }
}
