package br.softsistem.Gerenciamento_de_estoque.controller;


import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.model.MovimentacaoProduto;
import br.softsistem.Gerenciamento_de_estoque.service.MovimentacaoProdutoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/movimentacoes")
public class MovimentacaoProdutoController {

    private final MovimentacaoProdutoService service;

    public MovimentacaoProdutoController(MovimentacaoProdutoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MovimentacaoProduto> registrar(@RequestBody @Valid MovimentacaoProdutoDto dto) {
        MovimentacaoProduto response = service.registrarMovimentacao(dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/por-data")
    public ResponseEntity<List<MovimentacaoProdutoDto>> porData(@RequestParam TipoMovimentacao tipo,
                                                                 @RequestParam LocalDate data) {
        return ResponseEntity.ok(service.buscarPorData(tipo, data));
    }

    @GetMapping("/por-periodo")
    public ResponseEntity<List<MovimentacaoProdutoDto>> porPeriodo(@RequestParam TipoMovimentacao tipo,
                                                                    @RequestParam LocalDateTime inicio,
                                                                    @RequestParam LocalDateTime fim) {
        return ResponseEntity.ok(service.buscarPorPeriodo(tipo, inicio, fim));
    }

    @GetMapping("/total/ano")
    public ResponseEntity<Integer> totalAno(@RequestParam TipoMovimentacao tipo, @RequestParam int ano) {
        return ResponseEntity.ok(service.totalPorAno(tipo, ano));
    }

    @GetMapping("/total/mes")
    public ResponseEntity<Integer> totalMes(@RequestParam TipoMovimentacao tipo,
                                            @RequestParam int ano,
                                            @RequestParam int mes) {
        return ResponseEntity.ok(service.totalPorMes(tipo, ano, mes));
    }
}
