package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.exception.UsuarioDesativadoException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UsuarioRepository usuarioRepository;
    private OrgRepository orgRepository;
    private JwtService jwtService;
    private PasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;

    private AuthService authService;

    @BeforeEach
    void setup() {
        usuarioRepository = mock(UsuarioRepository.class);
        orgRepository = mock(OrgRepository.class);
        jwtService = mock(JwtService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        roleRepository = mock(RoleRepository.class);

        authService = new AuthService(usuarioRepository, orgRepository, jwtService, passwordEncoder, roleRepository);
    }

    @Test
    void login_deveRetornarTokenComCredenciaisValidas() {
        LoginRequestDto request = new LoginRequestDto("user", "senha123", 1L);
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        usuario.setUsername("user");
        usuario.setSenha("hashed_senha");
        usuario.setAtivo(true);
        Org org = new Org();
        org.setId(1L);
        usuario.setOrg(org);

        when(usuarioRepository.findByUsernameAndOrgId("user", 1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "hashed_senha")).thenReturn(true);
        when(jwtService.generateToken(any(), eq(10L), eq(1L))).thenReturn("fake-jwt");

        LoginResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("fake-jwt", response.token());
    }

    @Test
    void login_deveLancarExcecaoSeUsuarioDesativado() {
        LoginRequestDto request = new LoginRequestDto("user", "senha123", 1L);
        Usuario usuario = new Usuario();
        usuario.setUsername("user");
        usuario.setAtivo(false);
        usuario.setOrg(new Org());

        when(usuarioRepository.findByUsernameAndOrgId("user", 1L)).thenReturn(Optional.of(usuario));

        assertThrows(UsuarioDesativadoException.class, () -> authService.login(request));
    }

    @Test
    void login_deveLancarExcecaoSeSenhaIncorreta() {
        LoginRequestDto request = new LoginRequestDto("user", "senhaErrada", 1L);
        Usuario usuario = new Usuario();
        usuario.setSenha("hashed_senha");
        usuario.setAtivo(true);
        usuario.setOrg(new Org());

        when(usuarioRepository.findByUsernameAndOrgId("user", 1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "hashed_senha")).thenReturn(false);

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    void register_deveCriarNovoUsuario() {
        UsuarioRequestDto request = new UsuarioRequestDto(
                "novoUser",
                "senha123",
                "email@teste.com",
                List.of("ADMIN", "USER"),
                1L
        );

        Org org = new Org();
        org.setId(1L);
        when(orgRepository.findById(1L)).thenReturn(Optional.of(org));

        Role roleAdmin = new Role();
        roleAdmin.setNome("ADMIN");
        Role roleUser = new Role();
        roleUser.setNome("USER");

        when(roleRepository.findByNome("ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(roleRepository.findByNome("USER")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(roleUser);

        when(passwordEncoder.encode("senha123")).thenReturn("encodedSenha");

        Usuario usuarioSalvo = new Usuario();
        usuarioSalvo.setId(99L);
        usuarioSalvo.setUsername("novoUser");
        usuarioSalvo.setEmail("email@teste.com");
        usuarioSalvo.setAtivo(true);
        usuarioSalvo.setOrg(org);
        usuarioSalvo.setRoles(List.of(roleAdmin, roleUser));

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioSalvo);

        UsuarioDto response = authService.register(request);

        assertEquals("novoUser", response.username());
        assertEquals("email@teste.com", response.email());
        assertTrue(response.ativo());
        assertEquals(List.of("ADMIN", "USER"), response.roles());
    }
}
