package br.softsistem.Gerenciamento_de_estoque.controller.admin;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminCreateUserRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminOrgDeviceLimitRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminOrgSummaryDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminUserDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminUserPasswordResponse;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.BypassSubscriptionRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin - Usuários", description = "Criação de usuários e limites de dispositivos (SUPER_ADMIN)")
public class AdminUsersController {

    private final AdminUserService adminUserService;

    public AdminUsersController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "Listar usuários", description = "Lista global ou filtrada por orgId")
    public ResponseEntity<Page<AdminUserDto>> listar(
            @PageableDefault(size = 30, sort = "username") Pageable pageable,
            @RequestParam(required = false) Long orgId) {
        return ResponseEntity.ok(adminUserService.listarUsuarios(orgId, pageable));
    }

    @GetMapping("/orgs")
    @Operation(summary = "Resumo das organizações", description = "Inclui limite de dispositivos e totais")
    public ResponseEntity<List<AdminOrgSummaryDto>> listarOrgs() {
        return ResponseEntity.ok(adminUserService.listarOrgsResumo());
    }

    @PostMapping
    @Operation(summary = "Criar usuário", description = "Senha temporária retornada uma única vez")
    public ResponseEntity<AdminUserPasswordResponse> criar(@Valid @RequestBody AdminCreateUserRequest request) {
        AdminUserPasswordResponse response = adminUserService.criarUsuario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{userId}/ativar")
    public ResponseEntity<AdminUserDto> ativar(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.setAtivo(userId, true));
    }

    @PatchMapping("/{userId}/desativar")
    public ResponseEntity<AdminUserDto> desativar(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.setAtivo(userId, false));
    }

    @PostMapping("/{userId}/reset-password")
    @Operation(summary = "Gerar nova senha temporária")
    public ResponseEntity<AdminUserPasswordResponse> resetSenha(@PathVariable Long userId) {
        return ResponseEntity.ok(adminUserService.resetSenha(userId));
    }

    @PatchMapping("/{userId}/bypass")
    @Operation(summary = "Isentar ou remover isenção de assinatura")
    public ResponseEntity<AdminUserDto> setBypass(
            @PathVariable Long userId,
            @Valid @RequestBody BypassSubscriptionRequest request) {
        return ResponseEntity.ok(adminUserService.setBypass(userId, request.bypass()));
    }

    @PatchMapping("/orgs/{orgId}/max-dispositivos")
    public ResponseEntity<OrgDto> atualizarLimiteDispositivos(
            @PathVariable Long orgId,
            @Valid @RequestBody AdminOrgDeviceLimitRequest request) {
        return ResponseEntity.ok(adminUserService.atualizarLimiteDispositivos(orgId, request));
    }
}
