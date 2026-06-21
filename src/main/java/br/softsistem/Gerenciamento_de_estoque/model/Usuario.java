package br.softsistem.Gerenciamento_de_estoque.model;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

    @Column(name = "senha_registrada", length = 128)
    private String senhaRegistrada;

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
    private List<Role> roles; // Lista de roles associadas ao usuário

    // Relacionamento com a entidade Org (Organização)
    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false) // Chave estrangeira para a organização
    private Org org;

    @Column(name = "bypass_subscription", nullable = false)
    private Boolean bypassSubscription = false;

    @Column(name = "asaas_customer_id")
    private String asaasCustomerId;

    @Column(name = "cpf_cnpj", length = 18)
    private String cpfCnpj;

    public Usuario(long l, String entregadorTest, String senha) {
    }

    public Usuario(){

    }

    // Métodos de UserDetails
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            return Collections.emptyList();
        }
        return roles.stream()
                .map(role -> (GrantedAuthority) () -> role.getNome())
                .toList();
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
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return ativo;
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

    public String getSenhaRegistrada() {
        return senhaRegistrada;
    }

    public void setSenhaRegistrada(String senhaRegistrada) {
        this.senhaRegistrada = senhaRegistrada;
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

    public Org getOrg() {
        return org;
    }

    public void setOrg(Org org) {
        this.org = org;
    }

    public Boolean getBypassSubscription() {
        return bypassSubscription;
    }

    public void setBypassSubscription(Boolean bypassSubscription) {
        this.bypassSubscription = bypassSubscription;
    }

    public boolean hasSubscriptionBypass() {
        return Boolean.TRUE.equals(bypassSubscription);
    }

    public boolean isSuperAdmin() {
        return roles != null && roles.stream()
                .anyMatch(r -> "ROLE_SUPER_ADMIN".equals(r.getNome()));
    }

    public String getAsaasCustomerId() {
        return asaasCustomerId;
    }

    public void setAsaasCustomerId(String asaasCustomerId) {
        this.asaasCustomerId = asaasCustomerId;
    }

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

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
                ", org=" + org.getNome() +
                '}';
    }
}
