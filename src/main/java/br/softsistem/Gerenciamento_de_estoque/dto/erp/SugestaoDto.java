package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.model.Sugestao;

import java.time.LocalDateTime;

public record SugestaoDto(
        Long id,
        Long orgId,
        String orgNome,
        Long usuarioId,
        String username,
        String texto,
        LocalDateTime criadoEm,
        String status
) {
    public SugestaoDto(Sugestao s) {
        this(
                s.getId(),
                s.getOrg() != null ? s.getOrg().getId() : null,
                s.getOrg() != null ? s.getOrg().getNome() : null,
                s.getUsuario() != null ? s.getUsuario().getId() : null,
                s.getUsername(),
                s.getTexto(),
                s.getCriadoEm(),
                s.getStatus()
        );
    }
}
