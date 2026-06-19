package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoCompraDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoCompraRequest;
import br.softsistem.Gerenciamento_de_estoque.service.PedidoCompraService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/pedidos-compra")
public class PedidoCompraController {

    private final PedidoCompraService service;

    public PedidoCompraController(PedidoCompraService service) { this.service = service; }

    @GetMapping
    public Page<PedidoCompraDto> listar(Pageable pageable) {
        return service.listar(pageable).map(PedidoCompraDto::new);
    }

    @GetMapping("/{id}")
    public PedidoCompraDto buscar(@PathVariable Long id) {
        return new PedidoCompraDto(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PedidoCompraDto> criar(@Valid @RequestBody PedidoCompraRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new PedidoCompraDto(service.criar(req)));
    }

    @PostMapping("/{id}/receber")
    public PedidoCompraDto receber(@PathVariable Long id) {
        return new PedidoCompraDto(service.receber(id));
    }

    @PostMapping("/{id}/cancelar")
    public PedidoCompraDto cancelar(@PathVariable Long id) {
        return new PedidoCompraDto(service.cancelar(id));
    }
}
