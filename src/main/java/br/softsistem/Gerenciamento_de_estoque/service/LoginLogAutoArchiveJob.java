package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.LoginLogProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoginLogAutoArchiveJob {

    private static final Logger log = LoggerFactory.getLogger(LoginLogAutoArchiveJob.class);

    private final LoginLogProperties properties;
    private final LoginAuditoriaService loginAuditoriaService;

    public LoginLogAutoArchiveJob(LoginLogProperties properties,
                                   LoginAuditoriaService loginAuditoriaService) {
        this.properties = properties;
        this.loginAuditoriaService = loginAuditoriaService;
    }

    /**
     * Compacta automaticamente logs mais antigos que {@code autoArchiveRetentionDays}.
     * Desligado por padrão — ative com LOGIN_LOG_AUTO_ARCHIVE=true.
     */
    @Scheduled(cron = "${app.login-logs.auto-archive-cron:0 0 4 * * *}")
    public void compactarLogsAntigos() {
        if (!properties.autoArchiveEnabled()) {
            return;
        }
        try {
            var result = loginAuditoriaService.compactarAntigosGlobal();
            if (result.registrosExportados() > 0 || result.registrosApagados() > 0) {
                log.info("Auto-arquivo login logs: {} exportados, {} apagados — {}",
                        result.registrosExportados(), result.registrosApagados(), result.filename());
            }
        } catch (Exception e) {
            log.warn("Falha no job de auto-arquivo de login logs: {}", e.getMessage());
        }
    }
}
