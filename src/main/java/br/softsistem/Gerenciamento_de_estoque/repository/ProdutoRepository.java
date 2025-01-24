package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    // Busca um produto pelo nome
    Produto findByNome(String nome);

    // Busca todos os produtos ativos com paginação
    Page<Produto> findByAtivoTrue(Pageable pageable);
}
