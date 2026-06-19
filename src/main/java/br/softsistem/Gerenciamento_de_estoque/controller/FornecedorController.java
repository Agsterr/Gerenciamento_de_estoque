package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.erp.*;
import br.softsistem.Gerenciamento_de_estoque.service.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fornecedores")
public class FornecedorController {

    private final FornecedorService service;

    public FornecedorController(FornecedorService service) { this.service = service; }

    @GetMapping
    public Page<FornecedorDto> listar(Pageable pageable) {
        return service.listar(pageable).map(FornecedorDto::new);
    }

    @GetMapping("/{id}")
    public FornecedorDto buscar(@PathVariable Long id) {
        return new FornecedorDto(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<FornecedorDto> criar(@Valid @RequestBody FornecedorRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new FornecedorDto(service.criar(req)));
    }

    @PutMapping("/{id}")
    public FornecedorDto atualizar(@PathVariable Long id, @Valid @RequestBody FornecedorRequest req) {
        return new FornecedorDto(service.atualizar(id, req));
    }

    @DeleteMapping("/{id}")
    public Map<String, String> excluir(@PathVariable Long id) {
        service.excluir(id);
        return Map.of("message", "Fornecedor desativado");
    }
}
