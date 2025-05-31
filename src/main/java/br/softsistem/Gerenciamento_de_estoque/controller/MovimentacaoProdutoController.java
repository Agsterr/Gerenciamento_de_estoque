package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto.MovimentacaoProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.TipoMovimentacao;
import br.softsistem.Gerenciamento_de_estoque.service.MovimentacaoProdutoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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

    /**
     * Registra uma nova movimentação (entrada ou saída) de produto.
     */
    @PostMapping
    public ResponseEntity<MovimentacaoProdutoDto> registrar(@RequestBody @Valid MovimentacaoProdutoDto dto) {
        MovimentacaoProdutoDto response = service.registrarMovimentacao(dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Retorna todas as movimentações de um dia específico, detalhadas.
     * Exemplo de chamada: GET /movimentacoes/por-data?tipo=ENTRADA&data=2025-05-31
     */
    @GetMapping("/por-data")
    public ResponseEntity<List<MovimentacaoProdutoDto>> porData(
            @RequestParam TipoMovimentacao tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        List<MovimentacaoProdutoDto> dtoList = service.buscarPorData(tipo, data);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Retorna todas as movimentações detalhadas de um intervalo de datas (data/hora).
     * Exemplo: GET /movimentacoes/por-periodo?tipo=SAIDA&inicio=2025-05-01T00:00:00&fim=2025-05-31T23:59:59
     */
    @GetMapping("/por-periodo")
    public ResponseEntity<List<MovimentacaoProdutoDto>> porPeriodo(
            @RequestParam TipoMovimentacao tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim
    ) {
        List<MovimentacaoProdutoDto> dtoList = service.buscarPorPeriodo(tipo, inicio, fim);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Retorna todas as movimentações detalhadas de um ano inteiro.
     * Exemplo: GET /movimentacoes/por-ano?ano=2025
     */
    @GetMapping("/por-ano")
    public ResponseEntity<List<MovimentacaoProdutoDto>> porAno(
            @RequestParam int ano
    ) {
        List<MovimentacaoProdutoDto> dtoList = service.listarDetalhadoPorAno(ano);
        return ResponseEntity.ok(dtoList);
    }

    /**
     * Retorna todas as movimentações detalhadas de um mês específico (mês + ano).
     * Exemplo: GET /movimentacoes/por-mes?ano=2025&mes=5
     */
    @GetMapping("/por-mes")
    public ResponseEntity<List<MovimentacaoProdutoDto>> porMes(
            @RequestParam int ano,
            @RequestParam int mes
    ) {
        List<MovimentacaoProdutoDto> dtoList = service.listarDetalhadoPorMes(ano, mes);
        return ResponseEntity.ok(dtoList);
    }

}
