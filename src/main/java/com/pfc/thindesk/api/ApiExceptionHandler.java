package com.pfc.thindesk.api;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@RestControllerAdvice(basePackages = "com.pfc.thindesk.controller")
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<?> negocio(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(Map.of("codigo", ex.getCodigo(), "mensagem", ex.getMessage()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validacao(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage()).sorted()
                .findFirst().orElse("Verifique os campos informados.");
        return ResponseEntity.badRequest().body(Map.of("codigo", "DADOS_INVALIDOS", "mensagem", message));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<?> invalido(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("codigo", "DADOS_INVALIDOS", "mensagem", "Requisição inválida. Confira os campos e o formato JSON."));
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<?> metodo(Exception ex) {
        return ResponseEntity.status(405).body(Map.of("codigo", "METODO_INVALIDO", "mensagem", "Método não permitido."));
    }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> banco(DataAccessException ex) {
        log.error("Falha na consulta ao MongoDB", ex);
        return ResponseEntity.status(503).body(Map.of("codigo", "BANCO_INDISPONIVEL", "mensagem", "Não foi possível acessar os dados. Tente novamente."));
    }
}
