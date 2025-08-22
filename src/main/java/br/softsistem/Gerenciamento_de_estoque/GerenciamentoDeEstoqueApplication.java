package br.softsistem.Gerenciamento_de_estoque;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "br.softsistem.Gerenciamento_de_estoque")
public class GerenciamentoDeEstoqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(GerenciamentoDeEstoqueApplication.class, args);
    }

}
