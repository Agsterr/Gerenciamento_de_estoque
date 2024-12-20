package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDto;
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
    public ResponseEntity<ConsumidorDto> adicionarConsumidor(@RequestBody ConsumidorDto consumidorDto) {
        // Converte DTO para Entidade e salva
        Consumidor salvo = consumidorRepository.save(consumidorDto.toEntity());

        // Retorna o DTO convertido da Entidade salva
        return ResponseEntity.ok(ConsumidorDto.fromEntity(salvo));
    }

    @GetMapping
    public ResponseEntity<List<ConsumidorDto>> listarConsumidores() {
        // Busca entidades no banco e converte para lista de DTOs
        List<ConsumidorDto> consumidoresDto = consumidorRepository.findAll()
                .stream()
                .map(ConsumidorDto::fromEntity)
                .toList();

        return ResponseEntity.ok(consumidoresDto);
    }
}
