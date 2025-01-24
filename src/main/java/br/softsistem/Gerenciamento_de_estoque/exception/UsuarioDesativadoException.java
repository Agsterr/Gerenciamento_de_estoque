package br.softsistem.Gerenciamento_de_estoque.exception;

public class UsuarioDesativadoException extends RuntimeException {
    public UsuarioDesativadoException(String message) {
        super(message);
    }
}

