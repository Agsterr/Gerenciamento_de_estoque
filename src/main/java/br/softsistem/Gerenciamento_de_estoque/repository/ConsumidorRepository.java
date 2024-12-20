package br.softsistem.Gerenciamento_de_estoque.repository;


import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsumidorRepository extends JpaRepository<Consumidor, Long> {
    // Métodos personalizados podem ser adicionados aqui, se necessário
}
