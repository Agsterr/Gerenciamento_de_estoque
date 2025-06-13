package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    @Autowired
    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoDto>> listarTodos(Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Page<ProdutoDto> page = service.listarTodos(orgId, pageable).map(ProdutoDto::new);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criarProduto(@RequestBody ProdutoRequest produtoRequest) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.salvar(produtoRequest, orgId);
        return ResponseEntity.ok(Map.of("message", "Produto criado ou atualizado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> excluir(@PathVariable Long id) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        service.excluir(id, orgId);
        return ResponseEntity.ok(Map.of("message", "Produto excluído com sucesso."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(@PathVariable Long id) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Produto produto = service.buscarPorId(id, orgId);
        return ResponseEntity.ok(new ProdutoDto(produto));
    }

    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<ProdutoDto>> listarProdutosComEstoqueBaixo() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<ProdutoDto> produtos = service.listarProdutosComEstoqueBaixo(orgId).stream()
                .map(ProdutoDto::new)
                .toList();
        return ResponseEntity.ok(produtos);
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> editarProduto(@PathVariable Long id, @RequestBody ProdutoRequest produtoRequest) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Produto produtoEditado = service.editar(id, produtoRequest, orgId);
        return ResponseEntity.ok(new ProdutoDto(produtoEditado));
    }


}
