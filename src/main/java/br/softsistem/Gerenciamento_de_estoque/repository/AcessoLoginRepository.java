package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.model.AcessoLogin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcessoLoginRepository extends JpaRepository<AcessoLogin, Long> {
    Page<AcessoLogin> findAllByOrderByDataHoraDesc(Pageable pageable);
}
