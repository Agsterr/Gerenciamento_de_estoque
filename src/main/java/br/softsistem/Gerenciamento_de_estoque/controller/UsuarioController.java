package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.ReativarUsuarioRequest;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PutMapping("/{id}/ativar")
    public String ativarUsuario(@PathVariable Long id, @RequestParam Long orgId) {
        usuarioService.ativarUsuario(id, orgId);
        return "Usuário ativado com sucesso!";
    }

    @PostMapping("/reativar-usuario")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reativarUsuario(@RequestBody ReativarUsuarioRequest request, @RequestParam Long orgId) {
        usuarioService.reativarUsuario(request.username(), orgId);
        return ResponseEntity.ok("Usuário reativado com sucesso.");
    }

    @PutMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> desativarUsuario(@PathVariable Long id, @RequestParam Long orgId) {
        usuarioService.desativarUsuario(id, orgId);
        return ResponseEntity.ok("Usuário desativado com sucesso!");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ativos")
    public Page<Usuario> listarUsuariosAtivos(@RequestParam Long orgId, Pageable pageable) {
        return usuarioService.listarUsuariosAtivos(orgId, pageable);
    }
}
