package br.softsistem.Gerenciamento_de_estoque.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Classe responsável pela validação de assinatura de webhooks do Mercado Pago
 * 
 * Como funciona a validação:
 * 
 * 1. O Mercado Pago envia um header x-signature no formato: ts=<timestamp>,v1=<hash>
 *    Exemplo: x-signature: ts=1700000000,v1=a1b2c3d4e5f6...
 * 
 * 2. O algoritmo de validação:
 *    a) Extrai o timestamp (ts) e o hash (v1) do header
 *    b) Constrói o signedPayload = ts + "." + requestBody
 *    c) Calcula o hash esperado usando HMAC-SHA256(secret, signedPayload)
 *    d) Compara o hash calculado com o hash recebido (v1)
 * 
 * 3. Se os hashes coincidirem, a requisição é autêntica e veio do Mercado Pago.
 *    Caso contrário, a requisição é rejeitada como inválida.
 * 
 * 4. A comparação é feita em tempo constante (timing-safe) para evitar timing attacks.
 * 
 * Documentação oficial:
 * https://www.mercadopago.com.br/developers/pt/docs/your-integrations/notifications/webhooks
 */
@Component
public class WebhookSignatureValidator {
    
    private static final Logger log = LoggerFactory.getLogger(WebhookSignatureValidator.class);
    
    /**
     * Valida a assinatura X-Signature do webhook do Mercado Pago
     * 
     * @param signature Header x-signature (formato: ts=1700000000,v1=HASH_SHA256)
     * @param requestBody Body raw da requisição como string (JSON completo)
     * @param secret Secret configurado no painel de Webhooks do Mercado Pago
     * @return true se assinatura válida, false caso contrário
     * @throws IllegalArgumentException se parâmetros obrigatórios estiverem ausentes
     */
    public boolean validate(String signature, String requestBody, String secret) {
        // Validação de parâmetros obrigatórios
        if (signature == null || signature.isEmpty()) {
            log.warn("Webhook rejeitado: header x-signature ausente");
            return false;
        }
        
        if (secret == null || secret.isEmpty()) {
            log.error("Webhook secret não configurado - validação impossível");
            return false;
        }
        
        if (requestBody == null || requestBody.isEmpty()) {
            log.warn("Webhook rejeitado: request body vazio");
            return false;
        }
        
        return validateSignature(signature, requestBody, secret);
    }
    
    /**
     * Valida a assinatura X-Signature do webhook conforme documentação oficial do Mercado Pago
     * 
     * Algoritmo oficial:
     * 1. Parse do header: ts=<timestamp>,v1=<hash>
     * 2. signedPayload = ts + "." + requestBody
     * 3. expectedHash = HMAC-SHA256(secret, signedPayload)
     * 4. Comparar expectedHash com v1 (timing-safe)
     * 
     * @param signature Header x-signature (formato: ts=1700000000,v1=HASH_SHA256)
     * @param requestBody Body raw da requisição como string
     * @param secret Secret configurado no painel de Webhooks
     * @return true se assinatura válida, false caso contrário
     */
    private boolean validateSignature(String signature, String requestBody, String secret) {
        try {
            // 1. Parse da assinatura: ts=1700000000,v1=abc123...
            Map<String, String> parts = Stream.of(signature.split(","))
                .map(s -> s.split("=", 2))
                .filter(a -> a.length == 2)
                .collect(Collectors.toMap(
                    a -> a[0].trim(),
                    a -> a[1].trim(),
                    (v1, v2) -> v1 // Em caso de duplicatas, manter o primeiro
                ));
            
            String ts = parts.get("ts");
            String v1 = parts.get("v1");
            
            if (ts == null || ts.isEmpty() || v1 == null || v1.isEmpty()) {
                log.warn("Formato de assinatura inválido: ts ou v1 ausente. Signature: {}", signature);
                return false;
            }
            
            // 2. Conforme documentação oficial: signedPayload = ts + "." + requestBody
            String signedPayload = ts + "." + requestBody;
            
            // 3. Calcular HMAC-SHA256: HMAC-SHA256(secret, signedPayload)
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = 
                new javax.crypto.spec.SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hashBytes = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
            String expectedHash = bytesToHex(hashBytes);
            
            // 4. Comparar hashes (timing-safe para evitar timing attacks)
            boolean isValid = constantTimeEquals(expectedHash, v1);
            
            if (!isValid) {
                log.warn("Assinatura do webhook inválida - requisição rejeitada");
            } else {
                log.info("Assinatura do webhook válida");
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Erro ao validar assinatura do webhook: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Comparação de strings em tempo constante para evitar timing attacks
     * 
     * Timing attacks exploram diferenças no tempo de execução para descobrir informações.
     * Esta implementação garante que a comparação sempre leve o mesmo tempo,
     * independentemente de onde a diferença ocorre.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
    
    /**
     * Converte bytes para hexadecimal
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}







