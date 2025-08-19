package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    // DTOs de teste
    private LoginRequestDto requisicaoLoginValida;
    private LoginResponseDto respostaLogin;
    private UsuarioRequestDto requisicaoRegistroValida;
    private UsuarioDto respostaRegistro;

    // Removidos @MockBean desnecessários para simplificar o teste


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
    void login_QuandoCredenciaisValidas_RetornaTokenJwt() {
        when(authService.login(any(LoginRequestDto.class)))
                .thenReturn(respostaLogin);

        ResponseEntity<LoginResponseDto> response = authController.login(requisicaoLoginValida);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals("jwt-token-abc-123", response.getBody().token());
        verify(authService).login(requisicaoLoginValida);
    }

    @Test
    void login_QuandoServicoLancaExcecao_RetornaBadRequest() {
        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new RuntimeException("Usuário não encontrado"));

        ResponseEntity<LoginResponseDto> response = authController.login(requisicaoLoginValida);

        assertEquals(400, response.getStatusCodeValue());
        verify(authService).login(requisicaoLoginValida);
    }

    @Test
    void register_QuandoRequisicaoValida_RetornaUsuarioCriado() {
        when(authService.register(any(UsuarioRequestDto.class)))
                .thenReturn(respostaRegistro);

        ResponseEntity<UsuarioDto> response = authController.register(requisicaoRegistroValida);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody());
        assertEquals(42L, response.getBody().id());
        assertEquals("maria123", response.getBody().username());
        assertEquals("maria@example.com", response.getBody().email());
        assertTrue(response.getBody().ativo());
        assertTrue(response.getBody().roles().contains("ROLE_USER"));
        verify(authService).register(requisicaoRegistroValida);
    }

    @Test
    void register_QuandoServicoLancaExcecao_RetornaBadRequest() {
        when(authService.register(any(UsuarioRequestDto.class)))
                .thenThrow(new RuntimeException("Organização não encontrada"));

        ResponseEntity<UsuarioDto> response = authController.register(requisicaoRegistroValida);

        assertEquals(400, response.getStatusCodeValue());
        verify(authService).register(requisicaoRegistroValida);
    }
}
