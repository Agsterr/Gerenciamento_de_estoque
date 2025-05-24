package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsumidorRepository extends JpaRepository<Consumidor, Long> {

    // Método para buscar todos os consumidores de uma organização específica com paginação
    Page<Consumidor> findByOrg_Id(Long orgId, Pageable pageable);

    // Método para buscar um consumidor específico por nome e org_id
    Optional<Consumidor> findByNomeAndOrg_Id(String nome, Long orgId);

    // Outros métodos personalizados podem ser adicionados aqui, se necessário
}
