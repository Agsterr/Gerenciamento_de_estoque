package br.softsistem.Gerenciamento_de_estoque.controller.admin;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AcessoLoginDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.DispositivoUsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.service.DispositivoUsuarioService;
import br.softsistem.Gerenciamento_de_estoque.service.LoginAuditoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin - Acessos", description = "Auditoria de logins e dispositivos (SUPER_ADMIN)")
public class AdminAcessosController {

    private final LoginAuditoriaService loginAuditoriaService;
    private final DispositivoUsuarioService dispositivoUsuarioService;

    public AdminAcessosController(LoginAuditoriaService loginAuditoriaService,
                                  DispositivoUsuarioService dispositivoUsuarioService) {
        this.loginAuditoriaService = loginAuditoriaService;
        this.dispositivoUsuarioService = dispositivoUsuarioService;
    }

    @GetMapping("/login-logs")
    @Operation(summary = "Listar logs de login", description = "Histórico paginado de acessos ao sistema")
    public ResponseEntity<Page<AcessoLoginDto>> listarLoginLogs(
            @PageableDefault(size = 30, sort = "dataHora") Pageable pageable) {
        return ResponseEntity.ok(loginAuditoriaService.listar(pageable));
    }

    @GetMapping("/dispositivos/pendentes")
    @Operation(summary = "Listar dispositivos pendentes", description = "Scaffold Fase 2 — dispositivos aguardando aprovação")
    public ResponseEntity<Page<DispositivoUsuarioDto>> listarDispositivosPendentes(
            @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(dispositivoUsuarioService.listarPendentes(pageable));
    }
}
