package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.security.core.context.SecurityContextHolder;
import br.softsistem.Gerenciamento_de_estoque.config.CustomAuthenticationToken;

public class SecurityUtils {

    public static Long getCurrentOrgId() {
        Object authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof CustomAuthenticationToken) {
            return ((CustomAuthenticationToken) authentication).getOrgId();  // Retorna o org_id do contexto de segurança
        }
        return null;
    }
}