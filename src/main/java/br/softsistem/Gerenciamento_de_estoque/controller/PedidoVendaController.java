package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoVendaDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.PedidoVendaRequest;
import br.softsistem.Gerenciamento_de_estoque.service.PedidoVendaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pedidos-venda")
public class PedidoVendaController {

    private final PedidoVendaService service;

    public PedidoVendaController(PedidoVendaService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PedidoVendaDto> listar(Pageable pageable) {
        return service.listarDto(pageable);
    }

    @GetMapping("/{id}")
    public PedidoVendaDto buscar(@PathVariable Long id) {
        return service.buscarDto(id);
    }

    @PostMapping
    public ResponseEntity<PedidoVendaDto> criar(@Valid @RequestBody PedidoVendaRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new PedidoVendaDto(service.criar(req)));
    }

    @PostMapping("/{id}/confirmar")
    public PedidoVendaDto confirmar(@PathVariable Long id) {
        return new PedidoVendaDto(service.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    public PedidoVendaDto cancelar(@PathVariable Long id) {
        return new PedidoVendaDto(service.cancelar(id));
    }
}
