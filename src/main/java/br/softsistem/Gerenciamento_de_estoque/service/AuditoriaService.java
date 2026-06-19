package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.AcaoAuditoria;
import br.softsistem.Gerenciamento_de_estoque.model.AuditoriaLog;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.AuditoriaLogRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditoriaService {

    private final AuditoriaLogRepository repository;
    private final OrgRepository orgRepository;

    public AuditoriaService(AuditoriaLogRepository repository, OrgRepository orgRepository) {
        this.repository = repository;
        this.orgRepository = orgRepository;
    }

    @Transactional
    public void registrar(String entidade, Long entidadeId, AcaoAuditoria acao, String detalhes) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) return;
        Org org = orgRepository.findById(orgId).orElse(null);
        if (org == null) return;

        AuditoriaLog log = new AuditoriaLog();
        log.setEntidade(entidade);
        log.setEntidadeId(entidadeId);
        log.setAcao(acao);
        log.setDetalhes(detalhes);
        log.setOrg(org);
        log.setUsuario(SecurityUtils.getCurrentUsername());
        repository.save(log);
    }

    public Page<AuditoriaLog> listar(Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return repository.findByOrgIdOrderByCriadoEmDesc(orgId, pageable);
    }

    public Page<AuditoriaLog> listarPorEntidade(String entidade, Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();
        return repository.findByOrgIdAndEntidadeOrderByCriadoEmDesc(orgId, entidade, pageable);
    }
}
