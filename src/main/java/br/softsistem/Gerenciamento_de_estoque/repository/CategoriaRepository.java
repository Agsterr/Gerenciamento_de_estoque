package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    // Buscar uma categoria pelo nome e pelo org_id (Organização)
    Optional<Categoria> findByNomeAndOrg_Id(String nome, Long orgId);

    // Buscar categorias cujo nome contenha uma parte do nome e que pertençam a uma organização específica
    List<Categoria> findByNomeContainingAndOrg_Id(String parteDoNome, Long orgId);

    // Buscar todas as categorias de uma organização específica
    List<Categoria> findByOrg_Id(Long orgId);

}
