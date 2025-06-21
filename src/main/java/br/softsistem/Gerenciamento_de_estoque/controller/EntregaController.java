package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaComAvisoResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.service.EntregaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/entregas")
public class EntregaController {

    private final EntregaService entregaService;

    public EntregaController(EntregaService entregaService) {
        this.entregaService = entregaService;
    }

    // =========================
    // CRUD / PAGINAÇÃO
    // =========================

    @PostMapping
    public ResponseEntity<EntregaComAvisoResponseDto> criarEntrega(
            @RequestBody @Valid EntregaRequestDto request
    ) {
        EntregaComAvisoResponseDto response = entregaService.criarEntrega(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EntregaResponseDto> editarEntrega(
            @PathVariable Long id,
            @RequestBody @Valid EntregaRequestDto request
    ) {
        Entrega entregaAtualizada = entregaService.editarEntrega(id, request);
        if (entregaAtualizada == null) {
            throw new ResourceNotFoundException("Entrega não encontrada ou não pertence à organização.");
        }
        EntregaResponseDto responseDto = EntregaResponseDto.fromEntity(entregaAtualizada);
        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarEntrega(@PathVariable Long id) {
        entregaService.deletarEntrega(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<EntregaResponseDto>> listarEntregas(Pageable pageable) {
        Page<EntregaResponseDto> pageDto = entregaService.listarEntregas(pageable)
                .map(EntregaResponseDto::fromEntity);
        return ResponseEntity.ok(pageDto);
    }

    // =========================
    // NOVOS ENDPOINTS (RESPOSTA DETALHADA)
    // =========================

    /**
     * Lista todas as entregas detalhadas de um dia específico.
     * GET /entregas/por-dia?dia=YYYY-MM-DD
     */

    @GetMapping("/por-dia")
    public ResponseEntity<Page<EntregaResponseDto>> porDia(
            @RequestParam("dia") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dia,
            Pageable pageable) {
        Page<EntregaResponseDto> pageDto = entregaService.listarEntregasPorDia(dia, pageable);
        return ResponseEntity.ok(pageDto);
    }

    /**
     * Lista todas as entregas detalhadas em um intervalo de data/hora.
     * GET /entregas/por-periodo?inicio=YYYY-MM-DDTHH:MM:SS&fim=YYYY-MM-DDTHH:MM:SS
     */

    @GetMapping("/por-periodo")
    public ResponseEntity<Page<EntregaResponseDto>> porPeriodo(
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam("fim")    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            Pageable pageable) {
        Page<EntregaResponseDto> pageDto = entregaService.listarEntregasPorPeriodo(inicio, fim, pageable);
        return ResponseEntity.ok(pageDto);
    }

    /**
     * Lista todas as entregas detalhadas de um mês e ano.
     * GET /entregas/por-mes?mes=MM&ano=YYYY
     */
    @GetMapping("/por-mes")
    public ResponseEntity<Page<EntregaResponseDto>> porMes(
            @RequestParam("mes") int mes,
            @RequestParam("ano") int ano,
            Pageable pageable) {
        Page<EntregaResponseDto> pageDto = entregaService.listarEntregasPorMes(mes, ano, pageable);
        return ResponseEntity.ok(pageDto);
    }


    /**
     * Lista todas as entregas detalhadas de um ano inteiro.
     * GET /entregas/por-ano?ano=YYYY
     */

    @GetMapping("/por-ano")
    public ResponseEntity<Page<EntregaResponseDto>> porAno(
            @RequestParam("ano") int ano,
            Pageable pageable) {
        Page<EntregaResponseDto> pageDto = entregaService.listarEntregasPorAno(ano, pageable);
        return ResponseEntity.ok(pageDto);
    }

    /**
     * Lista todas as entregas detalhadas de um consumidor específico (sem filtro de data).
     * GET /entregas/por-consumidor/{consumidorId}
     */

    @GetMapping("/por-consumidor/{consumidorId}")
    public ResponseEntity<Page<EntregaResponseDto>> porConsumidor(
            @PathVariable Long consumidorId,
            Pageable pageable) {
        Page<EntregaResponseDto> pageDto = entregaService.listarEntregasPorConsumidor(consumidorId, pageable);
        return ResponseEntity.ok(pageDto);
    }

    /**
     * Lista todas as entregas detalhadas de um consumidor em um intervalo de data/hora.
     * GET /entregas/por-consumidor/{consumidorId}/periodo?inicio=...&fim=...
     */
    @GetMapping("/por-consumidor/{consumidorId}/periodo")
    public ResponseEntity<Page<EntregaResponseDto>> porConsumidorPeriodo(
            @PathVariable Long consumidorId,
            @RequestParam("inicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam("fim")    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim,
            Pageable pageable) {
        Page<EntregaResponseDto> pageDto = entregaService.listarEntregasPorConsumidorPorPeriodo(consumidorId, inicio, fim, pageable);
        return ResponseEntity.ok(pageDto);
    }



}
