package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


    @RestController
    @RequestMapping("/produtos")
    public class ProdutoController {

        @Autowired
        private ProdutoService service;

        @GetMapping
        public List<Produto> listarTodos() {
            return service.listarTodos();
        }

        @PostMapping
        public Produto salvar(@RequestBody Produto produto) {
            return service.salvar(produto);
        }


    }

