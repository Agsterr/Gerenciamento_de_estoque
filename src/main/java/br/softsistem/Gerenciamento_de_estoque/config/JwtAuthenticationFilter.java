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

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        final Long orgId;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt); // Extrai o username do token
        orgId = jwtService.extractOrgId(jwt);  // Extraímos o org_id

        // Log para verificar o token, o nome de usuário e o org_id
        System.out.println("Token JWT: " + jwt);  // Log do token
        System.out.println("Username extraído: " + username);  // Log do username extraído
        System.out.println("Org ID extraído: " + orgId);  // Log do org_id extraído

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Usando a classe CustomAuthenticationToken para armazenar org_id
                var authToken = new CustomAuthenticationToken(userDetails, orgId);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Definindo o CustomAuthenticationToken no contexto de segurança
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("Token JWT inválido.");
            }
        }
        chain.doFilter(request, response);
    }
}
