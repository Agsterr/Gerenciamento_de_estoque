package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    private UsuarioRepository usuarioRepository;
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setup() {
        usuarioRepository = mock(UsuarioRepository.class);
        customUserDetailsService = new CustomUserDetailsService(usuarioRepository);
    }

    private Usuario criarUsuarioComRole() {
        Org org = new Org();
        org.setId(1L);
        org.setNome("Org Teste");

        Role role = new Role();
        role.setId(1L);
        role.setNome("ROLE_ADMIN");

        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("usuario1");
        usuario.setSenha("senhaSegura");
        usuario.setEmail("teste@exemplo.com");
        usuario.setAtivo(true);
        usuario.setRoles(List.of(role));
        usuario.setOrg(org);

        return usuario;
    }

    @Test
    void loadUserByUsername_deveRetornarUserDetails() {
        Usuario usuario = criarUsuarioComRole();
        when(usuarioRepository.findByUsernameAndOrgId("usuario1", 1L))
                .thenReturn(Optional.of(usuario));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("usuario1");

        assertNotNull(userDetails);
        assertEquals("usuario1", userDetails.getUsername());
        assertEquals("senhaSegura", userDetails.getPassword());
        assertEquals(1, userDetails.getAuthorities().size());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isCredentialsNonExpired());
    }


    @Test
    void loadUserByUsername_deveLancarExcecaoSeUsuarioNaoEncontrado() {
        when(usuarioRepository.findByUsernameAndOrgId("inexistente", 1L))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("inexistente"));
    }
}
