package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DemoCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(DemoCleanupJob.class);

    private final OrgRepository orgRepository;
    private final DemoOrgPurgeService demoOrgPurgeService;

    public DemoCleanupJob(OrgRepository orgRepository, DemoOrgPurgeService demoOrgPurgeService) {
        this.orgRepository = orgRepository;
        this.demoOrgPurgeService = demoOrgPurgeService;
    }

    /** Limpa orgs demo abandonadas há mais de 2 horas. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeStaleDemoOrgs() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(2);
        List<Long> staleOrgIds = orgRepository.findEphemeralOrgIdsWithLastAccessBefore(cutoff);
        for (Long orgId : staleOrgIds) {
            log.info("Job demo: limpando org efêmera id={}", orgId);
            demoOrgPurgeService.purgeOperationalData(orgId);
            orgRepository.findById(orgId).ifPresent(org -> {
                org.setDemoLastAccess(null);
                orgRepository.save(org);
            });
        }
    }
}
