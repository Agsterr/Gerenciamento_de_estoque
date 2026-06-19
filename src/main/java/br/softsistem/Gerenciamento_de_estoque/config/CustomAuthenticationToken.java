package br.softsistem.Gerenciamento_de_estoque.config;

/**
 * Token de autenticação personalizado para expor userId/orgId no SecurityContext.
 */
public class CustomAuthenticationToken extends org.springframework.security.authentication.AbstractAuthenticationToken {

    private final org.springframework.security.core.userdetails.UserDetails principal;
    private final java.lang.Long userId;
    private final java.lang.Long orgId;

    public CustomAuthenticationToken(
            org.springframework.security.core.userdetails.UserDetails principal,
            java.lang.Long userId,
            java.lang.Long orgId,
            java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities
    ) {
        super(authorities);
        this.principal = principal;
        this.userId = userId;
        this.orgId = orgId;
        setAuthenticated(true);
    }

    @Override
    public java.lang.Object getCredentials() {
        return null;
    }

    @Override
    public java.lang.Object getPrincipal() {
        return principal;
    }

    @Override
    public java.util.Collection<org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return super.getAuthorities();
    }

    public java.lang.Long getUserId() {
        return userId;
    }

    public java.lang.Long getOrgId() {
        return orgId;
    }
}
