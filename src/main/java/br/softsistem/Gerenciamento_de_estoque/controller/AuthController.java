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

    public AuthController(UsuarioRepository usuarioRepository,
                          JwtService jwtService,
                          PasswordEncoder passwordEncoder,
                          RoleRepository roleRepository) {
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    /**
     * Faz login: valida credenciais, verifica status e gera JWT que carrega
     * tanto o userId quanto o orgId. No payload do front basta enviar username,
     * senha e orgId; o token cuida do resto.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        // 1) Buscar o usuário pelo username e organização
        Usuario usuario = usuarioRepository
                .findByUsernameAndOrgId(request.username(), request.orgId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à organização"));

        // 2) Verificar se o usuário está ativo
        if (!usuario.getAtivo()) {
            throw new UsuarioDesativadoException("Usuário foi desativado");
        }

        // 3) Validar senha
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }

        // 4) Gerar token JWT incluindo userId e orgId
        Long userId = usuario.getId();
        Long orgId  = request.orgId();  // vem do corpo da requisição
        String token = jwtService.generateToken((UserDetails) usuario, userId, orgId);

        // 5) Retornar token no response
        LoginResponseDto responseDto = new LoginResponseDto(token);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Registra um novo usuário em uma organização. Recebe roles, username,
     * senha e orgId; retorna o DTO do usuário criado (sem expor senha).
     */
    @PostMapping("/register")
    public UsuarioDto register(@RequestBody @Valid UsuarioRequestDto usuarioRequestDto) {
        Long orgId = usuarioRequestDto.orgId();

        // Verifica se a organização existe (exemplo: validar se token ou DTO bate com entidade Org)
        var orgEntity = usuarioRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));
        // (A lógica acima assume que você usa a mesma tabela para usuários e organizações;
        // ajuste conforme seu modelo de domínio se for diferente.)

        // Convertendo nomes de role para entidades persistidas
        List<Role> rolesPersistidas = usuarioRequestDto.roles().stream()
                .map(nomeRole -> roleRepository.findByNome(nomeRole)
                        .orElseGet(() -> roleRepository.save(new Role(nomeRole))))
                .toList();

        // Criar e preencher entidade Usuario
        Usuario usuario = new Usuario();
        usuario.setUsername(usuarioRequestDto.username());
        usuario.setSenha(passwordEncoder.encode(usuarioRequestDto.senha()));
        usuario.setEmail(usuarioRequestDto.email());
        usuario.setRoles(rolesPersistidas);
        usuario.setAtivo(true);
        usuario.setOrg(orgEntity.getOrg());  // associa ao objeto de Org (ajuste se sua entidade for diferente)

        // Salvar no banco e retornar DTO
        usuario = usuarioRepository.save(usuario);
        return new UsuarioDto(usuario);
    }
}
