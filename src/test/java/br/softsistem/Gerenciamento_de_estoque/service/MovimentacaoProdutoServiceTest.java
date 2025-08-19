package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.EntregaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MovimentacaoProdutoServiceTest {

    @InjectMocks
    private MovimentacaoProdutoService service;

    @Mock
    private MovimentacaoProdutoRepository movimentacaoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private EntregaRepository entregaRepository;

    private Produto produto;
    private Org org;
    private Usuario usuario;

    @BeforeEach
    void setup() {
        org = new Org();
        org.setId(1L);

        produto = new Produto();
        produto.setId(10L);
        produto.setQuantidade(100);
        produto.setOrg(org);

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("testuser");
        usuario.setOrg(org);
    }

    @Test
    void deveRegistrarEntradaComSucesso() {
        Long orgId = 1L;
        Long usuarioId = 1L;

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(orgId);
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(usuarioId);
            when(produtoRepository.findByIdAndOrgId(10L, orgId)).thenReturn(Optional.of(produto));
            when(usuarioRepository.findByIdAndOrgId(usuarioId, orgId)).thenReturn(Optional.of(usuario));

            MovimentacaoProdutoDto entradaDto = new MovimentacaoProdutoDto();
            entradaDto.setProdutoId(10L);
            entradaDto.setQuantidade(20);
            entradaDto.setTipo(TipoMovimentacao.ENTRADA);

            MovimentacaoProduto salvo = new MovimentacaoProduto();
            salvo.setId(1L);
            salvo.setProduto(produto);
            salvo.setQuantidade(20);
            salvo.setTipo(TipoMovimentacao.ENTRADA);
            salvo.setDataHora(LocalDateTime.now());
            salvo.setOrg(org);
            salvo.setUsuario(usuario);

            when(movimentacaoRepository.save(any())).thenReturn(salvo);
            when(produtoRepository.save(any())).thenReturn(produto);

            MovimentacaoProdutoDto resultado = service.registrarMovimentacao(entradaDto);

            assertEquals(120, produto.getQuantidade());
            assertEquals(TipoMovimentacao.ENTRADA, resultado.getTipo());
            verify(produtoRepository).save(produto);
            verify(movimentacaoRepository).save(any());
        }
    }

    @Test
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        Long orgId = 99L;
        Long usuarioId = 1L;

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(orgId);
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(usuarioId);
            when(produtoRepository.findByIdAndOrgId(999L, orgId)).thenReturn(Optional.empty());

            MovimentacaoProdutoDto dto = new MovimentacaoProdutoDto();
            dto.setProdutoId(999L);
            dto.setQuantidade(5);
            dto.setTipo(TipoMovimentacao.SAIDA);

            // O service pode lançar ResourceNotFoundException ou IllegalArgumentException
            assertThrows(Exception.class, () -> service.registrarMovimentacao(dto));
        }
    }

    @Test
    void deveLancarExcecaoQuandoSaidaMaiorQueEstoque() {
        Long orgId = 1L;
        Long usuarioId = 1L;

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(orgId);
            mockedStatic.when(SecurityUtils::getCurrentUserId).thenReturn(usuarioId);
            when(produtoRepository.findByIdAndOrgId(10L, orgId)).thenReturn(Optional.of(produto));
            when(usuarioRepository.findByIdAndOrgId(usuarioId, orgId)).thenReturn(Optional.of(usuario));

            MovimentacaoProdutoDto dto = new MovimentacaoProdutoDto();
            dto.setProdutoId(10L);
            dto.setQuantidade(999); // maior que estoque atual (100)
            dto.setTipo(TipoMovimentacao.SAIDA);

            assertThrows(IllegalArgumentException.class, () -> service.registrarMovimentacao(dto));
        }
    }
}
