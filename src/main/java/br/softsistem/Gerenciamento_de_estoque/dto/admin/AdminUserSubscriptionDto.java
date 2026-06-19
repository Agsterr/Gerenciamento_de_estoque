package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record AdminUserSubscriptionDto(
        Long userId,
        String username,
        String email,
        Boolean ativo,
        Boolean bypassSubscription,
        Long subscriptionId,
        SubscriptionStatus status,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime trialEnd,
        Boolean accessBlocked,
        Boolean inTrial,
        String paymentMode
) {}
