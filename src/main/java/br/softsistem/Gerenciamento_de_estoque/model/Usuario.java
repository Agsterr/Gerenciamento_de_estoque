package br.softsistem.Gerenciamento_de_estoque.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O username é obrigatório.")
    @Size(max = 100, message = "O username deve ter no máximo 100 caracteres.")
    @Column(nullable = false, length = 100)
    private String username;

    @NotBlank(message = "O e-mail é obrigatório.")
    @Email(message = "O e-mail deve ser válido.")
    @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @NotBlank(message = "A senha é obrigatória.")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres.")
    @Column(nullable = false)
    private String senha;

    @NotNull(message = "A data de criação não pode ser nula.")
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @NotNull(message = "O status ativo é obrigatório.")
    @Column(nullable = false)
    private Boolean ativo = true;

    @ManyToMany(fetch = FetchType.EAGER) // Relacionamento ManyToMany
    @JoinTable(
            name = "usuario_roles", // Nome da tabela intermediária
            joinColumns = @JoinColumn(name = "usuario_id"), // Coluna para a entidade Usuario
            inverseJoinColumns = @JoinColumn(name = "role_id") // Coluna para a entidade Role
    )
    private List<Role>roles; // Lista de roles associadas ao usuário




    // Métodos de UserDetails

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {


        // Aqui você pode adicionar as permissões do usuário (roles)
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return senha;
    }

    public String getSenha() {
        return senha;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Customize se necessário
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Customize se necessário
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Customize se necessário
    }

    @Override
    public boolean isEnabled() {
        return ativo; // Utiliza o campo "ativo" para verificar se o usuário está habilitado
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }


    // equals, hashCode e toString

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Usuario usuario = (Usuario) o;
        return Objects.equals(id, usuario.id) &&
                Objects.equals(username, usuario.username) &&
                Objects.equals(email, usuario.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username, email);
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", criadoEm=" + criadoEm +
                ", ativo=" + ativo +
                '}';
    }


}
