package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails principal;
    private final Long orgId;

    public CustomAuthenticationToken(UserDetails principal, Long orgId) {
        super(principal.getAuthorities());  // Passando as authorities para o construtor
        this.principal = principal;
        this.orgId = orgId;
        setAuthenticated(true);  // Marcando como autenticado
    }

    @Override
    public Object getCredentials() {
        return null;  // A senha não é necessária aqui
    }

    @Override
    public Object getPrincipal() {
        return principal;  // Retorna o UserDetails
    }

    public Long getOrgId() {
        return orgId;  // Retorna o org_id
    }
}
