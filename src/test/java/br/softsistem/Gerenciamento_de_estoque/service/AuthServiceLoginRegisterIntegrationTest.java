package br.softsistem.Gerenciamento_de_estoque.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceLoginRegisterIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registerThenLogin_comSenhaInformada_deveAutenticar() {
        String username = "user_test_" + System.currentTimeMillis();
        String email = username + "@test.local";
        String senha = "senha12345";
        String orgNome = "Org Teste " + System.currentTimeMillis();

        authService.register(new UsuarioRequestDto(username, senha, email, null, null, orgNome));

        var saved = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertTrue(saved.getSenha().startsWith("$2"), "Senha deve estar em BCrypt no banco");
        assertTrue(passwordEncoder.matches(senha, saved.getSenha()));

        assertDoesNotThrow(() -> authService.login(new LoginRequestDto(username, senha), null));
        assertDoesNotThrow(() -> authService.login(new LoginRequestDto(email, senha), null));
    }

    @Test
    void login_comSenhaErrada_deveFalhar() {
        String username = "user_fail_" + System.currentTimeMillis();
        String email = username + "@test.local";
        String senha = "senha12345";
        String orgNome = "Org Fail " + System.currentTimeMillis();

        authService.register(new UsuarioRequestDto(username, senha, email, null, null, orgNome));

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequestDto(username, "senhaErrada"), null));
    }
}
