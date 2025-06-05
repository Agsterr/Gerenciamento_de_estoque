package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.exception.ConsumidorNaoEncontradoException;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsumidorServiceTest {

    private ConsumidorRepository consumidorRepository;
    private OrgRepository orgRepository;
    private ConsumidorService consumidorService;

    private static final Long ORG_ID = 1L;

    @BeforeEach
    void setup() {
        consumidorRepository = mock(ConsumidorRepository.class);
        orgRepository = mock(OrgRepository.class);
        consumidorService = new ConsumidorService(consumidorRepository, orgRepository);
    }

    private Consumidor criarConsumidor() {
        Org org = new Org();
        org.setId(ORG_ID);

        Consumidor c = new Consumidor();
        c.setId(1L);
        c.setNome("João");
        c.setCpf("12345678901");
        c.setEndereco("Rua A");
        c.setOrg(org);
        return c;
    }

    @Test
    void listarTodos_deveRetornarPaginaDeConsumidores() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Consumidor> page = new PageImpl<>(List.of(criarConsumidor()));

        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(ORG_ID);
            when(consumidorRepository.findByOrg_Id(ORG_ID, pageable)).thenReturn(page);

            Page<Consumidor> resultado = consumidorService.listarTodos(pageable);

            assertEquals(1, resultado.getTotalElements());
            verify(consumidorRepository).findByOrg_Id(ORG_ID, pageable);
        }
    }

    @Test
    void listarTodos_deveLancarExcecaoSeOrgNula() {
        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(null);

            assertThrows(OrganizacaoNaoEncontradaException.class,
                    () -> consumidorService.listarTodos(PageRequest.of(0, 10)));
        }
    }

    @Test
    void buscarPorNome_deveRetornarConsumidor() {
        Consumidor consumidor = criarConsumidor();
        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(ORG_ID);
            when(consumidorRepository.findByNomeAndOrg_Id("João", ORG_ID)).thenReturn(Optional.of(consumidor));

            Optional<Consumidor> resultado = consumidorService.buscarPorNome("João");

            assertTrue(resultado.isPresent());
            assertEquals("João", resultado.get().getNome());
        }
    }

    @Test
    void salvar_deveAssociarOrgEChamarSave() {
        Consumidor consumidor = criarConsumidor();
        consumidor.setOrg(null); // será definido internamente
        Org org = new Org();
        org.setId(ORG_ID);

        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(ORG_ID);
            when(orgRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
            when(consumidorRepository.save(any(Consumidor.class))).thenAnswer(i -> i.getArgument(0));

            Consumidor salvo = consumidorService.salvar(consumidor);

            assertNotNull(salvo.getOrg());
            assertEquals(ORG_ID, salvo.getOrg().getId());
            verify(consumidorRepository).save(consumidor);
        }
    }

    @Test
    void salvar_deveLancarExcecaoSeOrgNaoEncontrada() {
        Consumidor consumidor = criarConsumidor();
        consumidor.setOrg(null);

        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(ORG_ID);
            when(orgRepository.findById(ORG_ID)).thenReturn(Optional.empty());

            assertThrows(OrganizacaoNaoEncontradaException.class,
                    () -> consumidorService.salvar(consumidor));
        }
    }

    @Test
    void editar_deveAtualizarDadosConsumidor() {
        Consumidor existente = criarConsumidor();
        Consumidor atualizado = new Consumidor();
        atualizado.setNome("Maria");
        atualizado.setCpf("98765432100");
        atualizado.setEndereco("Rua Nova");

        when(consumidorRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(consumidorRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Consumidor salvo = consumidorService.editar(1L, atualizado);

        assertEquals("Maria", salvo.getNome());
        assertEquals("98765432100", salvo.getCpf());
        assertEquals("Rua Nova", salvo.getEndereco());
    }

    @Test
    void editar_deveLancarExcecaoSeConsumidorNaoExiste() {
        when(consumidorRepository.findById(1L)).thenReturn(Optional.empty());

        Consumidor novo = criarConsumidor();
        assertThrows(ConsumidorNaoEncontradoException.class,
                () -> consumidorService.editar(1L, novo));
    }

    @Test
    void excluir_deveRemoverConsumidor() {
        Consumidor consumidor = criarConsumidor();
        when(consumidorRepository.findById(1L)).thenReturn(Optional.of(consumidor));

        consumidorService.excluir(1L);

        verify(consumidorRepository).delete(consumidor);
    }

    @Test
    void excluir_deveLancarExcecaoSeNaoEncontrado() {
        when(consumidorRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ConsumidorNaoEncontradoException.class, () -> consumidorService.excluir(1L));
    }
}
