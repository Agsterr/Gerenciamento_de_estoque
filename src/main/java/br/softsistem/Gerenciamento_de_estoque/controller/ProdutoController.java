package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public Page<ProdutoDto> listarTodos(@RequestParam Long orgId, Pageable pageable) {
        return service.listarTodos(orgId, pageable).map(ProdutoDto::new);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criarProduto(@RequestBody ProdutoRequest produtoRequest) {
        service.salvar(produtoRequest, produtoRequest.getOrgId());
        return ResponseEntity.ok(Map.of("message", "Produto criado ou atualizado com sucesso!"));
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> excluir(@PathVariable Long id, @RequestParam Long orgId) {
        service.excluir(id, orgId);
        return ResponseEntity.ok(Map.of("message", "Produto excluído com sucesso."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(@PathVariable Long id, @RequestParam Long orgId) {
        Produto produto = service.buscarPorId(id, orgId);
        return ResponseEntity.ok(new ProdutoDto(produto));
    }
}
