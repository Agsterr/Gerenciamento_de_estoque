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

            // Adiciona variáveis do .env, mas SÓ se não existirem no SO
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                String value = entry.getValue();
                if (System.getenv(key) == null) {
                    dotenvProperties.put(key, value);
                    log.debug("Carregada variável do .env: {}", key);
                } else {
                    log.debug("Variável '{}' já definida no SO, ignorando valor do .env", key);
                }
            });

            // Adiciona as propriedades ao environment com BAIXA prioridade (não sobrescreve SO)
            environment.getPropertySources().addLast(
                    new MapPropertySource("dotenvProperties", dotenvProperties)
            );

            log.info("Arquivo .env carregado com sucesso. {} variáveis aplicadas (variáveis do SO têm prioridade).", dotenvProperties.size());

        } catch (Exception e) {
            log.warn("Erro ao carregar arquivo .env: {}", e.getMessage());
            log.info("Continuando sem arquivo .env. Certifique-se de que as variáveis de ambiente estão configuradas.");
        }
    }
}