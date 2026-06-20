package br.softsistem.Gerenciamento_de_estoque.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo")
public record DemoProperties(
        boolean enabled,
        String orgName,
        String username,
        String password
) {
    public DemoProperties {
        if (orgName == null || orgName.isBlank()) {
            orgName = "org_demo";
        }
        if (username == null || username.isBlank()) {
            username = "demo";
        }
        if (password == null || password.isBlank()) {
            password = "demo123";
        }
    }
}
