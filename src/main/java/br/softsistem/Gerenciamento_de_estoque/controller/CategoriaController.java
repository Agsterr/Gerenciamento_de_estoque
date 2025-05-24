package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaResponse;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.service.CategoriaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    // Constructor Injection
    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    // Listar todas as categorias de uma organização com paginação
    @GetMapping("/{orgId}")
    public ResponseEntity<Page<CategoriaResponse>> listarTodos(@PathVariable Long orgId, Pageable pageable) {
        Page<CategoriaResponse> categorias = service.listarTodos(orgId, pageable)
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()));
        return ResponseEntity.ok(categorias);
    }

    // Buscar uma categoria pelo nome e orgId
    @GetMapping("/{orgId}/nome/{nome}")
    public ResponseEntity<CategoriaResponse> buscarPorNome(@PathVariable Long orgId, @PathVariable String nome) {
        Optional<Categoria> categoria = service.buscarPorNomeEOrgId(nome, orgId);
        if (categoria.isPresent()) {
            CategoriaResponse response = new CategoriaResponse(categoria.get().getId(), categoria.get().getNome());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();  // Retorna 404 se não encontrar
    }

    // Buscar categorias cujo nome contenha uma parte do nome com paginação
    @GetMapping("/{orgId}/parteDoNome/{parteDoNome}")
    public ResponseEntity<Page<CategoriaResponse>> buscarPorParteDoNome(@PathVariable Long orgId, @PathVariable String parteDoNome, Pageable pageable) {
        Page<CategoriaResponse> categorias = service.buscarPorParteDoNomeEOrgId(parteDoNome, orgId, pageable)
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()));
        return ResponseEntity.ok(categorias);
    }

    // Salvar nova categoria associada à organização
    @PostMapping("/{orgId}")
    public ResponseEntity<CategoriaResponse> salvar(@PathVariable Long orgId, @Valid @RequestBody CategoriaRequest request) {
        Categoria categoriaSalva = service.salvarCategoria(request, orgId);
        CategoriaResponse response = new CategoriaResponse(categoriaSalva.getId(), categoriaSalva.getNome());
        return new ResponseEntity<>(response, HttpStatus.CREATED); // Retorna com código 201 (Criado)
    }

    // Editar uma categoria existente
    @PutMapping("/{orgId}/{id}")
    public ResponseEntity<CategoriaResponse> editar(@PathVariable Long orgId, @PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        Categoria categoriaEditada = service.editarCategoria(id, request, orgId);
        if (categoriaEditada != null) {
            CategoriaResponse response = new CategoriaResponse(categoriaEditada.getId(), categoriaEditada.getNome());
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); // Retorna 404 caso não encontre
    }

    // Excluir uma categoria existente
    @DeleteMapping("/{orgId}/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long orgId, @PathVariable Long id) {
        boolean excluida = service.excluirCategoria(id, orgId);
        if (excluida) {
            return ResponseEntity.noContent().build();  // Retorna 204 No Content
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();  // Retorna 404 caso não encontre
    }
}
