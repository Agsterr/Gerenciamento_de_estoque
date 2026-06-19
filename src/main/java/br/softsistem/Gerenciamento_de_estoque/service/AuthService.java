// src/main/java/br/softsistem/Gerenciamento_de_estoque/service/AuthService.java
package br.softsistem.Gerenciamento_de_estoque.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.softsistem.Gerenciamento_de_estoque.config.SecurityUtils;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginRequestDto;
import br.softsistem.Gerenciamento_de_estoque.dto.login.LoginResponseDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioRequestDto;
import br.softsistem.Gerenciamento_de_estoque.exception.UsuarioDesativadoException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;

@Service
public class AuthService {

    /** Mesma mensagem para usuário inexistente e senha errada (evita enumeração de contas) e evita HTTP 404 no login. */
    private static final String LOGIN_FAILED_MESSAGE = "Usuário ou senha incorretos.";

    private final UsuarioRepository usuarioRepository;
    private final OrgRepository orgRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final TrialSubscriptionService trialSubscriptionService;
    private final SubscriptionService subscriptionService;
    private final LoginAuditoriaService loginAuditoriaService;

    public AuthService(UsuarioRepository usuarioRepository,
                       OrgRepository orgRepository,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       RoleRepository roleRepository,
                       TrialSubscriptionService trialSubscriptionService,
                       SubscriptionService subscriptionService,
                       LoginAuditoriaService loginAuditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.orgRepository = orgRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
        this.trialSubscriptionService = trialSubscriptionService;
        this.subscriptionService = subscriptionService;
        this.loginAuditoriaService = loginAuditoriaService;
    }

    @Transactional
    public LoginResponseDto login(LoginRequestDto request, HttpServletRequest httpRequest) {
        String loginId = normalizeLoginIdentifier(request.username());
        String rawPassword = request.senha();

        Usuario usuario = authenticateByLoginIdAndPassword(loginId, rawPassword);

        if (!usuario.getAtivo()) {
            throw new UsuarioDesativadoException("Usuário foi desativado");
        }

        Long userId = usuario.getId();
        Long orgId  = usuario.getOrg() != null ? usuario.getOrg().getId() : null;
        List<String> roleNames = usuario.getRoles().stream()
                .map(Role::getNome)
                .toList();

        String token = jwtService.generateToken(
                (UserDetails) usuario,
                userId,
                orgId,
                roleNames
        );

        loginAuditoriaService.registrarSucesso(usuario, httpRequest);

        return new LoginResponseDto(token);
    }

