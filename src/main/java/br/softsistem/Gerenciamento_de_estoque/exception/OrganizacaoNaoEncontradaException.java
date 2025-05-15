package br.softsistem.Gerenciamento_de_estoque.exception;

public class OrganizacaoNaoEncontradaException extends RuntimeException {
    public OrganizacaoNaoEncontradaException(String message) {
        super(message);
    }
}
