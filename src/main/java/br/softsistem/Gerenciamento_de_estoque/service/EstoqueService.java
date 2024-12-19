package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstoqueService {

    private final ProdutoRepository produtoRepository;

    public EstoqueService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @Scheduled(fixedRate = 60000) // Verifica a cada 1 minuto
    public void verificarEstoque() {
        List<Produto> produtos = produtoRepository.findAll();
        produtos.stream()
                .filter(produto -> produto.getQuantidade() <= produto.getQuantidadeMinima())
                .forEach(produto -> System.out.println("Aviso: Produto " + produto.getNome() + " está abaixo da quantidade mínima!"));
    }
}

