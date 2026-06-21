package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.dto.usuarioDto.UsuarioLimiteDto;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
public class OrgUserLimitService {

    private final UsuarioRepository usuarioRepository;
    private final SubscriptionService subscriptionService;

    public OrgUserLimitService(UsuarioRepository usuarioRepository, SubscriptionService subscriptionService) {
        this.usuarioRepository = usuarioRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional(readOnly = true)
    public UsuarioLimiteDto consultarLimite(Org org, Long userIdForPlanLookup) {
        long ativos = usuarioRepository.countAtivosByOrgId(org.getId());
        Optional<Integer> max = effectiveMaxUsers(org, userIdForPlanLookup);
        return new UsuarioLimiteDto(ativos, max.orElse(null), max.isEmpty(), describeOrigem(org, userIdForPlanLookup));
    }

    public void assertCanAddUser(Org org, Long userIdForPlanLookup) {
        long ativos = usuarioRepository.countAtivosByOrgId(org.getId());

        Integer orgMax = positiveOrNull(org.getMaxDispositivos());
        if (orgMax != null && ativos >= orgMax) {
            throw new IllegalStateException(
                    "Limite de aparelhos/usuários da organização atingido (" + orgMax + " ativos). Contate o suporte para aumentar.");
        }

        if (subscriptionService.hasSubscriptionBypass(userIdForPlanLookup)) {
            return;
        }

        Integer planMax = resolvePlanMaxUsers(userIdForPlanLookup, org.getId());
        if (planMax != null && ativos >= planMax) {
            throw new IllegalStateException(
                    "Limite de usuários do plano atingido (" + planMax + "). Faça upgrade da assinatura.");
        }
    }

    Optional<Integer> effectiveMaxUsers(Org org, Long userIdForPlanLookup) {
        Integer orgMax = positiveOrNull(org.getMaxDispositivos());
        Integer planMax = subscriptionService.hasSubscriptionBypass(userIdForPlanLookup)
                ? null
                : positiveOrNull(resolvePlanMaxUsers(userIdForPlanLookup, org.getId()));

        if (orgMax != null && planMax != null) {
            return Optional.of(Math.min(orgMax, planMax));
        }
        if (orgMax != null) {
            return Optional.of(orgMax);
        }
        if (planMax != null) {
            return Optional.of(planMax);
        }
        return Optional.empty();
    }

    private Integer resolvePlanMaxUsers(Long userIdForPlanLookup, Long orgId) {
        if (userIdForPlanLookup != null) {
            return subscriptionService.getCurrentSubscription(userIdForPlanLookup)
                    .map(Subscription::getPlan)
                    .map(Plan::getMaxUsers)
                    .orElse(null);
        }
        return usuarioRepository.findByOrg_Id(orgId).stream()
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .map(Usuario::getId)
                .map(subscriptionService::getCurrentSubscription)
                .flatMap(Optional::stream)
                .map(Subscription::getPlan)
                .filter(Objects::nonNull)
                .map(Plan::getMaxUsers)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String describeOrigem(Org org, Long userIdForPlanLookup) {
        Integer orgMax = positiveOrNull(org.getMaxDispositivos());
        Integer planMax = subscriptionService.hasSubscriptionBypass(userIdForPlanLookup)
                ? null
                : positiveOrNull(resolvePlanMaxUsers(userIdForPlanLookup, org.getId()));
        if (orgMax != null && planMax != null) {
            return "DISPOSITIVOS_E_PLANO";
        }
        if (orgMax != null) {
            return "DISPOSITIVOS";
        }
        if (planMax != null) {
            return "PLANO";
        }
        return "ILIMITADO";
    }

    private Integer positiveOrNull(Integer value) {
        return value != null && value > 0 ? value : null;
    }
}
