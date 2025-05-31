package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Token de autenticação personalizado que armazena um userId e um orgId
 * além do UserDetails. Assim, qualquer parte do sistema pode saber quem é
 * o usuário logado e a qual organização ele pertence sem precisar enviar
 * esses dados no corpo da requisição.
 */
public class CustomAuthenticationToken extends AbstractAuthenticationToken {

    private final UserDetails principal;
    private final Long userId;
    private final Long orgId;

    /**
     * Construtor que recebe UserDetails, userId e orgId.
     *
     * @param principal o UserDetails do usuário autenticado
     * @param userId    o ID numérico do usuário
     * @param orgId     o ID da organização
     */
    public CustomAuthenticationToken(UserDetails principal,
                                     Long userId,
                                     Long orgId) {
        super(principal.getAuthorities()); // Usa as authorities do usuário
        this.principal = principal;
        this.userId = userId;
        this.orgId = orgId;
        setAuthenticated(true); // Marca como autenticado
    }

    @Override
    public Object getCredentials() {
        return null; // Nenhuma credencial (senha) é exposta depois do login
    }

    @Override
    public Object getPrincipal() {
        return principal; // Retorna o UserDetails (o “usuário”)
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
