package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Org;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrgRepository extends JpaRepository<Org, Long> {

    // Encontrar organização por ID
    Optional<Org> findById(Long id);

    // --- Adicione este método para buscar uma organização pelo nome ---
    Optional<Org> findByNome(String nome);
}

