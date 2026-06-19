package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.DepositoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.DepositoRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.EstoqueDepositoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.TransferenciaDepositoRequest;
import br.softsistem.Gerenciamento_de_estoque.service.DepositoService;
import br.softsistem.Gerenciamento_de_estoque.service.EstoqueDepositoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/depositos")
public class DepositoController {

    private final DepositoService depositoService;
    private final EstoqueDepositoService estoqueService;

    public DepositoController(DepositoService depositoService, EstoqueDepositoService estoqueService) {
        this.depositoService = depositoService;
        this.estoqueService = estoqueService;
    }

    @GetMapping
    public List<DepositoDto> listar() {
        return depositoService.listar().stream().map(DepositoDto::new).toList();
    }

    @GetMapping("/{id}")
    public DepositoDto buscar(@PathVariable Long id) {
        return new DepositoDto(depositoService.buscarPorId(id));
    }

    @GetMapping("/{id}/estoque")
    public List<EstoqueDepositoDto> estoque(@PathVariable Long id) {
        return estoqueService.listarPorDeposito(id).stream().map(EstoqueDepositoDto::new).toList();
    }

    @PostMapping
    public ResponseEntity<DepositoDto> criar(@Valid @RequestBody DepositoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new DepositoDto(depositoService.criar(req)));
    }

    @PutMapping("/{id}")
    public DepositoDto atualizar(@PathVariable Long id, @Valid @RequestBody DepositoRequest req) {
        return new DepositoDto(depositoService.atualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> excluir(@PathVariable Long id) {
        depositoService.excluir(id);
        return Map.of("message", "Depósito desativado");
    }

    @PostMapping("/transferir")
    public Map<String, String> transferir(@Valid @RequestBody TransferenciaDepositoRequest req) {
        estoqueService.transferir(req.depositoOrigemId(), req.depositoDestinoId(), req.produtoId(), req.quantidade());
        return Map.of("message", "Transferência realizada com sucesso");
    }
}
