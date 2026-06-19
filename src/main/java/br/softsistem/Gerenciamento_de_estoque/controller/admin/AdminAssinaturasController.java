package br.softsistem.Gerenciamento_de_estoque.controller.admin;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminOrgSubscriptionDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminUserSubscriptionDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.BypassSubscriptionRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.ExtendTrialRequest;
import br.softsistem.Gerenciamento_de_estoque.service.AdminSubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/subscriptions")
@Tag(name = "Admin - Assinaturas", description = "Gestão de trial, bypass e cobrança (SUPER_ADMIN)")
public class AdminAssinaturasController {

    private final AdminSubscriptionService adminSubscriptionService;

    public AdminAssinaturasController(AdminSubscriptionService adminSubscriptionService) {
        this.adminSubscriptionService = adminSubscriptionService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Visão geral por organização", description = "Lista orgs, usuários, trial e bypass")
    public ResponseEntity<List<AdminOrgSubscriptionDto>> overview() {
        return ResponseEntity.ok(adminSubscriptionService.listOverview());
    }

    @PatchMapping("/users/{userId}/bypass")
    @Operation(summary = "Alternar isenção de cobrança (não cobrar)")
    public ResponseEntity<AdminUserSubscriptionDto> setBypass(
            @PathVariable Long userId,
            @Valid @RequestBody BypassSubscriptionRequest request) {
        return ResponseEntity.ok(adminSubscriptionService.setBypass(userId, request.bypass()));
    }

    @PatchMapping("/users/{userId}/extend-trial")
    @Operation(summary = "Estender trial de um usuário")
    public ResponseEntity<AdminUserSubscriptionDto> extendTrialUser(
            @PathVariable Long userId,
            @RequestBody ExtendTrialRequest request) {
        return ResponseEntity.ok(adminSubscriptionService.extendTrial(userId, request));
    }

    @PatchMapping("/orgs/{orgId}/extend-trial")
    @Operation(summary = "Estender trial de todos os usuários da org (exceto bypass)")
    public ResponseEntity<List<AdminUserSubscriptionDto>> extendTrialOrg(
            @PathVariable Long orgId,
            @RequestBody ExtendTrialRequest request) {
        return ResponseEntity.ok(adminSubscriptionService.extendTrialForOrg(orgId, request));
    }

    @PatchMapping("/users/{userId}/force-pay")
    @Operation(summary = "Forçar cobrança: remove bypass e encerra trial")
    public ResponseEntity<AdminUserSubscriptionDto> forcePayUser(@PathVariable Long userId) {
        return ResponseEntity.ok(adminSubscriptionService.forcePay(userId));
    }

    @PatchMapping("/orgs/{orgId}/force-pay")
    @Operation(summary = "Forçar cobrança para todos os usuários da org")
    public ResponseEntity<List<AdminUserSubscriptionDto>> forcePayOrg(@PathVariable Long orgId) {
        return ResponseEntity.ok(adminSubscriptionService.forcePayForOrg(orgId));
    }
}
