package br.softsistem.Gerenciamento_de_estoque.dto.produtoDto;

import br.softsistem.Gerenciamento_de_estoque.model.Produto;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProdutoDto(
        @NotNull(message = "O ID do produto é obrigatório.")
        Long id,

        @NotBlank(message = "O nome do produto é obrigatório.")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres.")
        String nome,

        @NotNull(message = "O preço é obrigatório.")
        @DecimalMin(value = "0.01", message = "O preço deve ser maior que zero.")
        BigDecimal preco,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String descricao,

        @NotNull(message = "O ID da categoria é obrigatório.")
        Long categoriaId,

        @NotNull(message = "O ID da organização é obrigatório.")
        Long orgId,

        @PastOrPresent(message = "A data de criação não pode ser no futuro.")
        LocalDateTime criadoEm,

        @NotNull(message = "A quantidade é obrigatória.")
        @Min(value = 0, message = "A quantidade não pode ser negativa.")
        Integer quantidade,

        @NotNull(message = "A quantidade mínima é obrigatória.")
        @Min(value = 0, message = "A quantidade mínima não pode ser negativa.")
        Integer quantidadeMinima,

        Boolean ativo,

        Boolean estoqueBaixo // ✅ novo campo adicionado
) {
    public ProdutoDto(Produto produto) {
        this(
                produto.getId(),
                produto.getNome(),
                produto.getPreco(),
                produto.getDescricao(),
                produto.getCategoria().getId(),
                produto.getOrg().getId(),
                produto.getCriadoEm(),
                produto.getQuantidade(),
                produto.getQuantidadeMinima(),
                produto.getAtivo(),
                produto.isEstoqueBaixo() // ✅ nova lógica aqui
        );
    }
}
