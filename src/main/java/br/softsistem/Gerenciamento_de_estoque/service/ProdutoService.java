package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProdutoService {

    @Autowired
    private ProdutoRepository repository;

    // Método paginado para listar produtos ativos
    public Page<Produto> listarTodos(Pageable pageable) {
        return repository.findByAtivoTrue(pageable);
    }

    // Salva ou atualiza um produto (soma a quantidade se o produto já existir)
    public Produto salvar(Produto produto) {
        // Verificar se já existe um produto com o mesmo nome
        Produto produtoExistente = repository.findByNome(produto.getNome());
        if (produtoExistente != null) {
            // Soma a quantidade do produto existente com a quantidade do novo produto
            produtoExistente.setQuantidade(produtoExistente.getQuantidade() + produto.getQuantidade());
            return repository.save(produtoExistente); // Atualiza o produto existente
        }
        return repository.save(produto); // Salva como um novo produto
    }

    // Marca o produto como inativo em vez de deletá-lo
    public void excluir(Long id) {
        Optional<Produto> produtoOpt = repository.findById(id);
        if (produtoOpt.isPresent()) {
            Produto produto = produtoOpt.get();
            produto.setAtivo(false); // Marca como inativo
            repository.save(produto); // Salva a alteração
        } else {
            throw new RuntimeException("Produto não encontrado com ID: " + id);
        }
    }
}
