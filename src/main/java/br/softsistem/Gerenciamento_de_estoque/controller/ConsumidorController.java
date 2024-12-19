package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/consumidores")
public class ConsumidorController {

    private final ConsumidorRepository consumidorRepository;

    public ConsumidorController(ConsumidorRepository consumidorRepository) {
        this.consumidorRepository = consumidorRepository;
    }

    @PostMapping
    public ResponseEntity<Consumidor> adicionarConsumidor(@RequestBody Consumidor consumidor) {
        Consumidor salvo = consumidorRepository.save(consumidor);
        return ResponseEntity.ok(salvo);
    }

    @GetMapping
    public ResponseEntity<List<Consumidor>> listarConsumidores() {
        List<Consumidor> consumidores = consumidorRepository.findAll();
        return ResponseEntity.ok(consumidores);
    }
}
