package br.softsistem.Gerenciamento_de_estoque.config;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
@Profile("!test")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UsuarioRepository usuarioRepository) {
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);
        final String username;
        final Long tokenOrgId;
        final Long tokenUserId;
        try {
            username = jwtService.extractUsername(jwt);
            tokenOrgId = jwtService.extractOrgId(jwt);
            tokenUserId = jwtService.extractUserId(jwt);
        } catch (Exception e) {
            chain.doFilter(request, response);
            return;
        }

        if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        Usuario usuario = null;
        if (tokenOrgId != null) {
            usuario = usuarioRepository.findByUsernameAndOrgId(username, tokenOrgId).orElse(null);
            if (usuario != null) {
                Long resolvedOrgId = usuario.getOrg() != null ? usuario.getOrg().getId() : null;
                if (resolvedOrgId == null || !resolvedOrgId.equals(tokenOrgId)) {
                    usuario = null;
                } else if (tokenUserId != null && !tokenUserId.equals(usuario.getId())) {
                    usuario = null;
                }
            }
        } else if (tokenUserId != null) {
            usuario = usuarioRepository.findById(tokenUserId).orElse(null);
            if (usuario != null && !username.equals(usuario.getUsername())) {
                usuario = null;
            }
        }

        if (usuario != null && jwtService.isTokenValid(jwt, usuario)) {
            Long resolvedOrgId = usuario.getOrg() != null ? usuario.getOrg().getId() : null;
            Long resolvedUserId = usuario.getId();

            List<String> roleNames = jwtService.extractRoles(jwt);
            if (roleNames == null) {
                roleNames = List.of();
            }
            List<GrantedAuthority> authorities = roleNames.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            var authToken = new CustomAuthenticationToken(
                    usuario,
                    resolvedUserId,
                    resolvedOrgId,
                    authorities
            );
            authToken.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

        chain.doFilter(request, response);
    }
}
