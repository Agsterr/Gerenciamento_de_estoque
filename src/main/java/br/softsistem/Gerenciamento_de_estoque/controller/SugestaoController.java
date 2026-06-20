package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.SugestaoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.SugestaoRequest;
import br.softsistem.Gerenciamento_de_estoque.service.SugestaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sugestoes")
@Tag(name = "Sugestões", description = "Caixa de sugestões por organização")
public class SugestaoController {

    private final SugestaoService sugestaoService;

    public SugestaoController(SugestaoService sugestaoService) {
        this.sugestaoService = sugestaoService;
    }

    @PostMapping
    @Operation(summary = "Enviar sugestão", description = "Qualquer usuário autenticado da organização")
    public ResponseEntity<SugestaoDto> criar(@Valid @RequestBody SugestaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sugestaoService.criar(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Listar sugestões da organização")
    public ResponseEntity<Page<SugestaoDto>> listarOrg(
            @PageableDefault(size = 30, sort = "criadoEm") Pageable pageable) {
        return ResponseEntity.ok(sugestaoService.listarPorOrg(pageable));
    }
}
