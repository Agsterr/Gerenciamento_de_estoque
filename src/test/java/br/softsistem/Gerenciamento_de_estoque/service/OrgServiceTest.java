package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgRequestDto;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    @InjectMocks
    private OrgService orgService;

    @Mock
    private OrgRepository orgRepository;

    private Org orgAtiva;

    @BeforeEach
    void setUp() {
        orgAtiva = new Org();
        orgAtiva.setId(1L);
        orgAtiva.setNome("Empresa X");
        orgAtiva.setAtivo(true);
    }

    @Test
    void deveCriarNovaOrgQuandoNaoExisteComMesmoNome() {
        OrgRequestDto request = new OrgRequestDto("Nova Org");
        when(orgRepository.findByNome("Nova Org")).thenReturn(Optional.empty());

        Org nova = new Org("Nova Org");
        nova.setId(2L);

        when(orgRepository.save(any())).thenReturn(nova);

        Optional<OrgDto> resultado = orgService.createOrg(request);

        assertTrue(resultado.isPresent());
        assertEquals("Nova Org", resultado.get().nome());
    }

    @Test
    void naoDeveCriarOrgComNomeDuplicado() {
        OrgRequestDto request = new OrgRequestDto("Empresa X");
        when(orgRepository.findByNome("Empresa X")).thenReturn(Optional.of(orgAtiva));

        Optional<OrgDto> resultado = orgService.createOrg(request);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void deveListarTodasOrgs() {
        List<Org> lista = List.of(orgAtiva, new Org("Empresa Y"));
        when(orgRepository.findAll()).thenReturn(lista);

        List<OrgDto> orgs = orgService.getAllOrgs();

        assertEquals(2, orgs.size());
    }

    @Test
    void deveRetornarOrgPorId() {
        when(orgRepository.findById(1L)).thenReturn(Optional.of(orgAtiva));

        Optional<OrgDto> resultado = orgService.getOrgById(1L);

        assertTrue(resultado.isPresent());
        assertEquals("Empresa X", resultado.get().nome());
    }

    @Test
    void deveAtualizarNomeDaOrg() {
        OrgRequestDto novoNome = new OrgRequestDto("Empresa Z");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(orgAtiva));
        when(orgRepository.findByNome("Empresa Z")).thenReturn(Optional.empty());
        when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<OrgDto> atualizado = orgService.updateOrg(1L, novoNome);

        assertTrue(atualizado.isPresent());
        assertEquals("Empresa Z", atualizado.get().nome());
    }

    @Test
    void naoDeveAtualizarSeNomeJaExistirEmOutraOrg() {
        Org outraOrg = new Org("Empresa Z");
        outraOrg.setId(2L);

        when(orgRepository.findById(1L)).thenReturn(Optional.of(orgAtiva));
        when(orgRepository.findByNome("Empresa Z")).thenReturn(Optional.of(outraOrg));

        Optional<OrgDto> resultado = orgService.updateOrg(1L, new OrgRequestDto("Empresa Z"));

        assertNull(resultado.orElse(null));
    }

    @Test
    void deveDesativarOrg() {
        when(orgRepository.findById(1L)).thenReturn(Optional.of(orgAtiva));
        when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean sucesso = orgService.desativarOrg(1L);

        assertTrue(sucesso);
        assertFalse(orgAtiva.getAtivo());
    }

    @Test
    void deveAtivarOrg() {
        orgAtiva.setAtivo(false);
        when(orgRepository.findById(1L)).thenReturn(Optional.of(orgAtiva));
        when(orgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean sucesso = orgService.ativarOrg(1L);

        assertTrue(sucesso);
        assertTrue(orgAtiva.getAtivo());
    }

    @Test
    void naoDeveAtivarOuDesativarSeOrgNaoExistir() {
        when(orgRepository.findById(99L)).thenReturn(Optional.empty());

        assertFalse(orgService.ativarOrg(99L));
        assertFalse(orgService.desativarOrg(99L));
    }
}
