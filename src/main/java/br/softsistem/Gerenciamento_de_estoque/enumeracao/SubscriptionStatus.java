package br.softsistem.Gerenciamento_de_estoque.enumeracao;

/**
 * Enum que define os possíveis status de uma assinatura
 */
public enum SubscriptionStatus {
    /**
     * Período de teste gratuito ativo
     */
    TRIAL,
    
    /**
     * Assinatura ativa com pagamento em dia
     */
    ACTIVE,
    
    /**
     * Assinatura cancelada pelo usuário
     */
    CANCELED,
    
    /**
     * Assinatura suspensa por falta de pagamento
     */
    PAST_DUE,
    
    /**
     * Assinatura expirada
     */
    EXPIRED,
    
    /**
     * Assinatura incompleta (aguardando confirmação de pagamento)
     */
    INCOMPLETE,
    
    /**
     * Assinatura incompleta expirada
     */
    INCOMPLETE_EXPIRED
}