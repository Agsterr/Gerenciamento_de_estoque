package br.softsistem.Gerenciamento_de_estoque.controller;


import br.softsistem.Gerenciamento_de_estoque.config.JwtAuthenticationFilter;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import br.softsistem.Gerenciamento_de_estoque.service.AuthService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class, // Corrigido para o controlador correto
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    // DTOs de teste
    private LoginRequestDto requisicaoLoginValida;
    private LoginResponseDto respostaLogin;
    private UsuarioRequestDto requisicaoRegistroValida;
    private UsuarioDto respostaRegistro;

    @MockBean
    private OrgRepository orgRepository;

    @MockBean
    private RoleRepository roleRepository;

    @MockBean
    UsuarioRepository usuarioRepository;

    @MockBean
    private CategoriaRepository categoriaRepository;

    @MockBean
    private ConsumidorRepository consumidorRepository;


    @MockBean
    private EntregaRepository entregaRepository;

    @MockBean
    private ProdutoRepository produtoRepository;

    @MockBean
    private MovimentacaoProdutoRepository  movimentacaoProdutoRepository;


    @BeforeEach
    void configurar() {
        // Inicialização dos DTOs de login e registro
        requisicaoLoginValida = new LoginRequestDto("joao123", "senhaSegura", 2L);
        respostaLogin = new LoginResponseDto("jwt-token-abc-123");

        requisicaoRegistroValida = new UsuarioRequestDto(
                "maria123",
                "senhaForte",
                "maria@example.com",
                List.of("ROLE_USER"),
                2L
        );

        respostaRegistro = new UsuarioDto(
                42L,
                "maria123",
                "maria@example.com",
                true,
                List.of("ROLE_USER")
        );
    }

    @Test
    void login_QuandoCredenciaisValidas_RetornaTokenJwt() throws Exception {
        Mockito.when(authService.login(ArgumentMatchers.any(LoginRequestDto.class)))
                .thenReturn(respostaLogin);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(requisicaoLoginValida)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", is("jwt-token-abc-123")));

        Mockito.verify(authService).login(ArgumentMatchers.refEq(requisicaoLoginValida));
    }

    @Test
    void login_QuandoServicoLancaExcecao_RetornaBadRequest() throws Exception {
        Mockito.when(authService.login(ArgumentMatchers.any(LoginRequestDto.class)))
                .thenThrow(new RuntimeException("Usuário não encontrado"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(requisicaoLoginValida)))
                .andExpect(status().isBadRequest());

        Mockito.verify(authService).login(ArgumentMatchers.refEq(requisicaoLoginValida));
    }

    @Test
    void register_QuandoRequisicaoValida_RetornaUsuarioCriado() throws Exception {
        Mockito.when(authService.register(ArgumentMatchers.any(UsuarioRequestDto.class)))
                .thenReturn(respostaRegistro);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(requisicaoRegistroValida)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(42)))
                .andExpect(jsonPath("$.username", is("maria123")))
                .andExpect(jsonPath("$.email", is("maria@example.com")))
                .andExpect(jsonPath("$.ativo", is(true)))
                .andExpect(jsonPath("$.roles", contains("ROLE_USER")));

        Mockito.verify(authService).register(ArgumentMatchers.refEq(requisicaoRegistroValida));
    }

    @Test
    void register_QuandoServicoLancaExcecao_RetornaBadRequest() throws Exception {
        Mockito.when(authService.register(ArgumentMatchers.any(UsuarioRequestDto.class)))
                .thenThrow(new RuntimeException("Organização não encontrada"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(requisicaoRegistroValida)))
                .andExpect(status().isBadRequest());

        Mockito.verify(authService).register(ArgumentMatchers.refEq(requisicaoRegistroValida));
    }
}
