package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(String adminPassword) {
    public SeedProperties {
        if (adminPassword == null) {
            adminPassword = "";
        }
    }

    public boolean hasAdminPassword() {
        return !adminPassword.isBlank();
    }
}
