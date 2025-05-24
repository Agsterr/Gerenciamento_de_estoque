package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgRepository extends JpaRepository<Org, Long> {

    // Encontrar organização por ID
    Optional<Org> findById(Long id);

    // Buscar uma organização única pelo nome (para lógica de verificação)

    Optional<Org> findByNome(String nome);

    // Buscar várias organizações com o mesmo nome (paginado)
    Page<Org> findAllByNomeContaining(String nome, Pageable pageable);
}
