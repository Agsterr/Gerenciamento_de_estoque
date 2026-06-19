package br.softsistem.Gerenciamento_de_estoque.enumeracao;

/**
 * Modo de pagamento Asaas no checkout SaaS.
 */
public enum AsaasPaymentMode {
    /** Assinatura recorrente mensal no Asaas (cartão, Pix ou boleto na página do Asaas). */
    RECURRING,
    /** Cobrança avulsa mensal via Pix (renovação manual a cada ciclo). */
    PIX,
    /** Cobrança avulsa mensal via boleto (renovação manual a cada ciclo). */
    BOLETO;

    public static AsaasPaymentMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return RECURRING;
        }
        String normalized = value.trim().toUpperCase();
        if ("RECORRENTE".equals(normalized) || "SUBSCRIPTION".equals(normalized)) {
            return RECURRING;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return RECURRING;
        }
    }

    public String getBillingType() {
        return switch (this) {
            case PIX -> "PIX";
            case BOLETO -> "BOLETO";
            case RECURRING -> "UNDEFINED";
        };
    }
}
