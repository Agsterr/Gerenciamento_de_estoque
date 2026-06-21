package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.CreateUsuarioOrgRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.ReativarUsuarioRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioCreatedResponse;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioGestaoDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioLimiteDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioPasswordResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.OrganizacaoNaoEncontradaException;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    private Long requireOrgId() {
        Long orgId = SecurityUtils.getCurrentOrgId();
        if (orgId == null) {
            throw new OrganizacaoNaoEncontradaException("Organização não encontrada no contexto de segurança");
        }
        return orgId;
    }

    @PutMapping("/{id}/ativar")
    public ResponseEntity<MensagemResponse> ativarUsuario(@PathVariable Long id) {
        Long orgId = requireOrgId();
        usuarioService.ativarUsuario(id, orgId);
        return ResponseEntity.ok(new MensagemResponse("Usuário ativado com sucesso!"));
    }

    @PostMapping("/reativar-usuario")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensagemResponse> reativarUsuario(@RequestBody ReativarUsuarioRequest request) {
        Long orgId = requireOrgId();
        usuarioService.reativarUsuario(request.username(), orgId);
        return ResponseEntity.ok(new MensagemResponse("Usuário reativado com sucesso."));
    }

    @PutMapping("/{id}/desativar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MensagemResponse> desativarUsuario(@PathVariable Long id) {
        Long orgId = requireOrgId();
        usuarioService.desativarUsuario(id, orgId);
        return ResponseEntity.ok(new MensagemResponse("Usuário desativado com sucesso!"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/ativos")
    public ResponseEntity<Page<UsuarioGestaoDto>> listarUsuariosAtivos(Pageable pageable) {
        Long orgId = requireOrgId();
        Page<UsuarioGestaoDto> page = usuarioService.listarUsuariosAtivos(orgId, pageable)
                .map(UsuarioGestaoDto::new);
        return ResponseEntity.ok(page);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/limites")
    public ResponseEntity<UsuarioLimiteDto> consultarLimites() {
        Long orgId = requireOrgId();
        Long adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(usuarioService.consultarLimite(orgId, adminUserId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<UsuarioCreatedResponse> criarUsuario(@Valid @RequestBody CreateUsuarioOrgRequest request) {
        Long orgId = requireOrgId();
        Long adminUserId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(usuarioService.criarUsuarioComum(request, orgId, adminUserId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<UsuarioPasswordResponse> resetSenha(@PathVariable Long id) {
        Long orgId = requireOrgId();
        return ResponseEntity.ok(usuarioService.resetSenha(id, orgId));
    }
}
