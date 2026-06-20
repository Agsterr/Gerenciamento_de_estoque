package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.PesquisaPreco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PesquisaPrecoRepository extends JpaRepository<PesquisaPreco, Long> {

    Optional<PesquisaPreco> findByUsuarioId(Long usuarioId);

    List<PesquisaPreco> findAllByOrderByCriadoEmDesc();
}
