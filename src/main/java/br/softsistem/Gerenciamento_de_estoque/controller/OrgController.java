package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgRequestDto;
import br.softsistem.Gerenciamento_de_estoque.service.OrgService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(value = "/api/orgs", produces = MediaType.APPLICATION_JSON_VALUE)
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
            Map<String,String> body = Map.of("error", "Já existe uma organização com este nome.");
            return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(orgDto.get());
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
            Map<String,String> body = Map.of("error", "Já existe outra organização com este nome.");
            return ResponseEntity.status(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_JSON).body(body);
        }
        return updatedOrg.<ResponseEntity<?>>map(o -> ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(o))
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
}
