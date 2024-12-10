package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.service.UsuarioService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

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
    @PutMapping("/{id}/desativar")
    public String desativarUsuario(@PathVariable Long id) {
        usuarioService.desativarUsuario(id);
        return "Usuário desativado com sucesso!";
    }

    @GetMapping("/ativos")
    public List<Usuario> listarUsuariosAtivos() {
        return usuarioService.listarUsuariosAtivos();
    }
}



