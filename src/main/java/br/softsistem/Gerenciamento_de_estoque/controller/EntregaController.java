package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/entregas")
public class EntregaController {

    private final EntregaService entregaService;

    // Constructor Injection
    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    // Criar uma nova entrega
    @PostMapping
    public ResponseEntity<EntregaResponseDto> criarEntrega(@RequestBody @Valid EntregaRequestDto request) {
        // O orgId é recuperado no serviço, não no controlador.
        Entrega entrega = entregaService.criarEntrega(request);

        // Retornar a resposta com os dados da entrega criada
        EntregaResponseDto responseDto = EntregaResponseDto.fromEntity(entrega);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);  // Retorna com código 201 (Criado)
    }

    // Editar uma entrega existente
    @PutMapping("/{id}")
    public ResponseEntity<EntregaResponseDto> editarEntrega(@PathVariable Long id, @RequestBody @Valid EntregaRequestDto request) {
        Entrega entregaAtualizada = entregaService.editarEntrega(id, request);

        if (entregaAtualizada == null) {
            throw new ResourceNotFoundException("Entrega não encontrada ou não pertence à organização.");
        }

        // Retornar os dados da entrega editada
        EntregaResponseDto responseDto = EntregaResponseDto.fromEntity(entregaAtualizada);
        return ResponseEntity.ok(responseDto);
    }

    // Deletar uma entrega existente
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEntrega(@PathVariable Long id) {
        entregaService.deletarEntrega(id);
        return ResponseEntity.noContent().build();  // Retorna 204 No Content
    }

    // Listar todas as entregas de uma organização
    @GetMapping
    public ResponseEntity<List<EntregaResponseDto>> listarEntregas() {
        List<EntregaResponseDto> responseDtos = entregaService.listarEntregas()
                .stream()
                .map(EntregaResponseDto::fromEntity)
                .toList();
        return ResponseEntity.ok(responseDtos);
    }

    // Consultar total de entregas por dia
    @GetMapping("/total-por-dia")
    public ResponseEntity<BigDecimal> totalPorDia(@RequestParam LocalDate dia) {
        BigDecimal total = entregaService.getTotalPorDia(dia);
        return ResponseEntity.ok(total);
    }

    // Consultar total de entregas por semana
    @GetMapping("/total-semanal")
    public ResponseEntity<BigDecimal> totalSemanal(@RequestParam LocalDate inicioSemana, @RequestParam LocalDate fimSemana) {
        BigDecimal total = entregaService.getTotalSemanal(inicioSemana, fimSemana);
        return ResponseEntity.ok(total);
    }

    // Consultar total de entregas por mês
    @GetMapping("/total-mensal")
    public ResponseEntity<BigDecimal> totalMensal(@RequestParam int mes, @RequestParam int ano) {
        BigDecimal total = entregaService.getTotalMensal(mes, ano);
        return ResponseEntity.ok(total);
    }

    // Consultar total de entregas feitas por um consumidor
    @GetMapping("/total-por-consumidor/{consumidorId}")
    public ResponseEntity<BigDecimal> totalPorConsumidor(@PathVariable Long consumidorId) {
        BigDecimal total = entregaService.getTotalPorConsumidor(consumidorId);
        return ResponseEntity.ok(total);
    }

    // Consultar total de entregas feitas no mês atual
    @GetMapping("/total-do-mes")
    public ResponseEntity<BigDecimal> totalDoMesAtual() {
        BigDecimal total = entregaService.getTotalDoMesAtual();
        return ResponseEntity.ok(total);
    }

    // Consultar total semanal de entregas por consumidor
    @GetMapping("/total-semanal-por-consumidor/{consumidorId}")
    public ResponseEntity<BigDecimal> totalSemanalPorConsumidor(@PathVariable Long consumidorId,
                                                                @RequestParam LocalDate inicioSemana,
                                                                @RequestParam LocalDate fimSemana) {
        BigDecimal total = entregaService.getTotalSemanalPorConsumidor(consumidorId, inicioSemana, fimSemana);
        return ResponseEntity.ok(total);
    }

    // Consultar total anual de entregas
    @GetMapping("/total-anual")
    public ResponseEntity<BigDecimal> totalAnual(@RequestParam int ano) {
        BigDecimal total = entregaService.getTotalAnual(ano);
        return ResponseEntity.ok(total);
    }

    // Consultar total anual de entregas por consumidor
    @GetMapping("/total-anual-por-consumidor/{consumidorId}")
    public ResponseEntity<BigDecimal> totalAnualPorConsumidor(@PathVariable Long consumidorId, @RequestParam int ano) {
        BigDecimal total = entregaService.getTotalAnualPorConsumidor(consumidorId, ano);
        return ResponseEntity.ok(total);
    }
}
