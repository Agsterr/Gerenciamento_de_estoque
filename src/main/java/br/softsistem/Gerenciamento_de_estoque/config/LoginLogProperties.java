package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.login-logs")
public record LoginLogProperties(
        String exportDir,
        boolean autoArchiveEnabled,
        int autoArchiveRetentionDays,
        String autoArchiveCron
) {
    public LoginLogProperties {
        if (exportDir == null || exportDir.isBlank()) {
            exportDir = "data/log-exports";
        }
        if (autoArchiveRetentionDays <= 0) {
            autoArchiveRetentionDays = 90;
        }
        if (autoArchiveCron == null || autoArchiveCron.isBlank()) {
            autoArchiveCron = "0 0 4 * * *";
        }
    }
}
