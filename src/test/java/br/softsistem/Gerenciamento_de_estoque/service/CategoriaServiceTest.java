package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoriaServiceTest {

    private CategoriaRepository categoriaRepository;
    private OrgRepository orgRepository;
    private CategoriaService categoriaService;

    @BeforeEach
    void setup() {
        categoriaRepository = mock(CategoriaRepository.class);
        orgRepository = mock(OrgRepository.class);
        categoriaService = new CategoriaService();
        categoriaService.categoriaRepository = categoriaRepository;
        categoriaService.orgRepository = orgRepository;
    }

    private Categoria criarCategoria(Org org) {
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Eletrônicos");
        categoria.setDescricao("Aparelhos e dispositivos");
        categoria.setOrg(org);
        return categoria;
    }

    private Org criarOrg() {
        Org org = new Org();
        org.setId(1L);
        org.setNome("Empresa X");
        return org;
    }

    @Test
    void listarTodos_deveRetornarCategoriasPaginadas() {
        Org org = criarOrg();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Categoria> page = new PageImpl<>(List.of(criarCategoria(org)));

        when(categoriaRepository.findByOrg_Id(1L, pageable)).thenReturn(page);

        Page<Categoria> resultado = categoriaService.listarTodos(1L, pageable);

        assertEquals(1, resultado.getContent().size());
        verify(categoriaRepository).findByOrg_Id(1L, pageable);
    }

    @Test
    void buscarPorNomeEOrgId_deveRetornarCategoria() {
        Org org = criarOrg();
        Categoria categoria = criarCategoria(org);
        when(categoriaRepository.findByNomeAndOrg_Id("Eletrônicos", 1L)).thenReturn(Optional.of(categoria));

        Optional<Categoria> resultado = categoriaService.buscarPorNomeEOrgId("Eletrônicos", 1L);

        assertTrue(resultado.isPresent());
        assertEquals("Eletrônicos", resultado.get().getNome());
    }

    @Test
    void buscarPorParteDoNomeEOrgId_deveRetornarCategoriasPaginadas() {
        Org org = criarOrg();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Categoria> page = new PageImpl<>(List.of(criarCategoria(org)));

        when(categoriaRepository.findByNomeContainingAndOrg_Id("tron", 1L, pageable)).thenReturn(page);

        Page<Categoria> resultado = categoriaService.buscarPorParteDoNomeEOrgId("tron", 1L, pageable);

        assertEquals(1, resultado.getTotalElements());
    }

    @Test
    void salvarCategoria_deveSalvarCategoriaComSucesso() {
        Org org = criarOrg();
        CategoriaRequest request = new CategoriaRequest("Informática", "Tudo de TI");

        when(orgRepository.findById(1L)).thenReturn(Optional.of(org));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Categoria salva = categoriaService.salvarCategoria(request, 1L);

        assertEquals("Informática", salva.getNome());
        assertEquals("Tudo de TI", salva.getDescricao());
        assertEquals(org, salva.getOrg());
    }

    @Test
    void salvarCategoria_deveLancarExcecaoSeOrgNaoEncontrada() {
        CategoriaRequest request = new CategoriaRequest("Informática", "Tudo de TI");
        when(orgRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> categoriaService.salvarCategoria(request, 1L));
        assertEquals("Organização não encontrada", ex.getMessage());
    }

    @Test
    void editarCategoria_deveAtualizarCategoria() {
        Org org = criarOrg();
        Categoria categoriaExistente = criarCategoria(org);
        CategoriaRequest request = new CategoriaRequest("Nova Categoria", "Nova descrição");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoriaExistente));
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Categoria atualizada = categoriaService.editarCategoria(1L, request, 1L);

        assertEquals("Nova Categoria", atualizada.getNome());
        assertEquals("Nova descrição", atualizada.getDescricao());
    }

    @Test
    void editarCategoria_deveRetornarNullSeNaoPertencerOrg() {
        Org outraOrg = new Org();
        outraOrg.setId(2L);
        Categoria categoria = criarCategoria(outraOrg);
        CategoriaRequest request = new CategoriaRequest("Outra", "Desc");

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        Categoria resultado = categoriaService.editarCategoria(1L, request, 1L);

        assertNull(resultado);
    }

    @Test
    void excluirCategoria_deveExcluirQuandoPertenceOrg() {
        Org org = criarOrg();
        Categoria categoria = criarCategoria(org);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        boolean excluido = categoriaService.excluirCategoria(1L, 1L);

        assertTrue(excluido);
        verify(categoriaRepository).deleteById(1L);
    }

    @Test
    void excluirCategoria_naoExcluiSeOrgNaoBate() {
        Org outraOrg = new Org();
        outraOrg.setId(2L);
        Categoria categoria = criarCategoria(outraOrg);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        boolean excluido = categoriaService.excluirCategoria(1L, 1L);

        assertFalse(excluido);
        verify(categoriaRepository, never()).deleteById(any());
    }
}
