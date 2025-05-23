package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgRequestDto;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orgs") // Endpoint base para as organizações
public class OrgController {

    private final OrgRepository orgRepository;

    public OrgController(OrgRepository orgRepository) {
        this.orgRepository = orgRepository;
    }

    // --- Endpoint para Criar uma Nova Organização ---
    @PostMapping
    public ResponseEntity<?> createOrg(@Valid @RequestBody OrgRequestDto orgRequestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        // Verifica se já existe uma organização com o mesmo nome (opcional, mas boa prática)
        Optional<Org> existingOrg = orgRepository.findByNome(orgRequestDto.nome());
        if (existingOrg.isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Já existe uma organização com este nome.");
        }

        Org org = new Org(orgRequestDto.nome());
        org = orgRepository.save(org);
        return ResponseEntity.status(HttpStatus.CREATED).body(new OrgDto(org));
    }

    // --- Endpoint para Listar Todas as Organizações ---
    @GetMapping
    public ResponseEntity<List<OrgDto>> getAllOrgs() {
        List<OrgDto> orgs = orgRepository.findAll().stream()
                .map(OrgDto::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(orgs);
    }

    // --- Endpoint para Buscar Organização por ID ---
    @GetMapping("/{id}")
    public ResponseEntity<OrgDto> getOrgById(@PathVariable Long id) {
        return orgRepository.findById(id)
                .map(org -> ResponseEntity.ok(new OrgDto(org)))
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Endpoint para Atualizar Organização ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateOrg(@PathVariable Long id, @Valid @RequestBody OrgRequestDto orgRequestDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getFieldErrors().stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(errors);
        }

        return orgRepository.findById(id)
                .map(existingOrg -> {
                    // Verifica se o novo nome já existe e não é o nome da própria organização
                    if (orgRepository.findByNome(orgRequestDto.nome()).isPresent() &&
                            !existingOrg.getNome().equals(orgRequestDto.nome())) {
                        return ResponseEntity.status(HttpStatus.CONFLICT).body("Já existe outra organização com este nome.");
                    }
                    existingOrg.setNome(orgRequestDto.nome());
                    Org updatedOrg = orgRepository.save(existingOrg);
                    return ResponseEntity.ok(new OrgDto(updatedOrg));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Endpoint para Desativar Organização ---
    @PutMapping("/{id}/desativar")
    public ResponseEntity<Object> desativarOrg(@PathVariable Long id) {
        return orgRepository.findById(id)
                .map(org -> {
                    org.setAtivo(false);
                    orgRepository.save(org);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Endpoint para Ativar Organização ---
    @PutMapping("/{id}/ativar")
    public ResponseEntity<Object> ativarOrg(@PathVariable Long id) {
        return orgRepository.findById(id)
                .map(org -> {
                    org.setAtivo(true);
                    orgRepository.save(org);
                    return ResponseEntity.noContent().build(); // 204 No Content
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
