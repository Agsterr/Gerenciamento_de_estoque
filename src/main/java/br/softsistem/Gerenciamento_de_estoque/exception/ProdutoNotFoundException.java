package br.softsistem.Gerenciamento_de_estoque.exception;


    public class ProdutoNotFoundException extends RuntimeException {
        public ProdutoNotFoundException(String message) {
            super(message);
        }
    }


