package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Produto findByNomeAndOrgId(String nome, Long orgId);

    Optional<Produto> findByIdAndOrgId(Long id, Long orgId);

    Page<Produto> findByAtivoTrueAndOrgId(Long orgId, Pageable pageable);
}
