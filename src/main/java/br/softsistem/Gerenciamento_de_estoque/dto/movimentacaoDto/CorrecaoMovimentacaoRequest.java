package br.softsistem.Gerenciamento_de_estoque.dto.movimentacaoDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CorrecaoMovimentacaoRequest(
        @NotNull @Min(0) Integer quantidadeCorreta,
        @NotBlank @Size(max = 500) String motivo
) {}
