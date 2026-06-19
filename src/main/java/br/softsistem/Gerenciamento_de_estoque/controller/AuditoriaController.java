package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.AuditoriaLogDto;
import br.softsistem.Gerenciamento_de_estoque.service.AuditoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auditoria")
public class AuditoriaController {

    private final AuditoriaService service;

    public AuditoriaController(AuditoriaService service) { this.service = service; }

    @GetMapping
    public Page<AuditoriaLogDto> listar(Pageable pageable,
                                        @RequestParam(required = false) String entidade) {
        if (entidade != null && !entidade.isBlank()) {
            return service.listarPorEntidade(entidade, pageable).map(AuditoriaLogDto::new);
        }
        return service.listar(pageable).map(AuditoriaLogDto::new);
    }
}
