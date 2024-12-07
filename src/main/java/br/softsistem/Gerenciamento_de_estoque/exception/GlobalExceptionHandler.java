package br.softsistem.Gerenciamento_de_estoque.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        // Personaliza a resposta de erro
        String mensagemPersonalizada = "Ocorreu um erro: " + ex.getMessage();
        return new ResponseEntity<>(mensagemPersonalizada, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> handleCustomException(CustomException ex) {
        return new ResponseEntity<>("Erro personalizado: " + ex.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
