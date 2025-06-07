package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.MovimentacaoProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimentacaoProdutoServiceTest {

    @InjectMocks
    private MovimentacaoProdutoService service;

    @Mock
    private MovimentacaoProdutoRepository movimentacaoRepository;

    @Mock
    private ProdutoRepository produtoRepository;

    private Produto produto;
    private Org org;

    @BeforeEach
    void setup() {
        org = new Org();
        org.setId(1L);

        produto = new Produto();
        produto.setId(10L);
        produto.setQuantidade(100);
        produto.setOrg(org);
    }

    @Test
    void deveRegistrarEntradaComSucesso() {
        Long orgId = 1L;

        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(orgId);
            when(produtoRepository.findByIdAndOrgId(10L, orgId)).thenReturn(Optional.of(produto));

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
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(99L);
            when(produtoRepository.findByIdAndOrgId(999L, 99L)).thenReturn(Optional.empty());

            MovimentacaoProdutoDto dto = new MovimentacaoProdutoDto();
            dto.setProdutoId(999L);
            dto.setQuantidade(5);
            dto.setTipo(TipoMovimentacao.SAIDA);

            assertThrows(ResourceNotFoundException.class, () -> service.registrarMovimentacao(dto));
        }
    }

    @Test
    void deveLancarExcecaoQuandoSaidaMaiorQueEstoque() {
        try (MockedStatic<SecurityUtils> mockedStatic = mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(1L);
            when(produtoRepository.findByIdAndOrgId(10L, 1L)).thenReturn(Optional.of(produto));

            MovimentacaoProdutoDto dto = new MovimentacaoProdutoDto();
            dto.setProdutoId(10L);
            dto.setQuantidade(999); // maior que estoque atual (100)
            dto.setTipo(TipoMovimentacao.SAIDA);

            assertThrows(IllegalArgumentException.class, () -> service.registrarMovimentacao(dto));
        }
    }
}
