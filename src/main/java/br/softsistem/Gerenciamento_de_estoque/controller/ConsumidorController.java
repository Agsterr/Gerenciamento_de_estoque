package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.consumidorDto.ConsumidorDtoResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.ConsumidorNaoEncontradoException;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.model.Consumidor;
import br.softsistem.Gerenciamento_de_estoque.repository.ConsumidorRepository;
import br.softsistem.Gerenciamento_de_estoque.service.ConsumidorService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/consumidores")
public class ConsumidorController {

    private final ConsumidorService consumidorService;
    private final ConsumidorRepository consumidorRepository;

    // Constructor Injection
    public ConsumidorController(ConsumidorService consumidorService, ConsumidorRepository consumidorRepository) {
        this.consumidorService = consumidorService;
        this.consumidorRepository = consumidorRepository;
    }

    @PostMapping
    public ResponseEntity<ConsumidorDtoResponse> criarConsumidor(@Valid @RequestBody ConsumidorDtoRequest consumidorDtoRequest) {
        Consumidor consumidor = consumidorDtoRequest.toEntity();
        consumidor = consumidorService.salvar(consumidor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ConsumidorDtoResponse.fromEntity(consumidor));
    }

    // Listar todos os consumidores de uma organização com paginação
    @GetMapping
    public Page<ConsumidorDtoResponse> listarTodos(Pageable pageable) {
        Long orgId = SecurityUtils.getCurrentOrgId();  // Obtém o org_id do contexto de segurança
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        // Busca os consumidores paginados e mapeia para o DTO
        Page<Consumidor> consumidores = consumidorRepository.findByOrg_Id(orgId, pageable);
        return consumidores.map(ConsumidorDtoResponse::fromEntity);  // Mapeia para DTO com paginação
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletarConsumidor(@PathVariable Long id) {
        try {
            consumidorService.excluir(id);
            return ResponseEntity.ok(Map.of("message", "Consumidor excluído com sucesso"));
        } catch (ConsumidorNaoEncontradoException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Consumidor não encontrado"));
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
