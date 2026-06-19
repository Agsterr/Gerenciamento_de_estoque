package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoResponse;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.service.ConsumidorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/consumidores")
public class ConsumidorController {

    private final ConsumidorService consumidorService;

    public ConsumidorController(ConsumidorService consumidorService) {
        this.consumidorService = consumidorService;
    }

    @PostMapping
    public ResponseEntity<ConsumidorDtoResponse> criarConsumidor(
            @Valid @RequestBody ConsumidorDtoRequest consumidorDtoRequest) {
        Consumidor entidade = consumidorDtoRequest.toEntity();
        Consumidor salvo = consumidorService.salvar(entidade);
        ConsumidorDtoResponse response = ConsumidorDtoResponse.fromEntity(salvo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ConsumidorDtoResponse>> listarTodos(Pageable pageable) {
        Page<ConsumidorDtoResponse> dtoPage = consumidorService.listarTodos(pageable)
                .map(ConsumidorDtoResponse::fromEntity);
        return ResponseEntity.ok(dtoPage);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsumidorDtoResponse> buscarPorId(@PathVariable Long id) {
        Consumidor consumidor = consumidorService.buscarPorId(id);
        return ResponseEntity.ok(ConsumidorDtoResponse.fromEntity(consumidor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletarConsumidor(@PathVariable Long id) {
        consumidorService.excluir(id);
        return ResponseEntity.ok(Map.of("message", "Consumidor excluído com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsumidorDtoResponse> editarConsumidor(
            @PathVariable Long id,
            @Valid @RequestBody ConsumidorDtoRequest consumidorDtoRequest) {
        Consumidor entidadeAtualizada = consumidorDtoRequest.toEntity();
        Consumidor consumidorAtualizado = consumidorService.editar(id, entidadeAtualizada);
        ConsumidorDtoResponse response = ConsumidorDtoResponse.fromEntity(consumidorAtualizado);
        return ResponseEntity.ok(response);
    }
}
