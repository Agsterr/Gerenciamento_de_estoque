package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Token de autenticação personalizado que armazena um orgId adicional além do UserDetails.
 */
public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails principal;
    private final Long orgId;

    public CustomAuthenticationToken(UserDetails principal, Long orgId) {
        super(principal.getAuthorities()); // Usa as authorities do usuário
        this.principal = principal;
        this.orgId = orgId;
        setAuthenticated(true); // Marca como autenticado
    }

    @Override
    public Object getCredentials() {
        return null; // Nenhuma credencial é exposta
    }

    @Override
    public Object getPrincipal() {
        return principal; // Retorna o usuário autenticado
    }

    public Long getOrgId() {
        return orgId; // Permite acesso ao ID da organização
    }
}
