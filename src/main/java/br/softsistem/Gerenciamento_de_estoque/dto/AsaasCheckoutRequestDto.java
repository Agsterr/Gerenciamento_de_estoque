package br.softsistem.Gerenciamento_de_estoque.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para checkout de assinatura via Asaas (link de pagamento).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AsaasCheckoutRequestDto {

    @NotBlank(message = "planId é obrigatório")
    private String planId;

    /** CPF ou CNPJ do pagador (obrigatório em produção; no sandbox usa valor padrão se omitido). */
    private String cpfCnpj;

    /**
     * Modo de pagamento: RECURRING (padrão), PIX ou BOLETO.
     * RECURRING cria assinatura recorrente no Asaas; PIX/BOLETO geram cobrança avulsa mensal.
     */
    private String paymentMode;
}
