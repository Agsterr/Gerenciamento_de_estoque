package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Token de autenticação personalizado que armazena um userId, orgId e authorities
 * além do UserDetails. Assim, qualquer parte do sistema pode saber quem é
 * o usuário logado e a qual organização ele pertence sem precisar enviar
 * esses dados no corpo da requisição.
 */
public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails principal;
    private final Long userId;
    private final Long orgId;

    /**
     * Construtor que recebe UserDetails, userId, orgId e authorities.
     *
     * @param principal   o UserDetails do usuário autenticado
     * @param userId      o ID numérico do usuário
     * @param orgId       o ID da organização
     * @param authorities as autoridades (roles) do usuário
     */
    public CustomAuthenticationToken(UserDetails principal,
                                     Long userId,
                                     Long orgId,
                                     Collection<GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        this.userId = userId;
        this.orgId = orgId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null; // Credenciais não expostas após a autenticação
    }

    @Override
    public Object getPrincipal() {
        return principal; // Retorna o UserDetails (o usuário)
    }

    /**
     * Retorna as autoridades (roles) associadas ao usuário autenticado.
     */
    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return super.getAuthorities();
    }

    /**
     * @return o ID numérico do usuário autenticado
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * @return o ID da organização associada ao usuário autenticado
     */
    public Long getOrgId() {
        return orgId;
    }
}
