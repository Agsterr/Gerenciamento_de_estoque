package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgRequestDto;
import br.softsistem.Gerenciamento_de_estoque.service.OrgService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orgs")
public class OrgController {

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    // --- Criar nova organização ---
    @PostMapping
    public ResponseEntity<?> createOrg(@Valid @RequestBody OrgRequestDto orgRequestDto) {
        Optional<OrgDto> orgDto = orgService.createOrg(orgRequestDto);
        if (orgDto.isEmpty()) {
            // Caso de conflito (nome já existe)
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("Já existe uma organização com este nome."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(orgDto.get());
    }

    // --- Listar todas as organizações ---
    @GetMapping
    public ResponseEntity<List<OrgDto>> getAllOrgs() {
        return ResponseEntity.ok(orgService.getAllOrgs());
    }

    // --- Buscar por ID ---
    @GetMapping("/{id}")
    public ResponseEntity<OrgDto> getOrgById(@PathVariable Long id) {
        return orgService.getOrgById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Atualizar organização ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrg(@PathVariable Long id, @Valid @RequestBody OrgRequestDto orgRequestDto) {
        Optional<OrgDto> updatedOrg = orgService.updateOrg(id, orgRequestDto);
        if (updatedOrg == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("Já existe outra organização com este nome."));
        }
        return updatedOrg.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Desativar organização ---
    @PutMapping("/{id}/desativar")
    public ResponseEntity<Object> desativarOrg(@PathVariable Long id) {
        boolean result = orgService.desativarOrg(id);
        if (result) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // --- Ativar organização ---
    @PutMapping("/{id}/ativar")
    public ResponseEntity<Object> ativarOrg(@PathVariable Long id) {
        boolean result = orgService.ativarOrg(id);
        if (result) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Classe utilitária para mensagens de erro (API padrão)
    public record ApiError(String error) {}
}
