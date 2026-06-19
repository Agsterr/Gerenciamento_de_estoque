package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminOrgSubscriptionDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.AdminUserSubscriptionDto;
import br.softsistem.Gerenciamento_de_estoque.dto.admin.ExtendTrialRequest;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.exception.ResourceNotFoundException;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AdminSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(AdminSubscriptionService.class);

    private final OrgRepository orgRepository;
    private final UsuarioRepository usuarioRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TrialSubscriptionService trialSubscriptionService;

    public AdminSubscriptionService(OrgRepository orgRepository,
                                    UsuarioRepository usuarioRepository,
                                    SubscriptionRepository subscriptionRepository,
                                    TrialSubscriptionService trialSubscriptionService) {
        this.orgRepository = orgRepository;
        this.usuarioRepository = usuarioRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.trialSubscriptionService = trialSubscriptionService;
    }

    @Transactional(readOnly = true)
    public List<AdminOrgSubscriptionDto> listOverview() {
        List<Org> orgs = orgRepository.findAll();
        List<AdminOrgSubscriptionDto> result = new ArrayList<>();
        for (Org org : orgs) {
            result.add(buildOrgOverview(org));
        }
        result.sort(Comparator.comparing(AdminOrgSubscriptionDto::orgNome, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public AdminUserSubscriptionDto setBypass(Long userId, boolean bypass) {
        Usuario user = findUser(userId);
        user.setBypassSubscription(bypass);
        usuarioRepository.save(user);
        log.info("Admin alterou bypass userId={} bypass={}", userId, bypass);
        return toUserDto(user, findCurrentSubscription(user));
    }

    public AdminUserSubscriptionDto extendTrial(Long userId, ExtendTrialRequest request) {
        validateExtendRequest(request);
        Usuario user = findUser(userId);
        Subscription subscription = resolveSubscriptionForExtend(user);
        LocalDateTime newEnd = computeTrialEnd(subscription, request);
        applyTrialEnd(subscription, newEnd);
        subscriptionRepository.save(subscription);
        log.info("Admin estendeu trial userId={} até {}", userId, newEnd);
        return toUserDto(user, Optional.of(subscription));
    }

    public List<AdminUserSubscriptionDto> extendTrialForOrg(Long orgId, ExtendTrialRequest request) {
        validateExtendRequest(request);
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));
        List<AdminUserSubscriptionDto> updated = new ArrayList<>();
        for (Usuario user : usuarioRepository.findByOrg_Id(org.getId())) {
            if (user.hasSubscriptionBypass()) {
                continue;
            }
            updated.add(extendTrial(user.getId(), request));
        }
        return updated;
    }

    public AdminUserSubscriptionDto forcePay(Long userId) {
        Usuario user = findUser(userId);
        user.setBypassSubscription(false);
        usuarioRepository.save(user);

        Optional<Subscription> subOpt = subscriptionRepository.findActiveSubscriptionByUser(user);
        if (subOpt.isPresent()) {
            Subscription sub = subOpt.get();
            if (sub.getStatus() == SubscriptionStatus.TRIAL) {
                LocalDateTime ended = LocalDateTime.now().minusMinutes(1);
                sub.setTrialEnd(ended);
                sub.setCurrentPeriodEnd(ended);
                subscriptionRepository.save(sub);
            }
        }

        log.info("Admin forçou cobrança userId={}", userId);
        return toUserDto(user, findCurrentSubscription(user));
    }

    public List<AdminUserSubscriptionDto> forcePayForOrg(Long orgId) {
        Org org = orgRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organização não encontrada"));
        List<AdminUserSubscriptionDto> updated = new ArrayList<>();
        for (Usuario user : usuarioRepository.findByOrg_Id(org.getId())) {
            updated.add(forcePay(user.getId()));
        }
        return updated;
    }

    private AdminOrgSubscriptionDto buildOrgOverview(Org org) {
        List<Usuario> users = usuarioRepository.findByOrg_Id(org.getId());
        List<AdminUserSubscriptionDto> userDtos = users.stream()
                .map(u -> toUserDto(u, findCurrentSubscription(u)))
                .sorted(Comparator.comparing(AdminUserSubscriptionDto::username, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int bypassCount = (int) userDtos.stream()
                .filter(u -> Boolean.TRUE.equals(u.bypassSubscription()))
                .count();

        LocalDateTime latestTrialEnd = userDtos.stream()
                .map(AdminUserSubscriptionDto::trialEnd)
                .filter(d -> d != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new AdminOrgSubscriptionDto(
                org.getId(),
                org.getNome(),
                org.getAtivo(),
                users.size(),
                bypassCount,
                summarizeOrgStatus(userDtos, org.getAtivo()),
                latestTrialEnd,
                userDtos
        );
    }

    private String summarizeOrgStatus(List<AdminUserSubscriptionDto> users, Boolean orgAtivo) {
        if (Boolean.FALSE.equals(orgAtivo)) {
            return "ORG_INATIVA";
        }
        if (users.isEmpty()) {
            return "SEM_USUARIOS";
        }
        boolean allBypass = users.stream().allMatch(u -> Boolean.TRUE.equals(u.bypassSubscription()));
        if (allBypass) {
            return "ISENTO";
        }
        boolean anyActive = users.stream().anyMatch(u ->
                Boolean.TRUE.equals(u.inTrial()) || SubscriptionStatus.ACTIVE.equals(u.status()));
        boolean anyBlocked = users.stream().anyMatch(u ->
                !Boolean.TRUE.equals(u.bypassSubscription())
                        && (u.status() == null
                        || (!Boolean.TRUE.equals(u.inTrial()) && !SubscriptionStatus.ACTIVE.equals(u.status()))));
        if (anyActive && anyBlocked) {
            return "MISTO";
        }
        if (anyActive) {
            return users.stream().anyMatch(u -> Boolean.TRUE.equals(u.inTrial())) ? "TRIAL" : "ATIVO";
        }
        return "BLOQUEADO";
    }

    private Subscription resolveSubscriptionForExtend(Usuario user) {
        Optional<Subscription> active = subscriptionRepository.findActiveSubscriptionByUser(user);
        if (active.isPresent()) {
            Subscription sub = active.get();
            if (sub.getStatus() == SubscriptionStatus.ACTIVE) {
                throw new IllegalStateException("Usuário já possui assinatura paga ativa.");
            }
            return sub;
        }
        return trialSubscriptionService.startTrialForUser(user);
    }

    private LocalDateTime computeTrialEnd(Subscription subscription, ExtendTrialRequest request) {
        if (request.trialEnd() != null) {
            return request.trialEnd();
        }
        LocalDateTime base = subscription.getTrialEnd();
        if (base == null || !base.isAfter(LocalDateTime.now())) {
            base = LocalDateTime.now();
        }
        return base.plusDays(request.days());
    }

    private void applyTrialEnd(Subscription subscription, LocalDateTime newEnd) {
        subscription.setStatus(SubscriptionStatus.TRIAL);
        if (subscription.getTrialStart() == null) {
            subscription.setTrialStart(LocalDateTime.now());
        }
        subscription.setTrialEnd(newEnd);
        subscription.setCurrentPeriodStart(subscription.getTrialStart());
        subscription.setCurrentPeriodEnd(newEnd);
        subscription.setAccessBlocked(false);
        subscription.setTrialWarningSent(false);
    }

    private void validateExtendRequest(ExtendTrialRequest request) {
        if (request == null || (request.days() == null && request.trialEnd() == null)) {
            throw new IllegalArgumentException("Informe days ou trialEnd.");
        }
        if (request.days() != null && request.days() <= 0) {
            throw new IllegalArgumentException("days deve ser maior que zero.");
        }
    }

    private Optional<Subscription> findCurrentSubscription(Usuario user) {
        return subscriptionRepository.findActiveSubscriptionByUser(user)
                .or(() -> subscriptionRepository.findByUserId(user.getId()));
    }

    private AdminUserSubscriptionDto toUserDto(Usuario user, Optional<Subscription> subOpt) {
        Subscription sub = subOpt.orElse(null);
        SubscriptionStatus status = sub != null ? sub.getStatus() : null;
        LocalDateTime trialEnd = sub != null ? sub.getTrialEnd() : null;
        boolean inTrial = sub != null && sub.isInTrial();
        return new AdminUserSubscriptionDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAtivo(),
                user.getBypassSubscription(),
                sub != null ? sub.getId() : null,
                status,
                trialEnd,
                sub != null ? sub.getAccessBlocked() : null,
                inTrial,
                sub != null ? sub.getPaymentMode() : null
        );
    }

    private Usuario findUser(Long userId) {
        return usuarioRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}
