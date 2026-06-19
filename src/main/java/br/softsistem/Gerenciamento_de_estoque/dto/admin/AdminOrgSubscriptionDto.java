package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record AdminOrgSubscriptionDto(
        Long orgId,
        String orgNome,
        Boolean orgAtivo,
        int totalUsuarios,
        int usuariosComBypass,
        String statusResumo,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime trialEndMaisRecente,
        List<AdminUserSubscriptionDto> usuarios
) {}
