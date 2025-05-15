package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;
    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    // Constructor Injection
    public ProdutoController(ProdutoService service, ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.service = service;
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // Listar todos os produtos ativos de uma organização
    @GetMapping
    public Page<ProdutoDto> listarTodos(@RequestParam Long orgId, Pageable pageable) {
        return service.listarTodos(orgId, pageable).map(ProdutoDto::new);
    }

    // Criar um novo produto
    @PostMapping
    public ResponseEntity<Map<String, String>> criarProduto(@RequestParam Long orgId, @RequestBody @Valid ProdutoRequest produtoRequest) {
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));

        Produto produto = new Produto();
        produto.setNome(produtoRequest.getNome());
        produto.setDescricao(produtoRequest.getDescricao());
        produto.setPreco(produtoRequest.getPreco());
        produto.setQuantidade(produtoRequest.getQuantidade());
        produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produto.setCategoria(categoria);
        produto.setOrgId(orgId);  // Associando a organização ao produto

        produtoRepository.save(produto);

        return ResponseEntity.ok(Map.of("message", "Produto criado com sucesso!"));
    }

    // Excluir um produto
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> excluir(@PathVariable Long id, @RequestParam Long orgId) {
        if (!produtoRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Produto não encontrado."));
        }

        service.excluir(id, orgId);
        return ResponseEntity.ok(Map.of("message", "Produto excluído com sucesso."));
    }

    // Buscar produto por ID
    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(@PathVariable Long id, @RequestParam Long orgId) {
        Produto produto = produtoRepository.findByIdAndOrgId(id, orgId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID fornecido."));

        return ResponseEntity.ok(new ProdutoDto(produto));
    }
}
