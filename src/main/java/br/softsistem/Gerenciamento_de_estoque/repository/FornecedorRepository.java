package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Fornecedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    Page<Fornecedor> findByOrgIdAndAtivoTrue(Long orgId, Pageable pageable);
    Page<Fornecedor> findByOrgId(Long orgId, Pageable pageable);
    Optional<Fornecedor> findByIdAndOrgId(Long id, Long orgId);
}
