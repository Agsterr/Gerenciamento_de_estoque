package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import br.softsistem.Gerenciamento_de_estoque.model.AcessoLogin;

import java.time.LocalDateTime;

public record AcessoLoginDto(
        Long id,
        Long usuarioId,
        Long orgId,
        String orgNome,
        String username,
        String ip,
        String userAgent,
        LocalDateTime dataHora,
        boolean sucesso,
        String detalhes
) {
    public AcessoLoginDto(AcessoLogin log) {
        this(
                log.getId(),
                log.getUsuario() != null ? log.getUsuario().getId() : null,
                log.getOrg() != null ? log.getOrg().getId() : null,
                log.getOrg() != null ? log.getOrg().getNome() : null,
                log.getUsername(),
                log.getIp(),
                log.getUserAgent(),
                log.getDataHora(),
                Boolean.TRUE.equals(log.getSucesso()),
                log.getDetalhes()
        );
    }
}
