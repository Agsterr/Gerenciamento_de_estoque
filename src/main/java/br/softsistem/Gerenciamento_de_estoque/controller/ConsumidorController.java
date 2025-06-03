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

import java.util.Map;

@RestController
@RequestMapping("/consumidores")
public class ConsumidorController {

    private final ConsumidorService consumidorService;
    private final ConsumidorRepository consumidorRepository;

    public ConsumidorController(ConsumidorService consumidorService,
                                ConsumidorRepository consumidorRepository) {
        this.consumidorService = consumidorService;
        this.consumidorRepository = consumidorRepository;
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
        Long orgId = SecurityUtils.getCurrentOrgId();

        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }

        // Busca a página de consumidores do repositório
        Page<Consumidor> page = consumidorRepository.findByOrg_Id(orgId, pageable);

        if (page == null) {
            page = Page.empty();  // Se a página for nula, retorna uma página vazia
        }

        // Converte para DTO
        Page<ConsumidorDtoResponse> dtoPage = page.map(ConsumidorDtoResponse::fromEntity);

        return ResponseEntity.ok(dtoPage);
    }




    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletarConsumidor(@PathVariable Long id) {
        // Chama o serviço para excluir o consumidor
        consumidorService.excluir(id);
        return ResponseEntity.ok(Map.of("message", "Consumidor excluído com sucesso"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsumidorDtoResponse> editarConsumidor(
            @PathVariable Long id,
            @Valid @RequestBody ConsumidorDtoRequest consumidorDtoRequest) {

        // Converte o DTO para a entidade Consumidor
        Consumidor entidadeAtualizada = consumidorDtoRequest.toEntity();

        // Verifica se o consumidor existe, se não, lança a exceção
        Consumidor consumidorExistente = consumidorRepository.findById(id)
                .orElseThrow(() -> new ConsumidorNaoEncontradoException("Consumidor não encontrado"));

        // Atualiza os dados do consumidor
        consumidorExistente.setNome(entidadeAtualizada.getNome());
        consumidorExistente.setCpf(entidadeAtualizada.getCpf());
        consumidorExistente.setEndereco(entidadeAtualizada.getEndereco());

        // Salva o consumidor atualizado
        Consumidor consumidorAtualizado = consumidorRepository.save(consumidorExistente);
        ConsumidorDtoResponse response = ConsumidorDtoResponse.fromEntity(consumidorAtualizado);

        return ResponseEntity.ok(response); // Retorna o consumidor atualizado com status 200 (OK)
    }
}
