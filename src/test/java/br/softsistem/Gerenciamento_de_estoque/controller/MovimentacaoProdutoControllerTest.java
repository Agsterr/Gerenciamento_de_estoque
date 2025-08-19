package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.service.MovimentacaoProdutoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MovimentacaoProdutoControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MovimentacaoProdutoService movimentacaoProdutoService;

    private ObjectMapper objectMapper;

    private MovimentacaoProdutoDto dtoEntrada;
    private MovimentacaoProdutoDto dtoSaida;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MovimentacaoProdutoController controller = new MovimentacaoProdutoController(movimentacaoProdutoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

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
                        .accept(MediaType.APPLICATION_JSON)
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
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista, PageRequest.of(0,10), lista.size());
        when(movimentacaoProdutoService.buscarPorData(eq(TipoMovimentacao.ENTRADA), eq(data), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-data")
                        .param("tipo", "ENTRADA")
                        .param("data", data.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].nomeProduto").value("Produto A"));
    }

    @Test
    void porPeriodo_DeveRetornarListaMovimentacaoPorPeriodo() throws Exception {
        List<MovimentacaoProdutoDto> lista = Arrays.asList(dtoSaida);
        LocalDateTime inicio = LocalDateTime.now().minusDays(1);
        LocalDateTime fim = LocalDateTime.now();
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista, PageRequest.of(0,10), lista.size());

        when(movimentacaoProdutoService.buscarPorPeriodo(eq(TipoMovimentacao.SAIDA), eq(inicio), eq(fim), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-periodo")
                        .param("tipo", "SAIDA")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(2L))
                .andExpect(jsonPath("$.content[0].nomeProduto").value("Produto B"));
    }

    @Test
    void porAno_DeveRetornarListaMovimentacaoPorAno() throws Exception {
        List<MovimentacaoProdutoDto> lista = Arrays.asList(dtoEntrada, dtoSaida);
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista, PageRequest.of(0,10), lista.size());
        when(movimentacaoProdutoService.listarDetalhadoPorAno(eq(2025), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-ano")
                        .param("ano", "2025")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[1].id").value(2L));
    }

    @Test
    void porMes_DeveRetornarListaMovimentacaoPorMes() throws Exception {
        List<MovimentacaoProdutoDto> lista = Arrays.asList(dtoEntrada);
        Page<MovimentacaoProdutoDto> page = new PageImpl<>(lista, PageRequest.of(0,10), lista.size());
        when(movimentacaoProdutoService.listarDetalhadoPorMes(eq(2025), eq(5), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/movimentacoes/por-mes")
                        .param("ano", "2025")
                        .param("mes", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L))
                .andExpect(jsonPath("$.content[0].nomeProduto").value("Produto A"));
    }
}
