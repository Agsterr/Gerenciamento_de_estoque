package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.entregaDto.EntregaResponseDto;
import br.softsistem.Gerenciamento_de_estoque.model.Entrega;
import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.EntregaRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.ProdutoRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/entregas")
public class EntregaController {

    private final EntregaRepository entregaRepository;
    private final ProdutoRepository produtoRepository;
    private final UsuarioRepository usuarioRepository;

    public EntregaController(EntregaRepository entregaRepository, ProdutoRepository produtoRepository, UsuarioRepository usuarioRepository) {
        this.entregaRepository = entregaRepository;
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    public ResponseEntity<String> registrarEntrega(@RequestBody EntregaRequestDto request) {
        // Obtendo o usuário logado
        String username = getLoggedUserUsername();

        Usuario entregador = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário logado não encontrado."));

        Produto produto = produtoRepository.findById(request.getProdutoId())
                .orElseThrow(() -> new RuntimeException("Produto não encontrado."));

        // Valida a quantidade em estoque
        if (produto.getQuantidade() < request.getQuantidade()) {
            return ResponseEntity.badRequest().body("Quantidade insuficiente no estoque.");
        }

        // Atualiza o estoque do produto
        produto.setQuantidade(produto.getQuantidade() - request.getQuantidade());
        produtoRepository.save(produto);

        // Cria e salva a entrega
        Entrega entrega = criarEntrega(request, entregador, produto);
        entregaRepository.save(entrega);

        return ResponseEntity.ok("Entrega registrada com sucesso.");
    }

    private String getLoggedUserUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        } else {
            return principal.toString();
        }
    }

    private Entrega criarEntrega(EntregaRequestDto request, Usuario entregador, Produto produto) {
        Entrega entrega = new Entrega();
        entrega.setConsumidor(request.getConsumidor());
        entrega.setProduto(produto);
        entrega.setEntregador(entregador);
        entrega.setQuantidade(request.getQuantidade());
        entrega.setHorarioEntrega(LocalDateTime.now());
        return entrega;
    }

    @GetMapping
    public ResponseEntity<List<EntregaResponseDto>> listarEntregas() {
        // Busca todas as entregas do banco de dados
        List<Entrega> entregas = entregaRepository.findAll();

        // Converte a lista de entidades Entrega em uma lista de DTOs EntregaResponse
        List<EntregaResponseDto> entregaResponses = entregas.stream()
                .map(EntregaResponseDto::fromEntity)
                .toList();

        // Retorna a lista de entregas com status HTTP 200
        return ResponseEntity.ok(entregaResponses);
    }

}
