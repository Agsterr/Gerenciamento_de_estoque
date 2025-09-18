package br.softsistem.Gerenciamento_de_estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
<<<<<<< HEAD
import org.springframework.scheduling.annotation.EnableScheduling;
=======
>>>>>>> 977b3b0aab201d82e28cc2ef2b8518837abb38ea
import java.util.TimeZone;

@SpringBootApplication
@ComponentScan(basePackages = "br.softsistem.Gerenciamento_de_estoque")
<<<<<<< HEAD
@EnableScheduling
=======
>>>>>>> 977b3b0aab201d82e28cc2ef2b8518837abb38ea
public class GerenciamentoDeEstoqueApplication {

    public static void main(String[] args) {
        // Garante que toda a aplicação use o fuso horário de São Paulo
        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        System.setProperty("user.timezone", "America/Sao_Paulo");
        SpringApplication.run(GerenciamentoDeEstoqueApplication.class, args);
    }

}
