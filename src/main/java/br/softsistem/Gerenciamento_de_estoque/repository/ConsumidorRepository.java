package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConsumidorRepository extends JpaRepository<Consumidor, Long> {

    // Buscar todos os consumidores de uma organização com paginação
    Page<Consumidor> findByOrg_Id(Long orgId, Pageable pageable);

    // Buscar consumidor por nome e organização
    Optional<Consumidor> findByNomeAndOrg_Id(String nome, Long orgId);

    // Buscar consumidor por ID e organização (segurança multi-tenant)
    Optional<Consumidor> findByIdAndOrgId(Long id, Long orgId);
}
