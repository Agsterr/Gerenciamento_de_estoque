package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgRepository extends JpaRepository<Org, Long> {

    // Encontrar organização por ID
    Optional<Org> findById(Long id);

    // Buscar uma organização única pelo nome (para lógica de verificação)

    Optional<Org> findByNome(String nome);

    // Buscar várias organizações com o mesmo nome (paginado)
    Page<Org> findAllByNomeContaining(String nome, Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            "SELECT o.id FROM Org o WHERE o.ephemeral = true AND o.demoLastAccess IS NOT NULL AND o.demoLastAccess < :cutoff")
    List<Long> findEphemeralOrgIdsWithLastAccessBefore(
            @org.springframework.data.repository.query.Param("cutoff") java.time.LocalDateTime cutoff);
}
