package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Método para buscar uma Role pelo nome
    Optional<Role> findByNome(String nome);

    // Método para buscar múltiplas roles pelos nomes
    List<Role> findByNomeIn(List<Role> nomes);

}
