package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.produtoDto.ProdutoRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.CategoriaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProdutoController(ProdutoRepository produtoRepository, CategoriaRepository categoriaRepository) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    @Autowired
    private ProdutoService service;

    @GetMapping
    public Page<ProdutoDto> listarTodos(Pageable pageable) {
        return service.listarTodos(pageable).map(ProdutoDto::new);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> criarProduto(@RequestBody @Valid ProdutoRequest produtoRequest) {
        Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));

        Produto produto = new Produto();
        produto.setNome(produtoRequest.getNome());
        produto.setDescricao(produtoRequest.getDescricao());
        produto.setPreco(produtoRequest.getPreco());
        produto.setQuantidade(produtoRequest.getQuantidade());
        produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
        produto.setCategoria(categoria);

        produto.setId(null);

        produtoRepository.save(produto);

        return ResponseEntity.ok(Map.of("message", "Produto criado com sucesso!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> excluir(@PathVariable Long id) {
        if (!produtoRepository.existsById(id)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Produto não encontrado."));
        }

        service.excluir(id);
        return ResponseEntity.ok(Map.of("message", "Produto excluído com sucesso."));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDto> buscarPorId(@PathVariable Long id) {
        Produto produto = produtoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com o ID fornecido."));

        return ResponseEntity.ok(new ProdutoDto(produto));
    }
}
