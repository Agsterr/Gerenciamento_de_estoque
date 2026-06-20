package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.PesquisaPrecoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PesquisaPrecoRequest;
import br.softsistem.Gerenciamento_de_estoque.service.PesquisaPrecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pesquisa-preco")
@Tag(name = "Pesquisa de preço", description = "Disposição a pagar mensalmente pelo sistema")
public class PesquisaPrecoController {

    private final PesquisaPrecoService pesquisaPrecoService;

    public PesquisaPrecoController(PesquisaPrecoService pesquisaPrecoService) {
        this.pesquisaPrecoService = pesquisaPrecoService;
    }

    @PostMapping
    @Operation(summary = "Enviar ou atualizar minha faixa de preço mensal")
    public ResponseEntity<PesquisaPrecoDto> enviar(@Valid @RequestBody PesquisaPrecoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(pesquisaPrecoService.enviar(request));
    }

    @GetMapping("/minha")
    @Operation(summary = "Consultar minha resposta, se existir")
    public ResponseEntity<PesquisaPrecoDto> minhaResposta() {
        PesquisaPrecoDto dto = pesquisaPrecoService.minhaResposta();
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }
}
