package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminCreateUserRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminOrgDeviceLimitRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminOrgSummaryDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminUserDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminUserPasswordResponse;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.OrgDto;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.StatusDispositivo;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.DispositivoUsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.RoleRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AdminUserService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final OrgRepository orgRepository;
    private final RoleRepository roleRepository;
    private final DispositivoUsuarioRepository dispositivoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TrialSubscriptionService trialSubscriptionService;

    public AdminUserService(UsuarioRepository usuarioRepository,
                            OrgRepository orgRepository,
                            RoleRepository roleRepository,
                            DispositivoUsuarioRepository dispositivoUsuarioRepository,
                            PasswordEncoder passwordEncoder,
                            TrialSubscriptionService trialSubscriptionService) {
        this.usuarioRepository = usuarioRepository;
        this.orgRepository = orgRepository;
        this.roleRepository = roleRepository;
        this.dispositivoUsuarioRepository = dispositivoUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.trialSubscriptionService = trialSubscriptionService;
    }

    @Transactional(readOnly = true)
    public Page<AdminUserDto> listarUsuarios(Long orgId, Pageable pageable) {
        Page<Usuario> page = orgId != null
                ? usuarioRepository.findByOrg_Id(orgId, pageable)
                : usuarioRepository.findAll(pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public List<AdminOrgSummaryDto> listarOrgsResumo() {
        return orgRepository.findAll().stream()
                .map(this::toOrgSummary)
                .toList();
    }

    @Transactional
    public AdminUserPasswordResponse criarUsuario(AdminCreateUserRequest request) {
        Org org = orgRepository.findById(request.orgId())
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));

        if (Boolean.TRUE.equals(org.getEphemeral())) {
            throw new IllegalArgumentException("Não é permitido criar usuários permanentes na org demo.");
        }

        String email = request.email() != null && !request.email().isBlank()
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : request.username().trim().toLowerCase(Locale.ROOT) + "@admin-created.local";

        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            throw new DuplicateKeyException("E-mail já cadastrado: " + email);
        });

        ensureDefaultRoles(org);
        List<Role> roles = resolveRoles(request.roles(), org);

        String tempPassword = generateTemporaryPassword();
        Usuario user = new Usuario();
        user.setUsername(request.username().trim());
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(tempPassword));
        user.setOrg(org);
        user.setRoles(roles);
        user.setAtivo(true);
        user.setBypassSubscription(Boolean.TRUE.equals(request.bypassSubscription()));

        Usuario saved;
        try {
            saved = usuarioRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateKeyException("Usuário ou e-mail já existente.", e);
        }

        if (!Boolean.TRUE.equals(request.bypassSubscription())) {
            trialSubscriptionService.startTrialForUser(saved);
        }
        return new AdminUserPasswordResponse(toDto(saved), tempPassword);
    }

    @Transactional
    public AdminUserDto setAtivo(Long userId, boolean ativo) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (user.isSuperAdmin()) {
            throw new IllegalArgumentException("Não é permitido desativar SUPER_ADMIN.");
        }
        user.setAtivo(ativo);
        return toDto(usuarioRepository.save(user));
    }

    @Transactional
    public AdminUserPasswordResponse resetSenha(Long userId) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        String tempPassword = generateTemporaryPassword();
        user.setSenha(passwordEncoder.encode(tempPassword));
        return new AdminUserPasswordResponse(toDto(usuarioRepository.save(user)), tempPassword);
    }

    @Transactional
    public AdminUserDto setBypass(Long userId, boolean bypass) {
        Usuario user = usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (user.isSuperAdmin()) {
            throw new IllegalArgumentException("SUPER_ADMIN já possui bypass implícito.");
        }
        user.setBypassSubscription(bypass);
        return toDto(usuarioRepository.save(user));
    }

    @Transactional
    public OrgDto atualizarLimiteDispositivos(Long orgId, AdminOrgDeviceLimitRequest request) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));
        org.setMaxDispositivos(request.maxDispositivos());
        return new OrgDto(orgRepository.save(org));
    }

    private AdminUserDto toDto(Usuario user) {
        List<String> roleNames = user.getRoles() != null
                ? user.getRoles().stream().map(Role::getNome).toList()
                : List.of();
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAtivo(),
                user.getOrg().getId(),
                user.getOrg().getNome(),
                user.getBypassSubscription(),
                roleNames
        );
    }

    private AdminOrgSummaryDto toOrgSummary(Org org) {
        long users = usuarioRepository.findByOrg_Id(org.getId()).size();
        long devices = dispositivoUsuarioRepository.countByOrgIdAndStatus(org.getId(), StatusDispositivo.APPROVED);
        return new AdminOrgSummaryDto(
                org.getId(),
                org.getNome(),
                org.getAtivo(),
                org.getEphemeral(),
                org.getMaxDispositivos(),
                users,
                devices
        );
    }

    private void ensureDefaultRoles(Org org) {
        roleRepository.findByNomeAndOrgId("ROLE_USER", org.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", org)));
        roleRepository.findByNomeAndOrgId("ROLE_ADMIN", org.getId())
                .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN", org)));
    }

    private List<Role> resolveRoles(List<String> roleNames, Org org) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of(roleRepository.findByNomeAndOrgId("ROLE_USER", org.getId())
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", org))));
        }
        List<Role> roles = new ArrayList<>();
        for (String name : roleNames) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            if ("ROLE_SUPER_ADMIN".equals(trimmed)) {
                throw new IllegalArgumentException("ROLE_SUPER_ADMIN não pode ser atribuída via admin de usuários.");
            }
            roles.add(roleRepository.findByNomeAndOrgId(trimmed, org.getId())
                    .orElseGet(() -> roleRepository.save(new Role(trimmed, org))));
        }
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("Informe ao menos uma role válida.");
        }
        return roles;
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
