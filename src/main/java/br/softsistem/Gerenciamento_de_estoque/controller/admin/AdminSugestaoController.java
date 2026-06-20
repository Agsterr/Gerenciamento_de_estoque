package br.softsistem.Gerenciamento_de_estoque.controller.admin;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.SugestaoDto;
import br.softsistem.Gerenciamento_de_estoque.service.SugestaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/sugestoes")
@Tag(name = "Admin - Sugestões", description = "Todas as sugestões de todos os tenants (SUPER_ADMIN)")
public class AdminSugestaoController {

    private final SugestaoService sugestaoService;

    public AdminSugestaoController(SugestaoService sugestaoService) {
        this.sugestaoService = sugestaoService;
    }

    @GetMapping
    @Operation(summary = "Listar todas as sugestões")
    public ResponseEntity<Page<SugestaoDto>> listar(
            @PageableDefault(size = 30, sort = "criadoEm") Pageable pageable) {
        return ResponseEntity.ok(sugestaoService.listarGlobal(pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da sugestão")
    public ResponseEntity<SugestaoDto> atualizarStatus(
            @PathVariable Long id,
            @RequestBody StatusRequest request) {
        return ResponseEntity.ok(sugestaoService.atualizarStatus(id, request.status()));
    }

    public record StatusRequest(String status) {}
}
