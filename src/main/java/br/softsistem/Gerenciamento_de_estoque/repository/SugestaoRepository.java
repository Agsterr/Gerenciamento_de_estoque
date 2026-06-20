package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Sugestao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SugestaoRepository extends JpaRepository<Sugestao, Long> {
    Page<Sugestao> findByOrgIdOrderByCriadoEmDesc(Long orgId, Pageable pageable);
    Page<Sugestao> findAllByOrderByCriadoEmDesc(Pageable pageable);
}
