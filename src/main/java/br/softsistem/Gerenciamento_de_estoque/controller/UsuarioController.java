package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.ReativarUsuarioRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<MensagemResponse> ativarUsuario(@PathVariable Long id, @RequestParam Long orgId) {
        usuarioService.ativarUsuario(id, orgId);
        return ResponseEntity.ok(new MensagemResponse("Usuário ativado com sucesso!"));
    }

    @PostMapping("/reativar-usuario")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensagemResponse> reativarUsuario(@RequestBody ReativarUsuarioRequest request, @RequestParam Long orgId) {
        usuarioService.reativarUsuario(request.username(), orgId);
        return ResponseEntity.ok(new MensagemResponse("Usuário reativado com sucesso."));
    }

    @PutMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensagemResponse> desativarUsuario(@PathVariable Long id, @RequestParam Long orgId) {
        usuarioService.desativarUsuario(id, orgId);
        return ResponseEntity.ok(new MensagemResponse("Usuário desativado com sucesso!"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ativos")
    public ResponseEntity<List<UsuarioDto>> listarUsuariosAtivos(@RequestParam Long orgId, Pageable pageable) {
        Page<Usuario> page = usuarioService.listarUsuariosAtivos(orgId, pageable);
        if (!page.hasContent()) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }
        List<UsuarioDto> dtos = page.getContent().stream()
                .map(u -> new UsuarioDto(u))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