    private String normalizeLoginIdentifier(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.contains("@")) {
            return trimmed.toLowerCase(Locale.ROOT);
        }
        return trimmed;
    }

    /**
     * Localiza o usuário e valida a senha. Com várias organizações, o mesmo {@code username} pode existir
     * mais de uma vez: nesse caso a senha define qual conta é a correta.
     */
    private Usuario authenticateByLoginIdAndPassword(String loginId, String rawPassword) {
        if (loginId.isEmpty() || rawPassword == null) {
            throw new BadCredentialsException(LOGIN_FAILED_MESSAGE);
        }

        if (loginId.contains("@")) {
            List<Usuario> candidates = usuarioRepository.findAllByEmailIgnoreCase(loginId);
            if (candidates.isEmpty()) {
                candidates = usuarioRepository.findAllByEmail(loginId);
            }
            return pickUserMatchingPassword(candidates, rawPassword);
        }

        List<Usuario> byUsername = usuarioRepository.findAllByUsernameIgnoreCase(loginId);
        if (byUsername.isEmpty()) {
            Optional<Usuario> exact = usuarioRepository.findByUsername(loginId);
            byUsername = exact.map(List::of).orElseGet(ArrayList::new);
        }
        if (!byUsername.isEmpty()) {
            return pickUserMatchingPassword(byUsername, rawPassword);
        }

        List<Usuario> byEmail = usuarioRepository.findAllByEmailIgnoreCase(loginId);
        if (byEmail.isEmpty()) {
            byEmail = usuarioRepository.findAllByEmail(loginId);
        }
        if (!byEmail.isEmpty()) {
            return pickUserMatchingPassword(byEmail, rawPassword);
        }

        throw new BadCredentialsException(LOGIN_FAILED_MESSAGE);
    }

    private Usuario pickUserMatchingPassword(List<Usuario> candidates, String rawPassword) {
        if (candidates.isEmpty()) {
            throw new BadCredentialsException(LOGIN_FAILED_MESSAGE);
        }
        if (candidates.size() == 1) {
            Usuario u = candidates.get(0);
            if (!passwordMatches(rawPassword, u)) {
                throw new BadCredentialsException(LOGIN_FAILED_MESSAGE);
            }
            return u;
        }
        for (Usuario u : candidates) {
            if (passwordMatches(rawPassword, u)) {
                return u;
            }
        }
        throw new BadCredentialsException(LOGIN_FAILED_MESSAGE);
    }

    /**
     * Compara com BCrypt. Se o banco ainda tiver senha em texto plano (legado), valida e regrava com BCrypt.
     */
    private boolean passwordMatches(String rawPassword, Usuario usuario) {
        if (rawPassword == null) {
            return false;
        }
        String stored = usuario.getSenha();
        if (stored == null) {
            return false;
        }
        if (passwordEncoder.matches(rawPassword, stored)) {
            return true;
        }
        if (!stored.startsWith("$2")) {
            if (rawPassword.equals(stored)) {
                usuario.setSenha(passwordEncoder.encode(rawPassword));
                usuarioRepository.save(usuario);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public UsuarioDto register(UsuarioRequestDto dto) {
        String emailNormalizado = dto.email() != null ? dto.email().trim().toLowerCase(Locale.ROOT) : "";
        if (!emailNormalizado.isEmpty()) {
            usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                    .ifPresent(existing -> {
                        throw new DuplicateKeyException(
                                "Este e-mail já está cadastrado. Faça login com esse e-mail e a senha dessa conta, "
                                        + "ou cadastre-se com outro e-mail. "
                                        + "(O nome de usuário que você escolheu só vale após o cadastro ser concluído com sucesso.)");
                    });
        }

        Org org = resolveOrgForRegister(dto);
        boolean newOrganization = dto.orgNome() != null && !dto.orgNome().trim().isEmpty();
        validateUserLimitForRegister(org, dto.orgId() != null);
        List<Role> roles = resolveRolesForRegister(dto, org, newOrganization);

        Usuario u = new Usuario();
        u.setUsername(dto.username() != null ? dto.username().trim() : null);
        u.setSenha(encodeRawPassword(dto.senha()));
        u.setEmail(emailNormalizado);
        u.setOrg(org);
        u.setRoles(roles);
        u.setAtivo(true);

        final Usuario saved;
        try {
            saved = usuarioRepository.save(u);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateKeyException(
                    "Este e-mail já está cadastrado. Use outro e-mail ou faça login com a conta existente.", e);
        }

        trialSubscriptionService.startTrialForUser(saved);

        return new UsuarioDto(saved);
    }

    private void validateUserLimitForRegister(Org org, boolean existingOrg) {
        if (!existingOrg) {
            return;
        }
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return;
        }
        int currentCount = (int) usuarioRepository.countAtivosByOrgId(org.getId());
        if (!subscriptionService.isWithinLimits(userId, "users", currentCount)) {
            throw new IllegalStateException(
                    "Limite de usuários do seu plano foi atingido. Faça upgrade da assinatura para adicionar mais usuários.");
        }
    }

    private Org resolveOrgForRegister(UsuarioRequestDto dto) {
        boolean hasOrgId = dto.orgId() != null;
        boolean hasOrgNome = dto.orgNome() != null && !dto.orgNome().trim().isEmpty();

        if (hasOrgId && hasOrgNome) {
            throw new IllegalArgumentException("Informe apenas orgId (organização existente) ou orgNome (nova organização), não ambos.");
        }
        if (!hasOrgId && !hasOrgNome) {
            throw new IllegalArgumentException("É necessário fornecer 'orgNome' para criar nova organização ou 'orgId' para vincular a uma existente.");
        }

        if (hasOrgId) {
            return orgRepository.findById(dto.orgId())
                    .orElseThrow(() -> new IllegalArgumentException("Organização não encontrada para o orgId informado."));
        }

        String orgNome = dto.orgNome().trim();
        orgRepository.findByNome(orgNome)
                .ifPresent(existingOrg -> {
                    throw new IllegalStateException(
                            "Já existe uma organização com o nome \"" + orgNome
                                    + "\". Escolha outro nome de empresa/organização ou peça acesso à existente.");
                });

        Org org = orgRepository.save(new Org(orgNome));
        ensureDefaultRolesForOrg(org);
        return org;
    }

    private void ensureDefaultRolesForOrg(Org org) {
        roleRepository.findByNomeAndOrgId("ROLE_USER", org.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", org)));
        roleRepository.findByNomeAndOrgId("ROLE_ADMIN", org.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", org)));
    }

    private List<Role> resolveRolesForRegister(UsuarioRequestDto dto, Org org, boolean newOrganization) {
        if (dto.roles() != null && !dto.roles().isEmpty()) {
            return dto.roles().stream()
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .map(name -> roleRepository.findByNomeAndOrgId(name, org.getId())
                            .orElseGet(() -> roleRepository.save(new Role(name, org))))
                    .toList();
        }
        String defaultRoleName = newOrganization ? "ROLE_ADMIN" : "ROLE_USER";
        Role defaultRole = roleRepository.findByNomeAndOrgId(defaultRoleName, org.getId())
                .orElseGet(() -> roleRepository.save(new Role(defaultRoleName, org)));
        return List.of(defaultRole);
    }

    /** Evita gravar BCrypt em cima de hash já persistido (ex.: reenvio do formulário). */
    private String encodeRawPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("A senha não pode ser vazia.");
        }
        if (rawPassword.startsWith("$2a$") || rawPassword.startsWith("$2b$") || rawPassword.startsWith("$2y$")) {
            return rawPassword;
        }
        return passwordEncoder.encode(rawPassword);
    }
}
