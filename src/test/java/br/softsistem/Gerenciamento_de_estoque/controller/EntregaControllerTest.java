package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.EntregaService;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = EntregaController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
class EntregaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntregaService entregaService;


    @MockBean
    private UserDetailsService userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtService jwtService;


    private Entrega gerarEntregaMock() {
        Consumidor consumidor = new Consumidor();
        consumidor.setNome("Carlos");

        Produto produto = new Produto();
        produto.setNome("Produto Teste");

        Usuario entregador = new Usuario();
        entregador.setUsername("Entregador Teste");

        Entrega entrega = new Entrega();
        entrega.setId(1L);
        entrega.setConsumidor(consumidor);
        entrega.setProduto(produto);
        entrega.setEntregador(entregador);
        entrega.setQuantidade(10);
        entrega.setHorarioEntrega(LocalDateTime.now());
        return entrega;
    }

    @Test
    void criarEntrega() throws Exception {
        EntregaRequestDto request = new EntregaRequestDto();
        request.setConsumidorId(1L);
        request.setProdutoId(1L);
        request.setQuantidade(10);
        request.setHorarioEntrega(LocalDateTime.now());

        Entrega entregaMock = gerarEntregaMock();

        Mockito.when(entregaService.criarEntrega(any(EntregaRequestDto.class))).thenReturn(entregaMock);

        mockMvc.perform(post("/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.nomeConsumidor").value("Carlos"))
                .andExpect(jsonPath("$.nomeProduto").value("Produto Teste"))
                .andExpect(jsonPath("$.nomeEntregador").value("Entregador Teste"));
    }

    @Test
    void editarEntrega() throws Exception {
        EntregaRequestDto request = new EntregaRequestDto();
        request.setConsumidorId(1L);
        request.setProdutoId(1L);
        request.setQuantidade(15);
        request.setHorarioEntrega(LocalDateTime.now());

        Entrega entregaMock = gerarEntregaMock();
        entregaMock.setQuantidade(15);

        Mockito.when(entregaService.editarEntrega(anyLong(), any(EntregaRequestDto.class))).thenReturn(entregaMock);

        mockMvc.perform(put("/entregas/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.quantidade").value(15));
    }

    @Test
    void deletarEntrega() throws Exception {
        Mockito.doNothing().when(entregaService).deletarEntrega(1L);

        mockMvc.perform(delete("/entregas/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listarEntregas() throws Exception {
        Entrega entregaMock = gerarEntregaMock();
        Page<Entrega> page = new PageImpl<>(List.of(entregaMock));

        Mockito.when(entregaService.listarEntregas(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/entregas")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void porDia() throws Exception {
        LocalDate dia = LocalDate.now();
        Entrega entregaMock = gerarEntregaMock();

        Mockito.when(entregaService.listarEntregasPorDia(any(LocalDate.class))).thenReturn(List.of(entregaMock).stream().map(EntregaResponseDto::fromEntity).toList());

        mockMvc.perform(get("/entregas/por-dia")
                        .param("dia", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void porPeriodo() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2025, 6, 30, 23, 59);
        Entrega entregaMock = gerarEntregaMock();

        Mockito.when(entregaService.listarEntregasPorPeriodo(any(), any())).thenReturn(List.of(entregaMock).stream().map(EntregaResponseDto::fromEntity).toList());

        mockMvc.perform(get("/entregas/por-periodo")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void porMes() throws Exception {
        int mes = 6;
        int ano = 2025;
        Entrega entregaMock = gerarEntregaMock();

        Mockito.when(entregaService.listarEntregasPorMes(any(Integer.class), any(Integer.class))).thenReturn(List.of(entregaMock).stream().map(EntregaResponseDto::fromEntity).toList());

        mockMvc.perform(get("/entregas/por-mes")
                        .param("mes", String.valueOf(mes))
                        .param("ano", String.valueOf(ano)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void porAno() throws Exception {
        int ano = 2025;
        Entrega entregaMock = gerarEntregaMock();

        Mockito.when(entregaService.listarEntregasPorAno(any(Integer.class))).thenReturn(List.of(entregaMock).stream().map(EntregaResponseDto::fromEntity).toList());

        mockMvc.perform(get("/entregas/por-ano")
                        .param("ano", String.valueOf(ano)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void porConsumidor() throws Exception {
        Entrega entregaMock = gerarEntregaMock();

        Mockito.when(entregaService.listarEntregasPorConsumidor(anyLong())).thenReturn(List.of(entregaMock).stream().map(EntregaResponseDto::fromEntity).toList());

        mockMvc.perform(get("/entregas/por-consumidor/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void porConsumidorPeriodo() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2025, 6, 30, 23, 59);
        Entrega entregaMock = gerarEntregaMock();

        Mockito.when(entregaService.listarEntregasPorConsumidorPorPeriodo(anyLong(), any(), any())).thenReturn(List.of(entregaMock).stream().map(EntregaResponseDto::fromEntity).toList());

        mockMvc.perform(get("/entregas/por-consumidor/1/periodo")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }
}
