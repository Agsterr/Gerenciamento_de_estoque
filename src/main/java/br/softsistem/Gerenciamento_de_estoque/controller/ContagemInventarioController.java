package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.ContagemInventarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.ContagemInventarioRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.erp.ContagemItemUpdateRequest;
import br.softsistem.Gerenciamento_de_estoque.service.ContagemInventarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/inventario/contagens")
public class ContagemInventarioController {

    private final ContagemInventarioService service;

    public ContagemInventarioController(ContagemInventarioService service) { this.service = service; }

    @GetMapping
    public Page<ContagemInventarioDto> listar(Pageable pageable) {
        return service.listarDto(pageable);
    }

    @GetMapping("/{id}")
    public ContagemInventarioDto buscar(@PathVariable Long id) {
        return service.buscarDto(id);
    }

    @PostMapping
    public ResponseEntity<ContagemInventarioDto> iniciar(@Valid @RequestBody ContagemInventarioRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new ContagemInventarioDto(service.iniciar(req)));
    }

    @PutMapping("/{id}/itens")
    public ContagemInventarioDto registrarItens(@PathVariable Long id,
                                                @RequestBody List<ContagemItemUpdateRequest> updates) {
        return new ContagemInventarioDto(service.registrarContagem(id, updates));
    }

    @PostMapping("/{id}/finalizar")
    public ContagemInventarioDto finalizar(@PathVariable Long id) {
        return new ContagemInventarioDto(service.finalizar(id));
    }

    @PostMapping("/{id}/cancelar")
    public ContagemInventarioDto cancelar(@PathVariable Long id) {
        return new ContagemInventarioDto(service.cancelar(id));
    }
}
