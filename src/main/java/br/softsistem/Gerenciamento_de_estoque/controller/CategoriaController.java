package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaResponse;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.service.CategoriaService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    // Listar todas as categorias de uma organização com paginação
    @GetMapping("/org/{orgId}")
    public ResponseEntity<Page<CategoriaResponse>> listarTodos(@PathVariable Long orgId, Pageable pageable) {
        Page<CategoriaResponse> categorias = service.listarTodos(orgId, pageable)
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()));
        return ResponseEntity.ok(categorias);
    }

    // Buscar uma categoria pelo nome e orgId
    @GetMapping("/org/{orgId}/nome/{nome}")
    public ResponseEntity<CategoriaResponse> buscarPorNome(@PathVariable Long orgId, @PathVariable String nome) {
        Optional<Categoria> categoria = service.buscarPorNomeEOrgId(nome, orgId);
        return categoria
                .map(value -> ResponseEntity.ok(new CategoriaResponse(value.getId(), value.getNome())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // Buscar categorias por parte do nome com paginação
    @GetMapping("/org/{orgId}/parte-do-nome/{parteDoNome}")
    public ResponseEntity<Page<CategoriaResponse>> buscarPorParteDoNome(@PathVariable Long orgId, @PathVariable String parteDoNome, Pageable pageable) {
        Page<CategoriaResponse> categorias = service.buscarPorParteDoNomeEOrgId(parteDoNome, orgId, pageable)
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()));
        return ResponseEntity.ok(categorias);
    }

    // Salvar nova categoria associada à organização
    @PostMapping("/org/{orgId}")
    public ResponseEntity<CategoriaResponse> salvar(@PathVariable Long orgId, @Valid @RequestBody CategoriaRequest request) {
        Categoria categoriaSalva = service.salvarCategoria(request, orgId);
        CategoriaResponse response = new CategoriaResponse(categoriaSalva.getId(), categoriaSalva.getNome());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Editar uma categoria existente
    @PutMapping("/org/{orgId}/{id}")
    public ResponseEntity<CategoriaResponse> editar(@PathVariable Long orgId, @PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        Categoria categoriaEditada = service.editarCategoria(id, request, orgId);
        if (categoriaEditada != null) {
            CategoriaResponse response = new CategoriaResponse(categoriaEditada.getId(), categoriaEditada.getNome());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    // Excluir uma categoria existente
    @DeleteMapping("/org/{orgId}/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long orgId, @PathVariable Long id) {
        boolean excluida = service.excluirCategoria(id, orgId);
        return excluida ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
