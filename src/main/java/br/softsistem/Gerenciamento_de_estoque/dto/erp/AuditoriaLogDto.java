package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.model.AuditoriaLog;
import java.time.LocalDateTime;

public record AuditoriaLogDto(
        Long id, String entidade, Long entidadeId, AcaoAuditoria acao,
        String usuario, String detalhes, Long orgId, LocalDateTime criadoEm
) {
    public AuditoriaLogDto(AuditoriaLog log) {
        this(log.getId(), log.getEntidade(), log.getEntidadeId(), log.getAcao(),
                log.getUsuario(), log.getDetalhes(), log.getOrg().getId(), log.getCriadoEm());
    }
}
