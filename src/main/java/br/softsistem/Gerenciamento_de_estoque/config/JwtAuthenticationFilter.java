package br.softsistem.Gerenciamento_de_estoque.config;

import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT que extrai username, userId e orgId dos claims e popula o SecurityContext
 * com um CustomAuthenticationToken contendo UserDetails + userId + orgId.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        final Long orgId;
        final Long userId;

        // Se não tiver Authorization ou não começar com "Bearer ", apenas segue o fluxo
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // Extrai o token (texto após "Bearer ")
        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);
        orgId = jwtService.extractOrgId(jwt);       // novo claim de orgId
        userId = jwtService.extractUserId(jwt);     // novo claim de userId

        // DEBUG (ou use um logger no lugar dos prints em produção)
        System.out.println("=== JWT Filter ===");
        System.out.println(" Token JWT:      " + jwt);
        System.out.println(" Username extraído: " + username);
        System.out.println(" Org ID extraído:   " + orgId);
        System.out.println(" User ID extraído:  " + userId);

        // Só autentica se ainda não estiver autenticado no contexto
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Cria o CustomAuthenticationToken usando userId, orgId e authorities
                var authToken = new CustomAuthenticationToken(
                        userDetails,
                        userId,
                        orgId
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Seta a autenticação no SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("Token JWT inválido para usuário: " + username);
            }
        }

        // Prossegue a cadeia de filtros
        chain.doFilter(request, response);
    }
}
