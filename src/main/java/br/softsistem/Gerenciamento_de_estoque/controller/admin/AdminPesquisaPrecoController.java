package br.softsistem.Gerenciamento_de_estoque.controller.admin;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.PesquisaPrecoStatsDto;
import br.softsistem.Gerenciamento_de_estoque.service.PesquisaPrecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/pesquisa-preco")
@Tag(name = "Admin - Pesquisa de preço", description = "Estatísticas agregadas (SUPER_ADMIN)")
public class AdminPesquisaPrecoController {

    private final PesquisaPrecoService pesquisaPrecoService;

    public AdminPesquisaPrecoController(PesquisaPrecoService pesquisaPrecoService) {
        this.pesquisaPrecoService = pesquisaPrecoService;
    }

    @GetMapping("/stats")
    @Operation(summary = "Estatísticas e respostas da pesquisa de preço")
    public ResponseEntity<PesquisaPrecoStatsDto> estatisticas() {
        return ResponseEntity.ok(pesquisaPrecoService.estatisticasAdmin());
    }
}
