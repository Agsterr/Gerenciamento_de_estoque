package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.registration")
public record RegistrationProperties(boolean enabled) {
    public RegistrationProperties {
        // default quando ausente no .env
    }
}
