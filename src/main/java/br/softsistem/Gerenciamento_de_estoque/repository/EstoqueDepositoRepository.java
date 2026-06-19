package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.EstoqueDeposito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EstoqueDepositoRepository extends JpaRepository<EstoqueDeposito, Long> {
    Optional<EstoqueDeposito> findByProdutoIdAndDepositoId(Long produtoId, Long depositoId);
    List<EstoqueDeposito> findByDepositoIdAndOrgId(Long depositoId, Long orgId);
    List<EstoqueDeposito> findByProdutoIdAndOrgId(Long produtoId, Long orgId);
}
