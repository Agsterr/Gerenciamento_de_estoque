package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ExtendTrialRequest(
        Integer days,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime trialEnd
) {}
