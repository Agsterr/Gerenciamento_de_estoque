package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusContagemInventario;
import br.softsistem.Gerenciamento_de_estoque.model.ContagemInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContagemInventarioRepository extends JpaRepository<ContagemInventario, Long> {
    Page<ContagemInventario> findByOrgIdOrderByCriadoEmDesc(Long orgId, Pageable pageable);
    Optional<ContagemInventario> findByIdAndOrgId(Long id, Long orgId);
}
