package br.softsistem.Gerenciamento_de_estoque.exception;

/**
 * Lançada quando a assinatura foi criada no Mercado Pago com sucesso,
 * mas falhou ao persistir/atualizar no banco local (ex.: constraint, conexão).
 * O pagamento foi aceito pelo MP; o usuário não deve reenviar o mesmo token.
 */
public class SubscriptionPersistenceException extends RuntimeException {

    private final String preapprovalId;

    public SubscriptionPersistenceException(String message, String preapprovalId, Throwable cause) {
        super(message, cause);
        this.preapprovalId = preapprovalId;
    }

    public String getPreapprovalId() {
        return preapprovalId;
    }
}
