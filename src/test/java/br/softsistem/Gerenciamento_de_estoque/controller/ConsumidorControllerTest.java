package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.ConsumidorNaoEncontradoException;
import br.softsistem.Gerenciamento_de_estoque.exception.GlobalExceptionHandler;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.service.ConsumidorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ConsumidorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ConsumidorService consumidorService;

    @Mock
    private ConsumidorRepository consumidorRepository;

    private ObjectMapper objectMapper;

    private ConsumidorDtoRequest consumidorDtoRequest;
    private Consumidor consumidor;

    private MockedStatic<SecurityUtils> securityUtilsMockedStatic;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ConsumidorController controller = new ConsumidorController(consumidorService, consumidorRepository);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();

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
        Org org = new Org();
        org.setId(2L);
        consumidor.setOrg(org);

        securityUtilsMockedStatic = Mockito.mockStatic(SecurityUtils.class);
        securityUtilsMockedStatic.when(SecurityUtils::getCurrentOrgId).thenReturn(2L);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMockedStatic.close();
    }

    @Test
    void criarConsumidor() throws Exception {
        Mockito.when(consumidorService.salvar(any(Consumidor.class))).thenAnswer(inv -> {
            Consumidor c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        mockMvc.perform(post("/consumidores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consumidorDtoRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Silva"))
                .andExpect(jsonPath("$.cpf").value("12345678900"))
                .andExpect(jsonPath("$.endereco").value("Rua das Flores, 100"));
    }

    @Test
    void listarTodos() throws Exception {
        Org org = new Org();
        org.setId(2L);
        Consumidor outro = new Consumidor();
        outro.setId(4L);
        outro.setNome("Carlos Alberto azevedo");
        outro.setCpf("12312992399");
        outro.setEndereco(null);
        outro.setOrg(org);

        Page<Consumidor> pageMock = new PageImpl<>(List.of(outro), PageRequest.of(0,10),1);
        Mockito.when(consumidorRepository.findByOrg_Id(eq(2L), any(Pageable.class))).thenReturn(pageMock);

        mockMvc.perform(get("/consumidores")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].id").value(4))
                .andExpect(jsonPath("$.content[0].nome").value("Carlos Alberto azevedo"))
                .andExpect(jsonPath("$.content[0].cpf").value("12312992399"))
                .andExpect(jsonPath("$.content[0].orgId").value(2));
    }

    @Test
    void deletarConsumidor() throws Exception {
        Mockito.doNothing().when(consumidorService).excluir(1L);

        mockMvc.perform(delete("/consumidores/{id}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Consumidor excluído com sucesso"));
    }

    @Test
    void editarConsumidor_sucesso() throws Exception {
        Consumidor existente = new Consumidor();
        existente.setId(1L);
        existente.setNome("Velho");
        existente.setCpf("11111111111");
        existente.setEndereco("Antigo");
        Org org = new Org(); org.setId(2L); existente.setOrg(org);

        ConsumidorDtoRequest requestAtualizado = new ConsumidorDtoRequest(
                null,
                "João Atualizado",
                "98765432100",
                "Avenida Central, 200",
                2L
        );

        Mockito.when(consumidorRepository.findById(1L)).thenReturn(Optional.of(existente));
        Mockito.when(consumidorRepository.save(any(Consumidor.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/consumidores/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestAtualizado)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("João Atualizado"))
                .andExpect(jsonPath("$.cpf").value("98765432100"))
                .andExpect(jsonPath("$.endereco").value("Avenida Central, 200"));
    }

    @Test
    void editarConsumidor_naoEncontrado() throws Exception {
        Mockito.when(consumidorRepository.findById(999L)).thenReturn(Optional.empty());
        ConsumidorDtoRequest req = consumidorDtoRequest;

        mockMvc.perform(put("/consumidores/{id}", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Consumidor não encontrado"));
    }
}
