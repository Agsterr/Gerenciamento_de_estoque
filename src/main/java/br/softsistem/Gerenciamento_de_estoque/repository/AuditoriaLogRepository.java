package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.AuditoriaLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaLogRepository extends JpaRepository<AuditoriaLog, Long> {
    Page<AuditoriaLog> findByOrgIdOrderByCriadoEmDesc(Long orgId, Pageable pageable);
    Page<AuditoriaLog> findByOrgIdAndEntidadeOrderByCriadoEmDesc(Long orgId, String entidade, Pageable pageable);
}
