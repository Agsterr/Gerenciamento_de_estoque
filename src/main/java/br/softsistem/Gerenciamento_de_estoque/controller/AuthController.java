package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.exception.UsuarioDesativadoException;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public AuthController(UsuarioRepository usuarioRepository, JwtService jwtService, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;

    }

    @PostMapping("/login")
    public String login(@Valid @RequestBody LoginRequest request) {
        // Buscar o usuário pelo nome de usuário
        var usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        // Verificar se o usuário está ativo
        if (!usuario.getAtivo()) {
            throw new UsuarioDesativadoException("Usuário foi desativado");
        }

        // Verificar se a senha está correta
        if (!passwordEncoder.matches(request.getSenha(), usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }

        // Gerar o token JWT
        return jwtService.generateToken((UserDetails) usuario);
    }



    @PostMapping("/register")
    public String register(@RequestBody Usuario usuario) {

        // Processar as roles recebidas do payload
        List<Role> rolesPersistidas = usuario.getRoles().stream()
                .map(role -> {
                    // Buscar no banco a role pelo nome
                    return roleRepository.findByNome(role.getNome())
                            .orElseGet(() -> roleRepository.save(new Role(role.getNome())));
                })
                .toList();


        // Associar as roles persistidas ao usuário
        usuario.setRoles(rolesPersistidas);

        usuario.setUsername(usuario.getUsername());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setAtivo(true);


        usuarioRepository.save(usuario);
        return "Usuário registrado com sucesso!";
    }
}

class LoginRequest {
    @NotBlank(message = "O username não pode ser vazio.")
    private String username;

    @NotBlank(message = "A senha não pode ser vazia.")
    private String senha;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LoginRequest that = (LoginRequest) o;
        return Objects.equals(username, that.username) && Objects.equals(senha, that.senha);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, senha);
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "username='" + username + '\'' +
                ", password='" + senha + '\'' +
                '}';
    }
}

