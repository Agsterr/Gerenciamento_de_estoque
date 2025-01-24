package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.ReativarUsuarioRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {


    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }
    @Transactional
    @PutMapping("/{id}/ativar")
    public String ativarUsuario(@PathVariable Long id) {
        usuarioService.ativarUsuario(id);
        return "Usuário ativado com sucesso!";
    }

    @Transactional
    @PostMapping("/reativar-usuario")
    @PreAuthorize("hasRole('ADMIN')") // Apenas ADMIN pode acessar
    public ResponseEntity<String> reativarUsuario(@RequestBody ReativarUsuarioRequest request) {
        System.out.println("Username recebido: " + request.username());
        var usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (!usuario.getAtivo()) {
            usuario.setAtivo(true);
            usuarioRepository.save(usuario);
            return ResponseEntity.ok("Usuário reativado com sucesso.");
        }

        return ResponseEntity.badRequest().body("O usuário já está ativo.");
    }


    @Transactional
    @PutMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')") // Apenas ADMIN pode acessar
    public ResponseEntity<String> desativarUsuario(@PathVariable Long id) {
        return usuarioRepository.findById(id)
                .map(usuario -> {
                    usuario.setAtivo(false); // Desativa o usuário
                    usuarioRepository.save(usuario);
                    return ResponseEntity.ok("Usuário desativado com sucesso!");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasRole('ADMIN')") // Apenas ADMIN pode acessar
    @GetMapping("/ativos")
    public List<Usuario> listarUsuariosAtivos() {
        return usuarioService.listarUsuariosAtivos();
    }
}



