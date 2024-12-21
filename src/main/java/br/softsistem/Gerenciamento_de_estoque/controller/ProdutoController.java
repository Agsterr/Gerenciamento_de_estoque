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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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
        public List<ProdutoDto> listarTodos() {
            return service.listarTodos()
                    .stream().map(ProdutoDto::new)
                    .toList();
        }

        @PostMapping
        public ResponseEntity<String> criarProduto(@RequestBody @Valid ProdutoRequest produtoRequest) {
            // Busca a categoria vinculada
            Categoria categoria = categoriaRepository.findById(produtoRequest.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoria não encontrada."));

            // Converte o DTO para a entidade Produto
            Produto produto = new Produto();
            produto.setNome(produtoRequest.getNome());
            produto.setDescricao(produtoRequest.getDescricao());
            produto.setPreco(produtoRequest.getPreco());
            produto.setQuantidade(produtoRequest.getQuantidade());
            produto.setQuantidadeMinima(produtoRequest.getQuantidadeMinima());
            produto.setCategoria(categoria);

            // Salva o produto
            produtoRepository.save(produto);

            return ResponseEntity.ok("Produto criado com sucesso.");
        }


        @DeleteMapping("/{id}")
        public void excluir(@PathVariable Long id) {
            service.excluir(id);
        }


    }