package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.exception.UsuarioDesativadoException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AuthService: concentre aqui toda a lógica de login e registro.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final OrgRepository orgRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public AuthService(UsuarioRepository usuarioRepository,
                       OrgRepository orgRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       RoleRepository roleRepository) {
        this.usuarioRepository = usuarioRepository;
        this.orgRepository = orgRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    /**
     * Executa o login para um usuário em uma organização específica.
     * @param request contém username, senha e orgId (multitenant).
     * @return DTO contendo token JWT com userId + orgId embutidos.
     */
    @Transactional(readOnly = true)
    public LoginResponseDto login(LoginRequestDto request) {
        // 1) Buscar o usuário pelo username e orgId
        Usuario usuario = usuarioRepository
                .findByUsernameAndOrgId(request.username(), request.orgId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado ou não pertence à organização"));

        // 2) Verificar se o usuário está ativo
        if (!usuario.getAtivo()) {
            throw new UsuarioDesativadoException("Usuário foi desativado");
        }

        // 3) Verificar senha
        if (!passwordEncoder.matches(request.senha(), usuario.getSenha())) {
            throw new RuntimeException("Senha incorreta");
        }

        // 4) Extrair userId e orgId (já validado pela consulta acima)
        Long userId = usuario.getId();
        Long orgId  = request.orgId();

        // 5) Gerar token JWT contendo userId + orgId
        String token = jwtService.generateToken((UserDetails) usuario, userId, orgId);

        return new LoginResponseDto(token);
    }

    /**
     * Executa o registro de um novo usuário em uma organização.
     * @param usuarioRequestDto contém username, senha, email, roles e orgId.
     * @return DTO do usuário criado (sem expor senha).
     */
    @Transactional
    public UsuarioDto register(UsuarioRequestDto usuarioRequestDto) {
        Long orgId = usuarioRequestDto.orgId();

        // 1) Buscar a organização por ID usando OrgRepository
        Org orgEntity = orgRepository.findById(orgId)
                .orElseThrow(() -> new RuntimeException("Organização não encontrada"));

        // 2) Processar roles: converter nomes para entidades persistentes
        List<Role> rolesPersistidas = usuarioRequestDto.roles().stream()
                .map(nomeRole -> roleRepository.findByNome(nomeRole)
                        .orElseGet(() -> roleRepository.save(new Role(nomeRole))))
                .toList();

        // 3) Montar entidade Usuario
        Usuario usuario = new Usuario();
        usuario.setUsername(usuarioRequestDto.username());
        usuario.setSenha(passwordEncoder.encode(usuarioRequestDto.senha()));
        usuario.setEmail(usuarioRequestDto.email());
        usuario.setRoles(rolesPersistidas);
        usuario.setAtivo(true);
        usuario.setOrg(orgEntity);  // associa ao objeto de Org correto

        // 4) Salvar no banco
        Usuario salvo = usuarioRepository.save(usuario);

        // 5) Retornar DTO
        return new UsuarioDto(salvo);
    }
}
