package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.CategoriaDto.CategoriaResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.service.CategoriaService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return orgId;
    }

    @GetMapping
    public ResponseEntity<Page<CategoriaResponse>> listarTodos(Pageable pageable) {
        Long orgId = requireOrgId();
        Page<CategoriaResponse> categorias = service.listarTodos(orgId, pageable)
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()));
        return ResponseEntity.ok(categorias);
    }

    @GetMapping("/nome/{nome}")
    public ResponseEntity<CategoriaResponse> buscarPorNome(@PathVariable String nome) {
        Long orgId = requireOrgId();
        return service.buscarPorNomeEOrgId(nome, orgId)
                .map(value -> ResponseEntity.ok(new CategoriaResponse(value.getId(), value.getNome())))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/parte-do-nome/{parteDoNome}")
    public ResponseEntity<Page<CategoriaResponse>> buscarPorParteDoNome(@PathVariable String parteDoNome, Pageable pageable) {
        Long orgId = requireOrgId();
        Page<CategoriaResponse> categorias = service.buscarPorParteDoNomeEOrgId(parteDoNome, orgId, pageable)
                .map(categoria -> new CategoriaResponse(categoria.getId(), categoria.getNome()));
        return ResponseEntity.ok(categorias);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> salvar(@Valid @RequestBody CategoriaRequest request) {
        Long orgId = requireOrgId();
        Categoria categoriaSalva = service.salvarCategoria(request, orgId);
        CategoriaResponse response = new CategoriaResponse(categoriaSalva.getId(), categoriaSalva.getNome());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoriaResponse> editar(@PathVariable Long id, @Valid @RequestBody CategoriaRequest request) {
        Long orgId = requireOrgId();
        Categoria categoriaEditada = service.editarCategoria(id, request, orgId);
        CategoriaResponse response = new CategoriaResponse(categoriaEditada.getId(), categoriaEditada.getNome());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        Long orgId = requireOrgId();
        service.excluirCategoria(id, orgId);
        return ResponseEntity.noContent().build();
    }
}
