package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.JwtAuthenticationFilter;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import br.softsistem.Gerenciamento_de_estoque.service.OrgService;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProdutoController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProdutoService produtoService;

    @MockBean
    private OrgService orgService;

    @MockBean
    private JwtService jwtService;


    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;


    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listarTodos_deveRetornarPaginaDeProdutos() throws Exception {
        Produto produto = criarProduto();
        Page<Produto> page = new PageImpl<>(java.util.List.of(produto));
        Mockito.when(produtoService.listarTodos(anyLong(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/produtos")
                        .param("orgId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Produto Teste"));
    }

    @Test
    void criarProduto_deveRetornarMensagemSucesso() throws Exception {
        ProdutoRequest request = criarProdutoRequest();
        Mockito.when(produtoService.salvar(any(ProdutoRequest.class), anyLong())).thenReturn(new Produto());

        mockMvc.perform(post("/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Produto criado ou atualizado com sucesso!"));
    }

    @Test
    void excluir_deveRetornarMensagemSucesso() throws Exception {
        mockMvc.perform(delete("/produtos/1")
                        .param("orgId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Produto excluído com sucesso."));
    }

    @Test
    void buscarPorId_deveRetornarProdutoDto() throws Exception {
        Produto produto = criarProduto();
        Mockito.when(produtoService.buscarPorId(1L, 1L)).thenReturn(produto);

        mockMvc.perform(get("/produtos/1")
                        .param("orgId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produto Teste"));
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

        // Criar e configurar Org
        Org org = new Org();
        org.setId(1L);
        org.setNome("Org");
        produto.setOrg(org);

        // Criar e configurar Categoria
        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Categoria");
        categoria.setDescricao("Descrição categoria");
        categoria.setOrg(org); // Relacionamento obrigatório
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

    @Test
    void listarProdutosComEstoqueBaixo_deveRetornarProdutosFiltrados() throws Exception {
        Produto produto = criarProduto();
        // Simula que o estoque está abaixo do mínimo
        produto.setQuantidade(3);
        produto.setQuantidadeMinima(5);

        Mockito.when(produtoService.listarProdutosComEstoqueBaixo(1L))
                .thenReturn(java.util.List.of(produto));

        mockMvc.perform(get("/produtos/estoque-baixo")
                        .param("orgId", "1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Produto Teste"))
                .andExpect(jsonPath("$[0].quantidade").value(3))
                .andExpect(jsonPath("$[0].quantidadeMinima").value(5));
    }

}
