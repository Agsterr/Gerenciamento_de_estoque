package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.JwtAuthenticationFilter;
import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.service.CategoriaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = CategoriaController.class,
        // Exclui o filtro de JWT para que o Spring não tente criar JwtAuthenticationFilter
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        ),
        // Também desabilita toda a auto‐configuração de segurança do Spring Boot
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoriaService categoriaService;

    @Autowired
    private ObjectMapper objectMapper;

    private Categoria exemploCategoria;
    private CategoriaRequest exemploRequest;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Cria uma entidade Categoria de exemplo
        exemploCategoria = new Categoria();
        exemploCategoria.setId(1L);
        exemploCategoria.setNome("Eletrônicos");
        exemploCategoria.setDescricao("Produtos eletrônicos em geral");

        // Cria um DTO de request de exemplo (record)
        exemploRequest = new CategoriaRequest(
                "Eletrônicos",
                "Produtos eletrônicos em geral"
        );

        // Pageable de exemplo: página 0, tamanho 10
        pageable = PageRequest.of(0, 10);
    }

    @Test
    void listarTodos_ReturnsPageOfCategoriaResponse() throws Exception {
        // Prepara o mock do serviço para retornar uma página contendo a categoria de exemplo
        Page<Categoria> pageMock = new PageImpl<>(
                List.of(exemploCategoria),
                pageable,
                1
        );
        Mockito.when(categoriaService.listarTodos(
                ArgumentMatchers.eq(2L),
                ArgumentMatchers.any(Pageable.class))
        ).thenReturn(pageMock);

        mockMvc.perform(get("/categorias/{orgId}", 2L)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Verifica que há exatamente 1 elemento em "content"
                .andExpect(jsonPath("$.content", hasSize(1)))
                // Verifica os campos id e nome do primeiro elemento
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].nome", is("Eletrônicos")));

        Mockito.verify(categoriaService)
                .listarTodos(ArgumentMatchers.eq(2L), ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void buscarPorNome_WhenFound_ReturnsCategoriaResponse() throws Exception {
        // Mocka o serviço para retornar Optional.of(exemploCategoria)
        Mockito.when(categoriaService.buscarPorNomeEOrgId(
                ArgumentMatchers.eq("Eletrônicos"),
                ArgumentMatchers.eq(2L))
        ).thenReturn(Optional.of(exemploCategoria));

        mockMvc.perform(get("/categorias/{orgId}/nome/{nome}", 2L, "Eletrônicos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nome", is("Eletrônicos")));

        Mockito.verify(categoriaService)
                .buscarPorNomeEOrgId("Eletrônicos", 2L);
    }

    @Test
    void buscarPorNome_WhenNotFound_ReturnsNotFound() throws Exception {
        // Mocka o serviço para retornar Optional.empty()
        Mockito.when(categoriaService.buscarPorNomeEOrgId(
                ArgumentMatchers.eq("Inexistente"),
                ArgumentMatchers.eq(2L))
        ).thenReturn(Optional.empty());

        mockMvc.perform(get("/categorias/{orgId}/nome/{nome}", 2L, "Inexistente"))
                .andExpect(status().isNotFound());

        Mockito.verify(categoriaService)
                .buscarPorNomeEOrgId("Inexistente", 2L);
    }

    @Test
    void buscarPorParteDoNome_ReturnsPageOfCategoriaResponse() throws Exception {
        // 1) Montamos um Page<Categoria> de exemplo
        Page<Categoria> pageMock = new PageImpl<>(
                List.of(exemploCategoria),
                PageRequest.of(0, 10),
                1
        );

        // 2) Stub no serviço: todos os argumentos usando matchers
        Mockito.when(categoriaService.buscarPorParteDoNomeEOrgId(
                Mockito.eq("Eletr"),                   // matcher para o String
                Mockito.eq(2L),                        // matcher para o Long
                Mockito.any(Pageable.class)            // matcher para o Pageable
        )).thenReturn(pageMock);

        // 3) Disparamos a requisição simulada
        mockMvc.perform(get("/categorias/{orgId}/parteDoNome/{parteDoNome}", 2L, "Eletr")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].nome", is("Eletrônicos")));

        // 4) Verificação de que o service foi chamado corretamente
        Mockito.verify(categoriaService).buscarPorParteDoNomeEOrgId(
                Mockito.eq("Eletr"),
                Mockito.eq(2L),
                Mockito.any(Pageable.class)
        );
    }


    @Test
    void salvar_WhenValidRequest_ReturnsCreatedCategoriaResponse() throws Exception {
        // Categoria que o serviço retornará após salvar (com ID preenchido)
        Categoria categoriaSalva = new Categoria();
        categoriaSalva.setId(5L);
        categoriaSalva.setNome(exemploRequest.nome());
        categoriaSalva.setDescricao(exemploRequest.descricao());

        Mockito.when(categoriaService.salvarCategoria(
                ArgumentMatchers.any(CategoriaRequest.class),
                ArgumentMatchers.eq(2L))
        ).thenReturn(categoriaSalva);

        mockMvc.perform(post("/categorias/{orgId}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exemploRequest)))
                .andExpect(status().isCreated()) // 201
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.nome", is("Eletrônicos")));

        Mockito.verify(categoriaService)
                .salvarCategoria(ArgumentMatchers.refEq(exemploRequest), ArgumentMatchers.eq(2L));
    }

    @Test
    void editar_WhenExists_ReturnsUpdatedCategoriaResponse() throws Exception {
        // Categoria que o serviço retornará após edição
        Categoria categoriaEditada = new Categoria();
        categoriaEditada.setId(1L);
        categoriaEditada.setNome("Eletrônicos Atualizado");
        categoriaEditada.setDescricao("Descrição atualizada");

        Mockito.when(categoriaService.editarCategoria(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.any(CategoriaRequest.class),
                ArgumentMatchers.eq(2L))
        ).thenReturn(categoriaEditada);

        var requestAtualizado = new CategoriaRequest(
                "Eletrônicos Atualizado",
                "Descrição atualizada"
        );

        mockMvc.perform(put("/categorias/{orgId}/{id}", 2L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAtualizado)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nome", is("Eletrônicos Atualizado")));

        Mockito.verify(categoriaService).editarCategoria(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.refEq(requestAtualizado),
                ArgumentMatchers.eq(2L)
        );
    }

    @Test
    void editar_WhenNotFound_ReturnsNotFound() throws Exception {
        // Mocka serviço retornando null para indicar “não encontrou”
        Mockito.when(categoriaService.editarCategoria(
                ArgumentMatchers.eq(999L),
                ArgumentMatchers.any(CategoriaRequest.class),
                ArgumentMatchers.eq(2L))
        ).thenReturn(null);

        var qualquerRequest = new CategoriaRequest("NomeX", "DescX");

        mockMvc.perform(put("/categorias/{orgId}/{id}", 2L, 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qualquerRequest)))
                .andExpect(status().isNotFound());

        Mockito.verify(categoriaService).editarCategoria(
                ArgumentMatchers.eq(999L),
                ArgumentMatchers.refEq(qualquerRequest),
                ArgumentMatchers.eq(2L)
        );
    }

    @Test
    void excluir_WhenExists_ReturnsNoContent() throws Exception {
        // Mocka serviço retornando true (exclusão bem‐sucedida)
        Mockito.when(categoriaService.excluirCategoria(
                ArgumentMatchers.eq(1L),
                ArgumentMatchers.eq(2L))
        ).thenReturn(true);

        mockMvc.perform(delete("/categorias/{orgId}/{id}", 2L, 1L))
                .andExpect(status().isNoContent()); // 204

        Mockito.verify(categoriaService).excluirCategoria(1L, 2L);
    }

    @Test
    void excluir_WhenNotFound_ReturnsNotFound() throws Exception {
        // Mocka serviço retornando false (não encontrou)
        Mockito.when(categoriaService.excluirCategoria(
                ArgumentMatchers.eq(999L),
                ArgumentMatchers.eq(2L))
        ).thenReturn(false);

        mockMvc.perform(delete("/categorias/{orgId}/{id}", 2L, 999L))
                .andExpect(status().isNotFound());

        Mockito.verify(categoriaService).excluirCategoria(999L, 2L);
    }
}
