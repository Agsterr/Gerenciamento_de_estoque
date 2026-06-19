package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusPedidoCompra;
import br.softsistem.Gerenciamento_de_estoque.model.PedidoCompra;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PedidoCompraRepository extends JpaRepository<PedidoCompra, Long> {
    Page<PedidoCompra> findByOrgIdOrderByCriadoEmDesc(Long orgId, Pageable pageable);
    Optional<PedidoCompra> findByIdAndOrgId(Long id, Long orgId);
    long countByOrgId(Long orgId);
    boolean existsByNumeroAndOrgId(String numero, Long orgId);
}
