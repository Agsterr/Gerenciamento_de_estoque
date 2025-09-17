package br.softsistem.Gerenciamento_de_estoque.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração para carregar variáveis de ambiente do arquivo .env
 */
@Slf4j
public class DotenvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // Carrega o arquivo .env se existir
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();

            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            Map<String, Object> dotenvProperties = new HashMap<>();

            // Adiciona todas as variáveis do .env ao environment do Spring
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                dotenvProperties.put(key, value);
                log.debug("Carregada variável de ambiente: {}", key);
            });

            // Adiciona as propriedades ao environment com alta prioridade
            environment.getPropertySources().addFirst(
                    new MapPropertySource("dotenvProperties", dotenvProperties)
            );

            log.info("Arquivo .env carregado com sucesso. {} variáveis carregadas.", dotenvProperties.size());

        } catch (Exception e) {
            log.warn("Erro ao carregar arquivo .env: {}", e.getMessage());
            log.info("Continuando sem arquivo .env. Certifique-se de que as variáveis de ambiente estão configuradas.");
        }
    }
}