package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.ConsumidorNaoEncontradoException;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.service.ConsumidorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/consumidores")
public class ConsumidorController {

    private final ConsumidorService consumidorService;

    // Constructor Injection
    public ConsumidorController(ConsumidorService consumidorService) {
        this.consumidorService = consumidorService;
    }

    @PostMapping
    public ResponseEntity<ConsumidorDtoResponse> criarConsumidor(@Valid @RequestBody ConsumidorDtoRequest consumidorDtoRequest) {
        Consumidor consumidor = consumidorDtoRequest.toEntity();
        consumidor = consumidorService.salvar(consumidor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConsumidorDtoResponse.fromEntity(consumidor));
    }

    @GetMapping
    public ResponseEntity<List<ConsumidorDtoResponse>> listarConsumidores() {
        List<ConsumidorDtoResponse> consumidores = consumidorService.listarTodos().stream()
                .map(ConsumidorDtoResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(consumidores);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarConsumidor(@PathVariable Long id) {
        try {
            consumidorService.excluir(id);
            return ResponseEntity.noContent().build();
        } catch (ConsumidorNaoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsumidorDtoResponse> editarConsumidor(@PathVariable Long id, @Valid @RequestBody ConsumidorDtoRequest consumidorDtoRequest) {
        Consumidor consumidorAtualizado = consumidorDtoRequest.toEntity();
        Consumidor consumidor = consumidorService.editar(id, consumidorAtualizado);
        if (consumidor != null) {
            return ResponseEntity.ok(ConsumidorDtoResponse.fromEntity(consumidor));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
