package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaComAvisoResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.EntregaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EntregaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EntregaService entregaService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        EntregaController controller = new EntregaController(entregaService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

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

        EntregaResponseDto entregaDto = new EntregaResponseDto(
                1L,
                "Carlos",
                "Produto Teste",
                "Entregador Teste",
                10,
                LocalDateTime.now(),
                1L,
                1L
        );
        EntregaComAvisoResponseDto responseDto = new EntregaComAvisoResponseDto(entregaDto,false,null);
        Mockito.when(entregaService.criarEntrega(any(EntregaRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entrega.id").value(1))
                .andExpect(jsonPath("$.entrega.nomeConsumidor").value("Carlos"))
                .andExpect(jsonPath("$.entrega.nomeProduto").value("Produto Teste"))
                .andExpect(jsonPath("$.entrega.nomeEntregador").value("Entregador Teste"))
                .andExpect(jsonPath("$.entrega.produtoId").value(1))
                .andExpect(jsonPath("$.entrega.consumidorId").value(1))
                .andExpect(jsonPath("$.estoqueBaixo").value(false))
                .andExpect(jsonPath("$.mensagemEstoqueBaixo").doesNotExist());
    }

    @Test
    void criarEntregaComAvisoDeEstoqueBaixo() throws Exception {
        EntregaRequestDto request = new EntregaRequestDto();
        request.setConsumidorId(1L);
        request.setProdutoId(1L);
        request.setQuantidade(50);
        request.setHorarioEntrega(LocalDateTime.now());

        EntregaResponseDto entregaDto = new EntregaResponseDto(
                2L,
                "João",
                "Produto Crítico",
                "Entregador Teste",
                50,
                LocalDateTime.now(),
                1L,
                1L
        );
        String aviso = "⚠ Estoque do produto 'Produto Crítico' está abaixo do mínimo! Atual: 5 | Mínimo: 10";
        EntregaComAvisoResponseDto responseDto = new EntregaComAvisoResponseDto(entregaDto,true,aviso);
        Mockito.when(entregaService.criarEntrega(any(EntregaRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entrega.id").value(2))
                .andExpect(jsonPath("$.estoqueBaixo").value(true))
                .andExpect(jsonPath("$.mensagemEstoqueBaixo").value(aviso))
                .andExpect(jsonPath("$.entrega.produtoId").value(1))
                .andExpect(jsonPath("$.entrega.consumidorId").value(1));
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
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.quantidade").value(15));
    }

    @Test
    void deletarEntrega() throws Exception {
        Mockito.doNothing().when(entregaService).deletarEntrega(1L);
        mockMvc.perform(delete("/entregas/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void listarEntregas() throws Exception {
        Entrega entregaMock = gerarEntregaMock();
        java.util.List<Entrega> content = new java.util.ArrayList<>();
        content.add(entregaMock);
        Page<Entrega> page = new PageImpl<>(content, PageRequest.of(0,10), content.size());
        Mockito.when(entregaService.listarEntregas(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/entregas")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void porDia() throws Exception {
        LocalDate dia = LocalDate.now();
        Entrega entregaMock = gerarEntregaMock();
        Page<EntregaResponseDto> pageMock = new PageImpl<>(List.of(entregaMock).stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList()), PageRequest.of(0, 10), 1);
        Mockito.when(entregaService.listarEntregasPorDia(eq(dia), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/entregas/por-dia")
                        .param("dia", dia.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void porPeriodo() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime fim = LocalDateTime.of(2025, 6, 30, 23, 59);
        Entrega entregaMock = gerarEntregaMock();
        Page<EntregaResponseDto> pageMock = new PageImpl<>(List.of(entregaMock).stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList()), PageRequest.of(0, 10), 1);
        Mockito.when(entregaService.listarEntregasPorPeriodo(eq(inicio), eq(fim), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/entregas/por-periodo")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void porMes() throws Exception {
        int mes = 6; int ano = 2025; Entrega entregaMock = gerarEntregaMock();
        Page<EntregaResponseDto> pageMock = new PageImpl<>(List.of(entregaMock).stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList()), PageRequest.of(0, 10), 1);
        Mockito.when(entregaService.listarEntregasPorMes(eq(mes), eq(ano), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/entregas/por-mes")
                        .param("mes", String.valueOf(mes))
                        .param("ano", String.valueOf(ano))
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void porAno() throws Exception {
        int ano = 2025; Entrega entregaMock = gerarEntregaMock();
        Page<EntregaResponseDto> pageMock = new PageImpl<>(List.of(entregaMock).stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList()), PageRequest.of(0, 10), 1);
        Mockito.when(entregaService.listarEntregasPorAno(eq(ano), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/entregas/por-ano")
                        .param("ano", String.valueOf(ano))
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void porConsumidor() throws Exception {
        Entrega entregaMock = gerarEntregaMock();
        Page<EntregaResponseDto> pageMock = new PageImpl<>(List.of(entregaMock).stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList()), PageRequest.of(0, 10), 1);
        Mockito.when(entregaService.listarEntregasPorConsumidor(eq(1L), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/entregas/por-consumidor/1")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

    @Test
    void porConsumidorPeriodo() throws Exception {
        LocalDateTime inicio = LocalDateTime.of(2025, 6, 1, 0, 0); LocalDateTime fim = LocalDateTime.of(2025, 6, 30, 23, 59);
        Entrega entregaMock = gerarEntregaMock();
        Page<EntregaResponseDto> pageMock = new PageImpl<>(List.of(entregaMock).stream()
                .map(EntregaResponseDto::fromEntity)
                .collect(Collectors.toList()), PageRequest.of(0, 10), 1);
        Mockito.when(entregaService.listarEntregasPorConsumidorPorPeriodo(eq(1L), eq(inicio), eq(fim), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/entregas/por-consumidor/1/periodo")
                        .param("inicio", inicio.toString())
                        .param("fim", fim.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1L));
    }

}
