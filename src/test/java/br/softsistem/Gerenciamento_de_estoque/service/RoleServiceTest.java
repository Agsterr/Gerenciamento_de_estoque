package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @InjectMocks
    private RoleService service;

    @Mock
    private RoleRepository repository;

    private Role role;
    private Org org;

    @BeforeEach
    void setUp() {
        org = new Org();
        org.setId(1L);

        role = new Role();
        role.setId(1L);
        role.setNome("ADMIN");
        role.setOrg(org);
    }

    @Test
    void deveRetornarTodasAsRoles() {
        when(repository.findAll()).thenReturn(List.of(role));

        List<Role> resultado = service.getAllRoles();

        assertEquals(1, resultado.size());
        assertEquals("ADMIN", resultado.get(0).getNome());
    }

    @Test
    void deveRetornarRolePorId() {
        when(repository.findById(1L)).thenReturn(Optional.of(role));

        Optional<Role> resultado = service.getRoleById(1L);

        assertTrue(resultado.isPresent());
        assertEquals("ADMIN", resultado.get().getNome());
    }

    @Test
    void deveCriarRoleSeNomeNaoExistir() {
        when(repository.existsByNome("ADMIN")).thenReturn(false);
        when(repository.save(role)).thenReturn(role);

        Role criada = service.createRole(role);

        assertNotNull(criada);
        assertEquals("ADMIN", criada.getNome());
    }

    @Test
    void naoDeveCriarRoleComNomeDuplicado() {
        when(repository.existsByNome("ADMIN")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> service.createRole(role));
    }

    @Test
    void deveAtualizarRoleSeExistir() {
        Role atualizada = new Role();
        atualizada.setNome("GESTOR");
        atualizada.setOrg(org);

        when(repository.findById(1L)).thenReturn(Optional.of(role));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Role> resultado = service.updateRole(1L, atualizada);

        assertTrue(resultado.isPresent());
        assertEquals("GESTOR", resultado.get().getNome());
    }

    @Test
    void naoDeveAtualizarSeRoleNaoExistir() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        Optional<Role> resultado = service.updateRole(99L, new Role());

        assertFalse(resultado.isPresent());
    }

    @Test
    void deveExcluirRoleSeExistir() {
        when(repository.existsById(1L)).thenReturn(true);

        boolean resultado = service.deleteRole(1L);

        assertTrue(resultado);
        verify(repository).deleteById(1L);
    }

    @Test
    void naoDeveExcluirRoleSeNaoExistir() {
        when(repository.existsById(99L)).thenReturn(false);

        boolean resultado = service.deleteRole(99L);

        assertFalse(resultado);
        verify(repository, never()).deleteById(any());
    }
}
