package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.ProdutoDto;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


    @RestController
    @RequestMapping("/produtos")
    public class ProdutoController {

        @Autowired
        private ProdutoService service;

        @GetMapping
        public List<ProdutoDto> listarTodos() {
            return service.listarTodos()
                    .stream().map(ProdutoDto::new)
                    .toList();
        }

        @PostMapping
        public ProdutoDto salvar(@Valid @RequestBody ProdutoDto produtoDto) {
            // Converte o DTO para Produto antes de salvar
            Produto produto = new Produto();
            produto.setNome(produtoDto.nome());
            produto.setQuantidade(produtoDto.quantidade());
            produto.setPreco(produtoDto.preco());
            produto.setCategoria(produtoDto.categoria());
            produto.setCriadoEm(produtoDto.dateTime());
            produto.setDescricao(produtoDto.descricao());

            Produto produtoSalvo = service.salvar(produto);



            // Retorna o Produto salvo como ProdutoDto
            return new ProdutoDto(produtoSalvo);
        }

        @DeleteMapping("/{id}")
        public void excluir(@PathVariable Long id) {
            service.excluir(id);
        }


    }

