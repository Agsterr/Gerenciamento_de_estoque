package br.softsistem.Gerenciamento_de_estoque.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;

/**
 * Controller que expõe configuração necessária ao frontend para Checkout Transparente.
 * Expõe apenas a Public Key (nunca o Access Token).
 */
@RestController
@RequestMapping("/api/mercadopago")
@ConditionalOnProperty(name = "app.payment.provider", havingValue = "mercadopago")
public class MercadoPagoController {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoController.class);
    
    private final MercadoPagoConfig mercadoPagoConfig;

    public MercadoPagoController(MercadoPagoConfig mercadoPagoConfig) {
        this.mercadoPagoConfig = mercadoPagoConfig;
    }

    /**
     * Retorna a Public Key para o frontend inicializar o MercadoPago.js e gerar card_token.
     * Requer autenticação (usuário logado na tela de checkout).
     */
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        String publicKey = mercadoPagoConfig.getPublicKey();
        log.info("🔑 Retornando Public Key: ambiente={}, keyPresent={}", 
                 mercadoPagoConfig.getEnvironment(), 
                 publicKey != null && !publicKey.isEmpty());
        return ResponseEntity.ok(Map.of("publicKey", publicKey != null ? publicKey : ""));
    }
}
