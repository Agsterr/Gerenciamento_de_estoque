package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusDispositivo;
import br.softsistem.Gerenciamento_de_estoque.model.DispositivoUsuario;

import java.time.LocalDateTime;

public record DispositivoUsuarioDto(
        Long id,
        Long usuarioId,
        String username,
        Long orgId,
        String orgNome,
        String fingerprint,
        String userAgent,
        String nomeDispositivo,
        StatusDispositivo status,
        LocalDateTime solicitadoEm,
        LocalDateTime revisadoEm
) {
    public DispositivoUsuarioDto(DispositivoUsuario d) {
        this(
                d.getId(),
                d.getUsuario() != null ? d.getUsuario().getId() : null,
                d.getUsuario() != null ? d.getUsuario().getUsername() : null,
                d.getOrg() != null ? d.getOrg().getId() : null,
                d.getOrg() != null ? d.getOrg().getNome() : null,
                d.getFingerprint(),
                d.getUserAgent(),
                d.getNomeDispositivo(),
                d.getStatus(),
                d.getSolicitadoEm(),
                d.getRevisadoEm()
        );
    }
}
