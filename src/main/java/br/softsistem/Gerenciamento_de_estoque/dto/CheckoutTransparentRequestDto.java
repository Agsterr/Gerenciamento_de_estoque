package br.softsistem.Gerenciamento_de_estoque.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para Checkout Transparente (MPCP – Mercado Pago Checkout API).
 * O frontend envia apenas dados seguros: card_token_id (tokenizado), e-mail do pagador e external_reference.
 * Nenhum dado sensível de cartão é enviado ao backend (conforme PCI).
 *
 * @see <a href="https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/cardtoken">Card Token - Assinaturas</a>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutTransparentRequestDto {

    /**
     * ID do plano (local ou preapproval_plan_id do Mercado Pago).
     */
    @NotBlank(message = "planId é obrigatório")
    private String planId;

    /**
     * Token do cartão gerado pelo SDK JS do Mercado Pago (card_token_id).
     * Uso único; nunca enviar número do cartão, CVV ou dados sensíveis.
     */
    @NotBlank(message = "card_token_id é obrigatório no Checkout Transparente")
    private String cardTokenId;

    /**
     * E-mail do pagador (obrigatório na API de assinaturas do Mercado Pago).
     */
    @NotBlank(message = "E-mail do pagador é obrigatório")
    @Email(message = "E-mail do pagador inválido")
    private String payerEmail;

    /**
     * Referência externa obrigatória para vincular a assinatura ao usuário no sistema.
     * Deve corresponder ao ID do usuário logado (validado no backend).
     */
    @NotBlank(message = "external_reference é obrigatório")
    private String externalReference;
}
