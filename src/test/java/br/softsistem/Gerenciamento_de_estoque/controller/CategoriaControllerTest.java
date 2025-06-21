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
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        ),
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
        exemploCategoria = new Categoria();
        exemploCategoria.setId(1L);
        exemploCategoria.setNome("Eletrônicos");
        exemploCategoria.setDescricao("Produtos eletrônicos em geral");

        exemploRequest = new CategoriaRequest(
                "Eletrônicos",
                "Produtos eletrônicos em geral"
        );

        pageable = PageRequest.of(0, 10);
    }

    @Test
    void listarTodos_ReturnsPageOfCategoriaResponse() throws Exception {
        Page<Categoria> pageMock = new PageImpl<>(List.of(exemploCategoria), pageable, 1);
        Mockito.when(categoriaService.listarTodos(ArgumentMatchers.eq(2L), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(pageMock);

        mockMvc.perform(get("/categorias/{orgId}", 2L)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].nome", is("Eletrônicos")));

        Mockito.verify(categoriaService).listarTodos(ArgumentMatchers.eq(2L), ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void buscarPorNome_WhenFound_ReturnsCategoriaResponse() throws Exception {
        Mockito.when(categoriaService.buscarPorNomeEOrgId("Eletrônicos", 2L))
                .thenReturn(Optional.of(exemploCategoria));

        mockMvc.perform(get("/categorias/{orgId}/nome/{nome}", 2L, "Eletrônicos"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nome", is("Eletrônicos")));

        Mockito.verify(categoriaService).buscarPorNomeEOrgId("Eletrônicos", 2L);
    }

    @Test
    void buscarPorNome_WhenNotFound_ReturnsNotFound() throws Exception {
        Mockito.when(categoriaService.buscarPorNomeEOrgId("Inexistente", 2L))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/categorias/{orgId}/nome/{nome}", 2L, "Inexistente"))
                .andExpect(status().isNotFound());

        Mockito.verify(categoriaService).buscarPorNomeEOrgId("Inexistente", 2L);
    }

    @Test
    void buscarPorParteDoNome_ReturnsPageOfCategoriaResponse() throws Exception {
        Page<Categoria> pageMock = new PageImpl<>(List.of(exemploCategoria), pageable, 1);
        Mockito.when(categoriaService.buscarPorParteDoNomeEOrgId("Eletr", 2L, pageable))
                .thenReturn(pageMock);

        mockMvc.perform(get("/categorias/{orgId}/parte-do-nome/{parteDoNome}", 2L, "Eletr")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].nome", is("Eletrônicos")));

        Mockito.verify(categoriaService).buscarPorParteDoNomeEOrgId("Eletr", 2L, pageable);
    }

    @Test
    void salvar_WhenValidRequest_ReturnsCreatedCategoriaResponse() throws Exception {
        Categoria categoriaSalva = new Categoria();
        categoriaSalva.setId(5L);
        categoriaSalva.setNome(exemploRequest.nome());
        categoriaSalva.setDescricao(exemploRequest.descricao());

        Mockito.when(categoriaService.salvarCategoria(ArgumentMatchers.any(CategoriaRequest.class), ArgumentMatchers.eq(2L)))
                .thenReturn(categoriaSalva);

        mockMvc.perform(post("/categorias/{orgId}", 2L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(exemploRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(5)))
                .andExpect(jsonPath("$.nome", is("Eletrônicos")));

        Mockito.verify(categoriaService).salvarCategoria(ArgumentMatchers.refEq(exemploRequest), ArgumentMatchers.eq(2L));
    }

    @Test
    void editar_WhenExists_ReturnsUpdatedCategoriaResponse() throws Exception {
        Categoria categoriaEditada = new Categoria();
        categoriaEditada.setId(1L);
        categoriaEditada.setNome("Eletrônicos Atualizado");
        categoriaEditada.setDescricao("Descrição atualizada");

        Mockito.when(categoriaService.editarCategoria(ArgumentMatchers.eq(1L), ArgumentMatchers.any(CategoriaRequest.class), ArgumentMatchers.eq(2L)))
                .thenReturn(categoriaEditada);

        var requestAtualizado = new CategoriaRequest("Eletrônicos Atualizado", "Descrição atualizada");

        mockMvc.perform(put("/categorias/{orgId}/{id}", 2L, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAtualizado)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.nome", is("Eletrônicos Atualizado")));

        Mockito.verify(categoriaService).editarCategoria(ArgumentMatchers.eq(1L), ArgumentMatchers.refEq(requestAtualizado), ArgumentMatchers.eq(2L));
    }

    @Test
    void editar_WhenNotFound_ReturnsNotFound() throws Exception {
        Mockito.when(categoriaService.editarCategoria(ArgumentMatchers.eq(999L), ArgumentMatchers.any(CategoriaRequest.class), ArgumentMatchers.eq(2L)))
                .thenReturn(null);

        var requestAtualizado = new CategoriaRequest("NomeX", "DescX");

        mockMvc.perform(put("/categorias/{orgId}/{id}", 2L, 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAtualizado)))
                .andExpect(status().isNotFound());

        Mockito.verify(categoriaService).editarCategoria(ArgumentMatchers.eq(999L), ArgumentMatchers.refEq(requestAtualizado), ArgumentMatchers.eq(2L));
    }

    @Test
    void excluir_WhenExists_ReturnsNoContent() throws Exception {
        Mockito.when(categoriaService.excluirCategoria(ArgumentMatchers.eq(1L), ArgumentMatchers.eq(2L)))
                .thenReturn(true);

        mockMvc.perform(delete("/categorias/{orgId}/{id}", 2L, 1L))
                .andExpect(status().isNoContent());

        Mockito.verify(categoriaService).excluirCategoria(1L, 2L);
    }

    @Test
    void excluir_WhenNotFound_ReturnsNotFound() throws Exception {
        Mockito.when(categoriaService.excluirCategoria(ArgumentMatchers.eq(999L), ArgumentMatchers.eq(2L)))
                .thenReturn(false);

        mockMvc.perform(delete("/categorias/{orgId}/{id}", 2L, 999L))
                .andExpect(status().isNotFound());

        Mockito.verify(categoriaService).excluirCategoria(999L, 2L);
    }
}
