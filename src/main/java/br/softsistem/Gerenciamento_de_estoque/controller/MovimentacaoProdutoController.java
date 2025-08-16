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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Edita uma movimentação existente.
     * Exemplo de chamada: PUT /movimentacoes/1
     */
    @PutMapping("/{id}")
    public ResponseEntity<MovimentacaoProdutoDto> editar(
            @PathVariable Long id,
            @RequestBody @Valid MovimentacaoProdutoDto dto) {
        MovimentacaoProdutoDto response = service.editarMovimentacao(id, dto);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todas as movimentações de um produto específico.
     * Exemplo de chamada: GET /movimentacoes/produto/1
     */
    @GetMapping("/produto/{produtoId}")
    public ResponseEntity<List<MovimentacaoProdutoDto>> listarPorProduto(
            @PathVariable Long produtoId) {
        List<MovimentacaoProdutoDto> movimentacoes = service.buscarPorIdProduto(produtoId);
        return ResponseEntity.ok(movimentacoes);
    }

    /**
     * Retorna todas as movimentações de um dia específico, detalhadas.
     * Exemplo de chamada: GET /movimentacoes/por-data?tipo=ENTRADA&data=2025-05-31
     */
    @GetMapping("/por-data")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porData(
            @RequestParam TipoMovimentacao tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorData(tipo, data, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações detalhadas de um intervalo de datas (data/hora).
     * Exemplo: GET /movimentacoes/por-periodo?tipo=SAIDA&inicio=2025-05-01T00:00:00&fim=2025-05-31T23:59:59
     */
    @GetMapping("/por-periodo")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porPeriodo(
            @RequestParam TipoMovimentacao tipo,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorPeriodo(tipo, inicio, fim, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações detalhadas de um ano inteiro.
     * Exemplo: GET /movimentacoes/por-ano?ano=2025
     */
    @GetMapping("/por-ano")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porAno(
            @RequestParam int ano,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.listarDetalhadoPorAno(ano, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações detalhadas de um mês específico (mês + ano).
     * Exemplo: GET /movimentacoes/por-mes?ano=2025&mes=5
     */
    @GetMapping("/por-mes")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porMes(
            @RequestParam int ano,
            @RequestParam int mes,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.listarDetalhadoPorMes(ano, mes, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações de um produto pelo nome.
     * Exemplo de chamada: GET /movimentacoes/por-nome?nomeProduto=produtoX
     */
    @GetMapping("/por-nome")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porNomeProduto(
            @RequestParam String nomeProduto,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorNomeProduto(nomeProduto, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações de um produto por categoria.
     * Exemplo de chamada: GET /movimentacoes/por-categoria?categoriaProduto=categoriaX
     */
    @GetMapping("/por-categoria")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porCategoriaProduto(
            @RequestParam String categoriaProduto,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorCategoriaProduto(categoriaProduto, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações de um produto pelo ID.
     * Exemplo de chamada: GET /movimentacoes/por-id?produtoId=1
     */
    @GetMapping("/por-id")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porIdProduto(
            @RequestParam Long produtoId,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorIdProduto(produtoId, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações de uma organização, por produto, nome ou categoria, e por intervalo de datas.
     * Exemplo de chamada: GET /movimentacoes/por-intervalo?nome=produtoX&categoria=categoriaX&produtoId=1&inicio=2025-05-01T00:00:00&fim=2025-05-31T23:59:59
     */
    @GetMapping("/por-intervalo")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> porIntervalo(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Long produtoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorProdutoNomeCategoriaIdAndIntervalo(nome, categoria, produtoId, inicio, fim, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações do tipo ENTRADA e/ou SAIDA para a organização do usuário logado.
     * Exemplo de chamada: GET /movimentacoes/por-tipos?tipos=ENTRADA,SAIDA
     */
    @GetMapping("/por-tipos")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> buscarPorTipos(
            @RequestParam(name = "tipos") List<TipoMovimentacao> tipos,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorTipos(tipos, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Retorna todas as movimentações relacionadas a um consumidor pelo nome.
     * Exemplo de chamada: GET /movimentacoes/por-consumidor?nome=João Silva
     */
    @GetMapping("/por-consumidor")
    public ResponseEntity<Page<MovimentacaoProdutoDto>> buscarPorConsumidor(
            @RequestParam(name = "nome") String nomeConsumidor,
            Pageable pageable
    ) {
        Page<MovimentacaoProdutoDto> dtoPage = service.buscarPorNomeConsumidor(nomeConsumidor, pageable);
        return ResponseEntity.ok(dtoPage);
    }

    /**
     * Atualiza apenas o consumidor vinculado à movimentação.
     * Exemplo de chamada: PATCH /movimentacoes/1/consumidor?consumidorId=2
     */
    @PatchMapping("/{id}/consumidor")
    public ResponseEntity<MovimentacaoProdutoDto> atualizarConsumidor(
            @PathVariable Long id,
            @RequestParam Long consumidorId) {
        return ResponseEntity.ok(service.atualizarConsumidor(id, consumidorId));
    }
}
