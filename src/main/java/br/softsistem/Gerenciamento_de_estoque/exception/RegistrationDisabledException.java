package br.softsistem.Gerenciamento_de_estoque.exception;

public class RegistrationDisabledException extends RuntimeException {
    public RegistrationDisabledException(String message) {
        super(message);
    }
}
