package br.softsistem.Gerenciamento_de_estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(basePackages = "br.softsistem.Gerenciamento_de_estoque")
@EnableScheduling
public class GerenciamentoDeEstoqueApplication {

    public static void main(String[] args) {
        // Garante que toda a aplicação use o fuso horário de São Paulo
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        System.setProperty("user.timezone", "America/Sao_Paulo");
        SpringApplication.run(GerenciamentoDeEstoqueApplication.class, args);
    }

}
