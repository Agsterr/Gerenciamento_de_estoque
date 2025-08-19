package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProdutoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProdutoService produtoService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ProdutoController controller = new ProdutoController(produtoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listarTodos_deveRetornarPaginaDeProdutos() throws Exception {
        Produto produto = criarProduto();
        Page<Produto> page = new PageImpl<>(java.util.List.of(produto), PageRequest.of(0,10), 1);
        Mockito.when(produtoService.listarTodos(anyLong(), any(Pageable.class))).thenReturn(page);

        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(1L);

            mockMvc.perform(get("/produtos")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].nome").value("Produto Teste"));
        }
    }

    @Test
    void criarProduto_deveRetornarMensagemSucesso() throws Exception {
        ProdutoRequest request = criarProdutoRequest();

        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(1L);

            Mockito.when(produtoService.salvar(any(ProdutoRequest.class), eq(1L)))
                    .thenReturn(new Produto());

            mockMvc.perform(post("/produtos")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Produto criado ou atualizado com sucesso!"));
        }
    }

    @Test
    void excluir_deveRetornarMensagemSucesso() throws Exception {
        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(1L);

            mockMvc.perform(delete("/produtos/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Produto excluído com sucesso."));
        }
    }

    @Test
    void buscarPorId_deveRetornarProdutoDto() throws Exception {
        Produto produto = criarProduto();
        Mockito.when(produtoService.buscarPorId(1L, 1L)).thenReturn(produto);

        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(1L);

            mockMvc.perform(get("/produtos/1")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nome").value("Produto Teste"));
        }
    }

    @Test
    void listarProdutosComEstoqueBaixo_deveRetornarProdutosFiltrados() throws Exception {
        Produto produto = criarProduto();
        produto.setQuantidade(3);
        produto.setQuantidadeMinima(5);

        Mockito.when(produtoService.listarProdutosComEstoqueBaixo(1L))
                .thenReturn(java.util.List.of(produto));

        try (MockedStatic<SecurityUtils> mockedStatic = Mockito.mockStatic(SecurityUtils.class)) {
            mockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(1L);

            mockMvc.perform(get("/produtos/estoque-baixo")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].nome").value("Produto Teste"))
                    .andExpect(jsonPath("$[0].quantidade").value(3))
                    .andExpect(jsonPath("$[0].quantidadeMinima").value(5));
        }
    }

    // Métodos auxiliares

    private Produto criarProduto() {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto Teste");
        produto.setDescricao("Descrição");
        produto.setPreco(new BigDecimal("10.00"));
        produto.setQuantidade(10);
        produto.setQuantidadeMinima(5);
        produto.setCriadoEm(LocalDateTime.now());
        produto.setAtivo(true);

        Org org = new Org();
        org.setId(1L);
        org.setNome("Org");
        produto.setOrg(org);

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Categoria");
        categoria.setDescricao("Descrição categoria");
        categoria.setOrg(org);
        produto.setCategoria(categoria);

        return produto;
    }

    private ProdutoRequest criarProdutoRequest() {
        ProdutoRequest request = new ProdutoRequest();
        request.setNome("Produto Teste");
        request.setDescricao("Descrição");
        request.setPreco(new BigDecimal("10.00"));
        request.setQuantidade(10);
        request.setQuantidadeMinima(5);
        request.setCategoriaId(1L);
        request.setOrgId(1L);
        return request;
    }
}
