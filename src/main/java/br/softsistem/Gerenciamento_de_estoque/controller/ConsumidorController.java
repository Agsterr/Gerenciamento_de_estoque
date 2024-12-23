package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoResponse;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/consumidores")
public class ConsumidorController {

    private final ConsumidorRepository consumidorRepository;

    public ConsumidorController(ConsumidorRepository consumidorRepository) {
        this.consumidorRepository = consumidorRepository;
    }

    @PostMapping
    public ResponseEntity<ConsumidorDtoResponse> criarConsumidor(@RequestBody ConsumidorDtoRequest consumidorDtoRequest) {
        Consumidor consumidor = consumidorDtoRequest.toEntity();
        consumidor = consumidorRepository.save(consumidor);
        return ResponseEntity.ok(ConsumidorDtoResponse.fromEntity(consumidor));
    }

    @GetMapping
    public ResponseEntity<List<ConsumidorDtoResponse>> listarConsumidores() {
        List<ConsumidorDtoResponse> consumidores = consumidorRepository.findAll()
                .stream()
                .map(ConsumidorDtoResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(consumidores);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletarConsumidor(@PathVariable Long id) {
        if (!consumidorRepository.existsById(id)) {
            return ResponseEntity.notFound().build(); // Retorna 404 se o ID não existir
        }
        consumidorRepository.deleteById(id);
        return ResponseEntity.ok("Consumidor deletado com sucesso.");
    }
}
