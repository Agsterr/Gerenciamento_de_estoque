package br.softsistem.Gerenciamento_de_estoque.dto.erp;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PesquisaPrecoRequest(
        @NotNull @DecimalMin("0.0") BigDecimal valorMin,
        @NotNull @DecimalMin("0.0") BigDecimal valorMax,
        String comentario
) {}
