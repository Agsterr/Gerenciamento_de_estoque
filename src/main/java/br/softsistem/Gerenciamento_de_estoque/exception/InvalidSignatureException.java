package br.softsistem.Gerenciamento_de_estoque.exception;

/**
 * Exceção específica para erros de verificação de assinatura de webhooks
 */
public class InvalidSignatureException extends RuntimeException {
    
    private final String signature;
    
    public InvalidSignatureException(String message) {
        super(message);
        this.signature = null;
    }
    
    public InvalidSignatureException(String message, String signature) {
        super(message);
        this.signature = signature;
    }
    
    public InvalidSignatureException(String message, Throwable cause) {
        super(message, cause);
        this.signature = null;
    }
    
    public InvalidSignatureException(String message, String signature, Throwable cause) {
        super(message, cause);
        this.signature = signature;
    }
    
    public String getSignature() {
        return signature;
    }
}