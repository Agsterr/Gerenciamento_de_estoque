package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.JwtAuthenticationFilter;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.ConsumidorNaoEncontradoException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.service.ConsumidorService;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ConsumidorController.class,
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
public class ConsumidorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsumidorService consumidorService;

    @MockBean
    private ConsumidorRepository consumidorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private ConsumidorDtoRequest consumidorDtoRequest;
    private Consumidor consumidor;

    private MockedStatic<SecurityUtils> securityUtilsMockedStatic;

    @BeforeEach
    void setUp() {
        consumidorDtoRequest = new ConsumidorDtoRequest(
                null,
                "João Silva",
                "12345678900",
                "Rua das Flores, 100",
                2L
        );

        consumidor = new Consumidor();
        consumidor.setId(1L);
        consumidor.setNome("João Silva");
        consumidor.setCpf("12345678900");
        consumidor.setEndereco("Rua das Flores, 100");

        securityUtilsMockedStatic = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(2L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMockedStatic.close();
    }

    @Test
    void criarConsumidor() throws Exception {
        Mockito.when(consumidorService.salvar(any(Consumidor.class))).thenReturn(consumidor);

        mockMvc.perform(
                        post("/consumidores")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(consumidorDtoRequest))
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(consumidor.getId()))
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678900"))
                .andExpect(jsonPath("$.endereco").value("Rua das Flores, 100"));
    }

    @Test
    void listarTodos() throws Exception {
        // Mock da organização
        Org org = new Org();
        org.setId(1L);

        // Mock do consumidor
        Consumidor consumidor = new Consumidor();
        consumidor.setId(4L);
        consumidor.setNome("Carlos Alberto azevedo");
        consumidor.setCpf("12312992399");
        consumidor.setEndereco(null);
        consumidor.setOrg(org);

        // Mock da resposta do repositório
        Page<Consumidor> pageMock = new PageImpl<>(List.of(consumidor));
        Mockito.when(consumidorRepository.findByOrg_Id(eq(2L), any(Pageable.class)))
                .thenReturn(pageMock);

        // Executa a requisição
        mockMvc.perform(get("/consumidores")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.content[0].nome").value("Carlos Alberto azevedo"))
                .andExpect(jsonPath("$.content[0].cpf").value("12312992399"))
                .andExpect(jsonPath("$.content[0].orgId").value(1));
    }



    @Test
    void deletarConsumidor() throws Exception {
        Mockito.doNothing().when(consumidorService).excluir(1L);

        mockMvc.perform(delete("/consumidores/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Consumidor excluído com sucesso"));
    }

    @Test
    void editarConsumidor() throws Exception {
        ConsumidorDtoRequest requestAtualizado = new ConsumidorDtoRequest(
                null,
                "João Atualizado",
                "98765432100",
                "Avenida Central, 200",
                2L
        );

        Mockito.when(consumidorService.editar(1L, requestAtualizado.toEntity()))
                .thenThrow(new ConsumidorNaoEncontradoException("Consumidor não encontrado"));

        mockMvc.perform(
                        put("/consumidores/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestAtualizado))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Consumidor não encontrado"));
    }

    @Test
    void editarConsumidor_WhenNotFound_ReturnsNotFound() throws Exception {
        Mockito.when(consumidorService.editar(anyLong(), any(Consumidor.class)))
                .thenThrow(new ConsumidorNaoEncontradoException("Consumidor não encontrado"));

        mockMvc.perform(
                        put("/consumidores/{id}", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(consumidorDtoRequest))
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Consumidor não encontrado"));
    }
}
