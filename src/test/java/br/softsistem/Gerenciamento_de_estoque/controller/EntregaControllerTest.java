package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaComAvisoResponseDto;
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
        // Monta request
        EntregaRequestDto request = new EntregaRequestDto();
        request.setConsumidorId(1L);
        request.setProdutoId(1L);
        request.setQuantidade(10);
        request.setHorarioEntrega(LocalDateTime.now());

        // Monta EntregaResponseDto simulado
        EntregaResponseDto entregaDto = new EntregaResponseDto(
                1L, // ID
                "Carlos", // Nome do consumidor
                "Produto Teste", // Nome do produto
                "Entregador Teste", // Nome do entregador
                10, // Quantidade
                LocalDateTime.now(), // Horário da entrega
                1L, // produtoId
                1L  // consumidorId
        );

        // Simula resposta sem aviso de estoque baixo
        EntregaComAvisoResponseDto responseDto = new EntregaComAvisoResponseDto(
                entregaDto,
                false,      // estoqueBaixo
                null        // mensagemEstoqueBaixo
        );

        // Configura o mock do serviço
        Mockito.when(entregaService.criarEntrega(any(EntregaRequestDto.class)))
                .thenReturn(responseDto);

        // Realiza a requisição POST e valida a resposta
        mockMvc.perform(post("/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())  // Verifica se o status é 201 Created
                // Valida os dados da entrega retornada
                .andExpect(jsonPath("$.entrega.id").value(1))
                .andExpect(jsonPath("$.entrega.nomeConsumidor").value("Carlos"))
                .andExpect(jsonPath("$.entrega.nomeProduto").value("Produto Teste"))
                .andExpect(jsonPath("$.entrega.nomeEntregador").value("Entregador Teste"))
                .andExpect(jsonPath("$.entrega.produtoId").value(1)) // Verifica produtoId
                .andExpect(jsonPath("$.entrega.consumidorId").value(1)) // Verifica consumidorId
                // Valida a nova flag e a ausência de mensagem de estoque baixo
                .andExpect(jsonPath("$.estoqueBaixo").value(false))
                .andExpect(jsonPath("$.mensagemEstoqueBaixo").doesNotExist());
    }



    @Test
    void criarEntregaComAvisoDeEstoqueBaixo() throws Exception {
        // Prepara a requisição (DTO) que será enviada para a API
        EntregaRequestDto request = new EntregaRequestDto();
        request.setConsumidorId(1L);  // Consumidor ID
        request.setProdutoId(1L);     // Produto ID
        request.setQuantidade(50);    // Quantidade solicitada
        request.setHorarioEntrega(LocalDateTime.now()); // Horário da entrega

        // Prepara a resposta que o serviço irá retornar
        EntregaResponseDto entregaDto = new EntregaResponseDto(
                2L, // ID da entrega
                "João", // Nome do consumidor
                "Produto Crítico", // Nome do produto
                "Entregador Teste", // Nome do entregador
                50, // Quantidade
                LocalDateTime.now(), // Horário da entrega
                1L, // produtoId
                1L  // consumidorId
        );

        // Aviso de estoque baixo
        String avisoMensagem = "⚠ Estoque do produto 'Produto Crítico' está abaixo do mínimo! Atual: 5 | Mínimo: 10";

        // Prepara o DTO de resposta com o aviso de estoque baixo
        EntregaComAvisoResponseDto responseDto = new EntregaComAvisoResponseDto(
                entregaDto,
                true,           // Estoque baixo
                avisoMensagem   // Mensagem do aviso de estoque baixo
        );

        // Configura o comportamento do mock para retornar a resposta desejada
        Mockito.when(entregaService.criarEntrega(any(EntregaRequestDto.class)))
                .thenReturn(responseDto);

        // Realiza a requisição POST para o endpoint de criar entrega
        mockMvc.perform(post("/entregas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())  // Espera que a resposta seja 201 Created
                .andExpect(jsonPath("$.entrega.id").value(2)) // Verifica o ID da entrega
                .andExpect(jsonPath("$.estoqueBaixo").value(true)) // Verifica se o estoque está baixo
                .andExpect(jsonPath("$.mensagemEstoqueBaixo").value(avisoMensagem)) // Verifica a mensagem de aviso
                .andExpect(jsonPath("$.entrega.produtoId").value(1L)) // Verifica o produtoId
                .andExpect(jsonPath("$.entrega.consumidorId").value(1L)); // Verifica o consumidorId
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
