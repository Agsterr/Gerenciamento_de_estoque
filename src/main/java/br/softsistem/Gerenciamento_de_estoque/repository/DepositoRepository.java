package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DepositoRepository extends JpaRepository<Deposito, Long> {
    List<Deposito> findByOrgIdAndAtivoTrue(Long orgId);
    Optional<Deposito> findByIdAndOrgId(Long id, Long orgId);
    Optional<Deposito> findByOrgIdAndPadraoTrue(Long orgId);
    long countByOrgId(Long orgId);
}
