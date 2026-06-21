package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.CreateUsuarioOrgRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioCreatedResponse;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioPasswordResponse;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @InjectMocks
    private UsuarioService service;

    @Mock
    private UsuarioRepository repository;

    @Mock
    private OrgRepository orgRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TrialSubscriptionService trialSubscriptionService;

    @Mock
    private OrgUserLimitService orgUserLimitService;

    private Usuario usuario;
    private Org org;

    @BeforeEach
    void setup() {
        org = new Org();
        org.setId(1L);
        org.setNome("Org Teste");
        org.setEphemeral(false);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("usuario1");
        usuario.setOrg(org);
        usuario.setAtivo(false);
        usuario.setRoles(new ArrayList<>());
    }

    @Test
    void deveAtivarUsuarioSePertencerAOrg() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ativarUsuario(1L, 1L);

        assertTrue(usuario.getAtivo());
    }

    @Test
    void deveLancarErroAoAtivarUsuarioDeOutraOrg() {
        Org outraOrg = new Org();
        outraOrg.setId(99L);
        usuario.setOrg(outraOrg);

        when(repository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(RuntimeException.class, () -> service.ativarUsuario(1L, 1L));
    }

    @Test
    void deveReativarUsuarioInativoPorUsernameEOrg() {
        usuario.setAtivo(false);
        when(repository.findByUsernameAndOrgId("usuario1", 1L)).thenReturn(Optional.of(usuario));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.reativarUsuario("usuario1", 1L);

        assertTrue(usuario.getAtivo());
    }

    @Test
    void naoDeveSalvarSeUsuarioJaEstiverAtivoNaReativacao() {
        usuario.setAtivo(true);
        when(repository.findByUsernameAndOrgId("usuario1", 1L)).thenReturn(Optional.of(usuario));

        service.reativarUsuario("usuario1", 1L);

        verify(repository, never()).save(any());
    }

    @Test
    void deveDesativarUsuarioSePertencerAOrg() {
        usuario.setAtivo(true);
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.desativarUsuario(1L, 1L);

        assertFalse(usuario.getAtivo());
    }

    @Test
    void deveLancarErroAoDesativarUsuarioDeOutraOrg() {
        Org outraOrg = new Org();
        outraOrg.setId(99L);
        usuario.setOrg(outraOrg);

        when(repository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(RuntimeException.class, () -> service.desativarUsuario(1L, 1L));
    }

    @Test
    void deveListarUsuariosAtivosComPaginacao() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Usuario> page = new PageImpl<>(List.of(usuario));

        when(repository.findByAtivoTrueAndOrgId(1L, pageable)).thenReturn(page);

        Page<Usuario> resultado = service.listarUsuariosAtivos(1L, pageable);

        assertEquals(1, resultado.getContent().size());
    }

    @Test
    void deveResetarSenhaDeUsuarioDaOrg() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UsuarioPasswordResponse response = service.resetSenha(1L, 1L);

        assertEquals("usuario1", response.username());
        assertEquals(12, response.temporaryPassword().length());
        verify(passwordEncoder).encode(response.temporaryPassword());
    }

    @Test
    void deveCriarUsuarioComumNaOrg() {
        CreateUsuarioOrgRequest request = new CreateUsuarioOrgRequest("novo_user", "novo@test.local");
        Role userRole = new Role("ROLE_USER", org);

        when(orgRepository.findById(1L)).thenReturn(Optional.of(org));
        doNothing().when(orgUserLimitService).assertCanAddUser(org, 10L);
        when(repository.findByEmailIgnoreCase("novo@test.local")).thenReturn(Optional.empty());
        when(roleRepository.findByNomeAndOrgId("ROLE_USER", 1L)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hash");
        when(repository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario saved = inv.getArgument(0);
            saved.setId(99L);
            saved.setRoles(List.of(userRole));
            return saved;
        });

        UsuarioCreatedResponse response = service.criarUsuarioComum(request, 1L, 10L);

        assertEquals("novo_user", response.usuario().username());
        assertEquals(12, response.temporaryPassword().length());
        verify(trialSubscriptionService).startTrialForUser(any(Usuario.class));
    }
}
