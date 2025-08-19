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
        // Para o método loadUserByUsername, normalmente usamos o username, mas também precisamos do orgId
        // Para fazer isso, você pode pegar o orgId de algum contexto, como o token JWT, um parâmetro da requisição, etc.
        // Supondo que o orgId seja obtido de alguma outra forma (por exemplo, como parte do token JWT)

        // O exemplo a seguir usa um orgId hardcoded (substitua com a lógica necessária para obter o orgId)
        Long orgId = getOrgIdFromSomewhere(); // Lógica para obter o orgId

        // Carregar o usuário pelo username e orgId
        Usuario usuario = usuarioRepository.findByUsernameAndOrgId(username, orgId)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado ou não pertence à organização"));

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

    // Método fictício para ilustrar a obtenção do orgId
    private Long getOrgIdFromSomewhere() {
        // Exemplo de como você pode obter o orgId de algum contexto, como JWT, etc.
        // Se você estiver usando um sistema de autenticação baseado em JWT, você obteria o orgId do token JWT
        return 1L;  // Retorne o orgId real que você recupera do contexto
    }
}
