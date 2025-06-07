package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.*;
import br.softsistem.Gerenciamento_de_estoque.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @InjectMocks
    private ProdutoService service;

    @Mock private ProdutoRepository produtoRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private OrgRepository orgRepository;
    @Mock private MovimentacaoProdutoRepository movimentacaoProdutoRepository;

    private ProdutoRequest request;
    private Categoria categoria;
    private Org org;
    private Produto produto;

    @BeforeEach
    void setup() {
        categoria = new Categoria();
        categoria.setId(1L);

        org = new Org();
        org.setId(1L);

        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setQuantidade(10);
        produto.setOrg(org);

        request = new ProdutoRequest();
        request.setNome("Produto Teste");
        request.setDescricao("Descrição");
        request.setPreco(BigDecimal.valueOf(100.0));
        request.setQuantidade(5);
        request.setQuantidadeMinima(3);
        request.setCategoriaId(1L);
    }

    @Test
    void deveSalvarNovoProduto() {
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(orgRepository.findById(1L)).thenReturn(Optional.of(org));
        when(produtoRepository.findByNomeAndOrgId("Produto Teste", 1L)).thenReturn(null);
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Produto salvo = service.salvar(request, 1L);

        assertEquals("Produto Teste", salvo.getNome());
        assertEquals(5, salvo.getQuantidade());
        verify(movimentacaoProdutoRepository).save(any(MovimentacaoProduto.class));
    }

    @Test
    void deveAtualizarProdutoExistenteSomandoQuantidade() {
        produto.setQuantidade(10);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(orgRepository.findById(1L)).thenReturn(Optional.of(org));
        when(produtoRepository.findByNomeAndOrgId("Produto Teste", 1L)).thenReturn(produto);
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Produto salvo = service.salvar(request, 1L);

        assertEquals(15, salvo.getQuantidade()); // 10 existente + 5 da requisição
        verify(movimentacaoProdutoRepository).save(any());
    }

    @Test
    void deveListarProdutosComEstoqueBaixo() {
        Produto p1 = new Produto();
        p1.setQuantidade(2);
        p1.setQuantidadeMinima(5);
        p1.setOrg(org);
        p1.setAtivo(true);
        when(produtoRepository.findByAtivoTrueAndOrgId(1L)).thenReturn(List.of(p1));

        List<Produto> resultado = service.listarProdutosComEstoqueBaixo(1L);

        assertEquals(1, resultado.size());
    }

    @Test
    void deveListarTodosComPaginacao() {
        Page<Produto> pagina = new PageImpl<>(List.of(produto));
        Pageable pageable = PageRequest.of(0, 10);
        when(produtoRepository.findByAtivoTrueAndOrgId(1L, pageable)).thenReturn(pagina);

        Page<Produto> resultado = service.listarTodos(1L, pageable);

        assertEquals(1, resultado.getContent().size());
    }

    @Test
    void deveBuscarPorId() {
        when(produtoRepository.findByIdAndOrgId(1L, 1L)).thenReturn(Optional.of(produto));

        Produto resultado = service.buscarPorId(1L, 1L);

        assertEquals("Produto Teste", resultado.getNome());
    }

    @Test
    void deveLancarErroSeProdutoNaoEncontrado() {
        when(produtoRepository.findByIdAndOrgId(1L, 1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(1L, 1L));
    }

    @Test
    void deveDesativarProdutoAoExcluir() {
        when(produtoRepository.findByIdAndOrgId(1L, 1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.excluir(1L, 1L);

        assertFalse(produto.getAtivo());
    }

    @Test
    void deveVerificarSeProdutoExiste() {
        when(produtoRepository.findByIdAndOrgId(1L, 1L)).thenReturn(Optional.of(produto));

        boolean existe = service.produtoExistente(1L, 1L);

        assertTrue(existe);
    }

    @Test
    void deveRetornarFalseSeProdutoNaoExiste() {
        when(produtoRepository.findByIdAndOrgId(1L, 1L)).thenReturn(Optional.empty());

        assertFalse(service.produtoExistente(1L, 1L));
    }
}
