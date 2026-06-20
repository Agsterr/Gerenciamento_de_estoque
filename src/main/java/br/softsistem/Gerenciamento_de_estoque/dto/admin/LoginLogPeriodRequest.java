package br.softsistem.Gerenciamento_de_estoque.dto.admin;

import jakarta.validation.constraints.AssertTrue;

public record LoginLogPeriodRequest(
        Integer ano,
        Integer mes,
        Integer dia,
        Long orgId,
        Boolean confirm
) {
    @AssertTrue(message = "Informe ao menos o ano do período.")
    public boolean isPeriodoValido() {
        return ano != null;
    }
}
