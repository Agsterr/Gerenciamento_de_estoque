package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.JwtAuthenticationFilter;
import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import br.softsistem.Gerenciamento_de_estoque.service.MovimentacaoProdutoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = MovimentacaoProdutoController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class MovimentacaoProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MovimentacaoProdutoService movimentacaoProdutoService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private MovimentacaoProdutoDto dtoEntrada;
    private MovimentacaoProdutoDto dtoSaida;

    @BeforeEach
    void setUp() {
        dtoEntrada = new MovimentacaoProdutoDto();
        dtoEntrada.setId(1L);
        dtoEntrada.setProdutoId(10L);
        dtoEntrada.setNomeProduto("Produto A");
        dtoEntrada.setQuantidade(5);
        dtoEntrada.setDataHora(LocalDateTime.now());
        dtoEntrada.setTipo(TipoMovimentacao.ENTRADA);
        dtoEntrada.setOrgId(100L);

        dtoSaida = new MovimentacaoProdutoDto();
        dtoSaida.setId(2L);
        dtoSaida.setProdutoId(20L);
        dtoSaida.setNomeProduto("Produto B");
        dtoSaida.setQuantidade(2);
        dtoSaida.setDataHora(LocalDateTime.now());
        dtoSaida.setTipo(TipoMovimentacao.SAIDA);
        dtoSaida.setOrgId(100L);
    }

    @Test
    void registrar_DeveRetornarMovimentacaoRegistrada() throws Exception {
        when(movimentacaoProdutoService.registrarMovimentacao(any(MovimentacaoProdutoDto.class)))
                .thenReturn(dtoEntrada);

        mockMvc.perform(post("/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoEntrada)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nomeProduto").value("Produto A"))
                .andExpect(jsonPath("$.tipo").value("ENTRADA"));
    }

    @Test
    void porData_DeveRetornarListaMovimentacaoPorData() throws Exception {
        List<MovimentacaoProdutoDto> lista = Arrays.asList(dtoEntrada);
        LocalDate data = LocalDate.now();
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista);
        when(movimentacaoProdutoService.buscarPorData(eq(TipoMovimentacao.ENTRADA), eq(data), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-data")
                        .param("tipo", "ENTRADA")
                        .param("data", data.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].nomeProduto").value("Produto A"));
    }

    @Test
    void porPeriodo_DeveRetornarListaMovimentacaoPorPeriodo() throws Exception {
        List<MovimentacaoProdutoDto> lista = Arrays.asList(dtoSaida);
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista);

        when(movimentacaoProdutoService.buscarPorPeriodo(
                eq(TipoMovimentacao.SAIDA),
                eq(inicio),
                eq(fim),
                any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-periodo")
                        .param("tipo", "SAIDA")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2L))
                .andExpect(jsonPath("$.content[0].nomeProduto").value("Produto B"));
    }

    @Test
    void porAno_DeveRetornarListaMovimentacaoPorAno() throws Exception {
        List<MovimentacaoProdutoDto> lista = Arrays.asList(dtoEntrada, dtoSaida);
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista);
        when(movimentacaoProdutoService.listarDetalhadoPorAno(eq(2025), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-ano")
                        .param("ano", "2025"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[1].id").value(2L));
    }

    @Test
    void porMes_DeveRetornarListaMovimentacaoPorMes() throws Exception {
        List<MovimentacaoProdutoDto> lista = Arrays.asList(dtoEntrada);
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista);
        when(movimentacaoProdutoService.listarDetalhadoPorMes(eq(2025), eq(5), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-mes")
                        .param("ano", "2025")
                        .param("mes", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].nomeProduto").value("Produto A"));
    }
}
