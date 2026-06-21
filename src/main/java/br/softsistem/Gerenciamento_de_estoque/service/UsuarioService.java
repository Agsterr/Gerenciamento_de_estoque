package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.CreateUsuarioOrgRequest;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioCreatedResponse;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioLimiteDto;
import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioPasswordResponse;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Role;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
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
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class UsuarioService {

    private static final String PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final OrgRepository orgRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TrialSubscriptionService trialSubscriptionService;
    private final OrgUserLimitService orgUserLimitService;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          OrgRepository orgRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          TrialSubscriptionService trialSubscriptionService,
                          OrgUserLimitService orgUserLimitService) {
        this.usuarioRepository = usuarioRepository;
        this.orgRepository = orgRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.trialSubscriptionService = trialSubscriptionService;
        this.orgUserLimitService = orgUserLimitService;
    }

    @Transactional
    public void ativarUsuario(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            orgUserLimitService.assertCanAddUser(usuario.getOrg(), null);
        }
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reativarUsuario(String username, Long orgId) {
        Usuario usuario = usuarioRepository.findByUsernameAndOrgId(username, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));

        if (!usuario.getAtivo()) {
            orgUserLimitService.assertCanAddUser(usuario.getOrg(), null);
            usuario.setAtivo(true);
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void desativarUsuario(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listarUsuariosAtivos(Long orgId, Pageable pageable) {
        return usuarioRepository.findByAtivoTrueAndOrgId(orgId, pageable);
    }

    @Transactional
    public UsuarioCreatedResponse criarUsuarioComum(CreateUsuarioOrgRequest request, Long orgId, Long adminUserId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));
        if (Boolean.TRUE.equals(org.getEphemeral())) {
            throw new IllegalArgumentException("Não é permitido criar usuários na organização demo.");
        }

        orgUserLimitService.assertCanAddUser(org, adminUserId);

        String email = request.email() != null && !request.email().isBlank()
                ? request.email().trim().toLowerCase(Locale.ROOT)
                : request.username().trim().toLowerCase(Locale.ROOT) + "@empresa.local";

        usuarioRepository.findByEmailIgnoreCase(email).ifPresent(u -> {
            throw new DuplicateKeyException("E-mail já cadastrado: " + email);
        });

        Role userRole = roleRepository.findByNomeAndOrgId("ROLE_USER", orgId)
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER", org)));

        String tempPassword = generateTemporaryPassword();
        Usuario user = new Usuario();
        user.setUsername(request.username().trim());
        user.setEmail(email);
        user.setSenha(passwordEncoder.encode(tempPassword));
        user.setOrg(org);
        user.setRoles(List.of(userRole));
        user.setAtivo(true);
        user.setBypassSubscription(false);

        Usuario saved;
        try {
            saved = usuarioRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateKeyException("Usuário ou e-mail já existente.", e);
        }

        trialSubscriptionService.startTrialForUser(saved);
        return new UsuarioCreatedResponse(new UsuarioDto(saved), tempPassword);
    }

    @Transactional
    public UsuarioPasswordResponse resetSenha(Long id, Long orgId) {
        Usuario usuario = usuarioRepository.findById(id)
                .filter(u -> u.getOrg().getId().equals(orgId))
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado ou não pertence à organização"));
        if (usuario.isSuperAdmin()) {
            throw new IllegalArgumentException("Não é permitido resetar senha de SUPER_ADMIN.");
        }
        String tempPassword = generateTemporaryPassword();
        usuario.setSenha(passwordEncoder.encode(tempPassword));
        usuarioRepository.save(usuario);
        return new UsuarioPasswordResponse(usuario.getUsername(), tempPassword);
    }

    @Transactional(readOnly = true)
    public UsuarioLimiteDto consultarLimite(Long orgId, Long adminUserId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));
        return orgUserLimitService.consultarLimite(org, adminUserId);
    }

    public Optional<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    private String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }
}
