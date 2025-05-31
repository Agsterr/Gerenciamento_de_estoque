package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utilitário de segurança para acessar dados do usuário autenticado.
 */
public class SecurityUtils {

    /**
     * Retorna o orgId do usuário autenticado, se disponível.
     *
     * @return orgId ou null se não estiver autenticado com CustomAuthenticationToken.
     */
    public static Long getCurrentOrgId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof CustomAuthenticationToken) {
            return ((CustomAuthenticationToken) authentication).getOrgId();
        }

        return null; // orgId não disponível ou autenticação não é do tipo esperado
    }

    /**
     * Retorna o userId do usuário autenticado, se disponível.
     *
     * @return userId ou null se não estiver autenticado com CustomAuthenticationToken.
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof CustomAuthenticationToken) {
            return ((CustomAuthenticationToken) authentication).getUserId();
        }

        return null; // userId não disponível ou autenticação não é do tipo esperado
    }
}
