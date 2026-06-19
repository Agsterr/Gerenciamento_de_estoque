package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
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
    public ResponseEntity<Page<ProdutoDto>> listarTodos(Pageable pageable) {
        Long orgId = requireOrgId();
        Page<ProdutoDto> page = service.listarTodos(orgId, pageable).map(ProdutoDto::new);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criarProduto(@Valid @RequestBody ProdutoRequest produtoRequest) {
        Long orgId = requireOrgId();
        service.salvar(produtoRequest, orgId);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Produto criado ou atualizado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> excluir(@PathVariable Long id) {
        Long orgId = requireOrgId();
        service.excluir(id, orgId);
        return ResponseEntity.ok(Map.of("message", "Produto excluído com sucesso."));
    }

    @GetMapping("/codigo-barras/{codigo}")
    public ResponseEntity<ProdutoDto> buscarPorCodigoBarras(@PathVariable String codigo) {
        Long orgId = requireOrgId();
        Produto produto = service.buscarPorCodigoBarras(codigo, orgId);
        return ResponseEntity.ok(new ProdutoDto(produto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(@PathVariable Long id) {
        Long orgId = requireOrgId();
        Produto produto = service.buscarPorId(id, orgId);
        return ResponseEntity.ok(new ProdutoDto(produto));
    }

    @GetMapping("/estoque-baixo")
    public ResponseEntity<List<ProdutoDto>> listarProdutosComEstoqueBaixo() {
        Long orgId = requireOrgId();
        List<ProdutoDto> produtos = service.listarProdutosComEstoqueBaixo(orgId).stream()
                .map(ProdutoDto::new)
                .toList();
        return ResponseEntity.ok(produtos);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDto> editarProduto(@PathVariable Long id, @Valid @RequestBody ProdutoRequest produtoRequest) {
        Long orgId = requireOrgId();
        Produto produtoEditado = service.editar(id, produtoRequest, orgId);
        return ResponseEntity.ok(new ProdutoDto(produtoEditado));
    }
}
