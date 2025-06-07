package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @InjectMocks
    private UsuarioService service;

    @Mock
    private UsuarioRepository repository;

    private Usuario usuario;
    private Org org;

    @BeforeEach
    void setup() {
        org = new Org();
        org.setId(1L);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("usuario1");
        usuario.setOrg(org);
        usuario.setAtivo(false);
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
}
