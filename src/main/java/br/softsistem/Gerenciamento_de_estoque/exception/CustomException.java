package br.softsistem.Gerenciamento_de_estoque.exception;

public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }

    @Override
    public Throwable fillInStackTrace() {
        // Retorna apenas a mensagem sem a stack trace para melhorar a legibilidade
        return this;
    }
}
