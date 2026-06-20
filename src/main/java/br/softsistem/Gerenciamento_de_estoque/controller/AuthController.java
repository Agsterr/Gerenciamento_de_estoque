package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.DemoProperties;
import br.softsistem.Gerenciamento_de_estoque.config.RegistrationProperties;
import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.auth.AuthPublicConfigDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.exception.LimiteDispositivosException;
import br.softsistem.Gerenciamento_de_estoque.exception.UsuarioDesativadoException;
import br.softsistem.Gerenciamento_de_estoque.service.AuthService;
import br.softsistem.Gerenciamento_de_estoque.service.LoginAuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

/**
 * AuthController: agora apenas orquestra as chamadas ao AuthService,
 * mantendo o controller enxuto.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final LoginAuditoriaService loginAuditoriaService;
    private final RegistrationProperties registrationProperties;
    private final DemoProperties demoProperties;

    public AuthController(AuthService authService,
                          LoginAuditoriaService loginAuditoriaService,
                          RegistrationProperties registrationProperties,
                          DemoProperties demoProperties) {
        this.authService = authService;
        this.loginAuditoriaService = loginAuditoriaService;
        this.registrationProperties = registrationProperties;
        this.demoProperties = demoProperties;
    }

    @GetMapping("/config")
    public ResponseEntity<AuthPublicConfigDto> publicConfig() {
        return ResponseEntity.ok(new AuthPublicConfigDto(
                registrationProperties.enabled(),
                demoProperties.enabled(),
                demoProperties.username()
        ));
    }

    /**
     * Recebe JSON com { username, senha, orgId }, repassa para o AuthService e retorna o token.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest
    ) {
        try {
            LoginResponseDto response = authService.login(request, httpRequest);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException ex) {
            loginAuditoriaService.registrarFalha(request.username(), httpRequest);
            throw ex;
        } catch (UsuarioDesativadoException ex) {
            loginAuditoriaService.registrarFalha(request.username(), httpRequest, ex.getMessage());
            throw ex;
        } catch (LimiteDispositivosException ex) {
            loginAuditoriaService.registrarFalha(request.username(), httpRequest, ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout(SecurityUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Recebe JSON com { username, senha, email, roles, orgId },
     * repassa para o AuthService e retorna o DTO do usuário criado.
     */
    @PostMapping("/register")
    public ResponseEntity<UsuarioDto> register(
            @RequestBody @Valid UsuarioRequestDto usuarioRequestDto
    ) {
        UsuarioDto criado = authService.register(usuarioRequestDto);
        return ResponseEntity.status(201).body(criado);
    }
}
