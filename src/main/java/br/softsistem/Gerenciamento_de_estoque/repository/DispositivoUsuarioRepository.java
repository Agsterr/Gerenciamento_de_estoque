package br.softsistem.Gerenciamento_de_estoque.repository;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusDispositivo;
import br.softsistem.Gerenciamento_de_estoque.model.DispositivoUsuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DispositivoUsuarioRepository extends JpaRepository<DispositivoUsuario, Long> {
    Optional<DispositivoUsuario> findByUsuarioIdAndFingerprint(Long usuarioId, String fingerprint);
    Page<DispositivoUsuario> findByStatusOrderBySolicitadoEmDesc(StatusDispositivo status, Pageable pageable);
}
