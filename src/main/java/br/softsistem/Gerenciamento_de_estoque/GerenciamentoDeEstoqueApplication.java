package br.softsistem.Gerenciamento_de_estoque;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(basePackages = "br.softsistem.Gerenciamento_de_estoque")
@EnableScheduling
@EnableAsync
public class GerenciamentoDeEstoqueApplication {

    public static void main(String[] args) {
        carregarEnv();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        System.setProperty("user.timezone", "America/Sao_Paulo");
        SpringApplication.run(GerenciamentoDeEstoqueApplication.class, args);
    }

    /**
     * Carrega variáveis do arquivo .env para System.properties antes do Spring subir.
     * Variáveis de ambiente do SO têm prioridade: se já existir no SO, o .env NÃO sobrescreve.
     */
    private static void carregarEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> {
                String key = entry.getKey();
                if (System.getenv(key) == null) {
                    System.setProperty(key, entry.getValue());
                }
            });
        } catch (Exception e) {
            // Continuar sem .env; variáveis podem vir do sistema ou do perfil
        }
    }

}
