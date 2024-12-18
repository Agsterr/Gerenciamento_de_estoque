package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.UsuarioRequestDto;
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
    public UsuarioDto register(@RequestBody @Valid UsuarioRequestDto usuarioRequestDto) {

        // Processar as roles recebidas no payload (convertendo de nomes para entidades persistidas)
        List<Role> rolesPersistidas = usuarioRequestDto.roles().stream()
                .map(nomeRole -> roleRepository.findByNome(nomeRole)
                        .orElseGet(() -> roleRepository.save(new Role(nomeRole))))
                .toList();

        // Criar o objeto Usuario e preencher os dados
        Usuario usuario = new Usuario();
        usuario.setUsername(usuarioRequestDto.username());
        usuario.setSenha(passwordEncoder.encode(usuarioRequestDto.senha())); // Criptografar a senha
        usuario.setEmail(usuarioRequestDto.email());
        usuario.setRoles(rolesPersistidas);
        usuario.setAtivo(true);

        // Salvar o usuário no banco
        usuario = usuarioRepository.save(usuario);

        // Retornar o DTO com os dados do usuário salvo
        return new UsuarioDto(usuario);
    }
}

