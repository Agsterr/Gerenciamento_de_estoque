package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.DispositivoUsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusDispositivo;
import br.softsistem.Gerenciamento_de_estoque.repository.DispositivoUsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DispositivoUsuarioService {

    private final DispositivoUsuarioRepository repository;

    public DispositivoUsuarioService(DispositivoUsuarioRepository repository) {
        this.repository = repository;
    }

    /** Scaffold Fase 2: listagem para o painel master. */
    @Transactional(readOnly = true)
    public Page<DispositivoUsuarioDto> listarPendentes(Pageable pageable) {
        return repository.findByStatusOrderBySolicitadoEmDesc(StatusDispositivo.PENDING, pageable)
                .map(DispositivoUsuarioDto::new);
    }
}
