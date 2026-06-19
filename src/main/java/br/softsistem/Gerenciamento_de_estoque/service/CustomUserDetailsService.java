package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Profile("!test")
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String loginId = username != null ? username.trim() : "";
        if (loginId.isEmpty()) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }

        Usuario usuario;
        if (loginId.contains("@")) {
            String email = loginId.toLowerCase(Locale.ROOT);
            List<Usuario> byEmail = usuarioRepository.findAllByEmailIgnoreCase(email);
            if (byEmail.isEmpty()) {
                byEmail = usuarioRepository.findAllByEmail(email);
            }
            usuario = byEmail.stream().findFirst()
                    .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado por e-mail"));
        } else {
            List<Usuario> byUsername = usuarioRepository.findAllByUsernameIgnoreCase(loginId);
            if (byUsername.isEmpty()) {
                usuario = usuarioRepository.findByUsername(loginId)
                        .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado por username"));
            } else {
                usuario = byUsername.get(0);
            }
        }

        // Converte as roles do banco de dados para SimpleGrantedAuthority
        Collection<SimpleGrantedAuthority> authorities = usuario.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getNome())) // Já vem como ROLE_ADMIN
                .collect(Collectors.toList());

        // Retorna o usuário com roles atribuídas
        return new org.springframework.security.core.userdetails.User(
                usuario.getUsername(),
                usuario.getSenha(),
                usuario.getAtivo(), // habilitado
                usuario.isAccountNonExpired(),
                usuario.isCredentialsNonExpired(),
                usuario.isAccountNonLocked(),
                authorities // Adiciona as roles
        );
    }
}
