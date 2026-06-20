package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.model.PesquisaPreco;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PesquisaPrecoDto(
        Long id,
        Long usuarioId,
        Long orgId,
        String orgNome,
        String username,
        BigDecimal valorMin,
        BigDecimal valorMax,
        String comentario,
        LocalDateTime criadoEm,
        LocalDateTime atualizadoEm
) {
    public PesquisaPrecoDto(PesquisaPreco entity) {
        this(
                entity.getId(),
                entity.getUsuario() != null ? entity.getUsuario().getId() : null,
                entity.getOrg() != null ? entity.getOrg().getId() : null,
                entity.getOrg() != null ? entity.getOrg().getNome() : null,
                entity.getUsername(),
                entity.getValorMin(),
                entity.getValorMax(),
                entity.getComentario(),
                entity.getCriadoEm(),
                entity.getAtualizadoEm()
        );
    }
}
