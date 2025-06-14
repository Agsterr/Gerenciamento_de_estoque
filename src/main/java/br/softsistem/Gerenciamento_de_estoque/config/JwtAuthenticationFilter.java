package br.softsistem.Gerenciamento_de_estoque.config;

import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtro JWT que extrai username, userId, orgId e roles dos claims e popula o SecurityContext
 * com um CustomAuthenticationToken contendo UserDetails + userId + orgId + authorities.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        String username = jwtService.extractUsername(jwt);
        Long orgId = jwtService.extractOrgId(jwt);
        Long userId = jwtService.extractUserId(jwt);

        if (username != null &&
                SecurityContextHolder.getContext().getAuthentication() == null) {

            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Extrai as roles do token e converte em GrantedAuthority
                List<String> roleNames = jwtService.extractRoles(jwt);
                List<GrantedAuthority> authorities = roleNames.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                // Cria CustomAuthenticationToken com principal, userId, orgId e authorities
                var authToken = new CustomAuthenticationToken(
                        userDetails,     // principal = UserDetails
                        userId,          // credentials (não usado)
                        orgId,           // detalhe da org
                        authorities      // roles como GrantedAuthority
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authToken);
            }
        }

        chain.doFilter(request, response);
    }
}
