package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.exception.UsuarioDesativadoException;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.service.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        // Buscar o usuário pelo nome de usuário e pela organização
        var usuario = usuarioRepository.findByUsernameAndOrgId(request.username(), request.orgId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à organização"));

        // Verificar se o usuário está ativo
        if (!usuario.getAtivo()) {
            throw new UsuarioDesativadoException("Usuário foi desativado");
        }

        // Verificar se a senha está correta
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }

        // Gerar o token JWT
        String token = jwtService.generateToken((UserDetails) usuario, request.orgId());  // Passa o orgId para o JWT

        // Retornar o token em um objeto JSON
        LoginResponseDto responseDto = new LoginResponseDto(token);
        return ResponseEntity.ok(responseDto);  // Retornando a resposta como um JSON
    }

    @PostMapping("/register")
    public UsuarioDto register(@RequestBody @Valid UsuarioRequestDto usuarioRequestDto) {
        // Recupera o orgId do DTO
        Long orgId = usuarioRequestDto.orgId();

        // Verificar se a organização com o orgId existe
        var org = usuarioRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));

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
        usuario.setOrg(org.getOrg());  // Associa o usuário à organização

        // Salvar o usuário no banco
        usuario = usuarioRepository.save(usuario);

        // Retornar o DTO com os dados do usuário salvo
        return new UsuarioDto(usuario);
    }
}
