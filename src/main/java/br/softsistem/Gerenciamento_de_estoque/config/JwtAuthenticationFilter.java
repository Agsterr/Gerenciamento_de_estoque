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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;
        final Long orgId;

        // Se o cabeçalho Authorization estiver ausente ou não começar com "Bearer ", ignora o filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        // Extrai o token JWT
        jwt = authHeader.substring(7);
        username = jwtService.extractUsername(jwt);
        orgId = jwtService.extractOrgId(jwt); // Extração de um campo adicional (ex: organização)

        // Debug logs (remova ou use logger para produção)
        System.out.println("Token JWT: " + jwt);
        System.out.println("Username extraído: " + username);
        System.out.println("Org ID extraído: " + orgId);

        // Se o usuário ainda não está autenticado
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                // Cria um token de autenticação personalizado que inclui o orgId
                var authToken = new CustomAuthenticationToken(userDetails, orgId);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // Define a autenticação no contexto de segurança
                SecurityContextHolder.getContext().setAuthentication(authToken);
            } else {
                System.out.println("Token JWT inválido.");
            }
        }

        // Continua a execução da cadeia de filtros
        chain.doFilter(request, response);
    }
}
