package br.softsistem.Gerenciamento_de_estoque.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para WebhookSignatureValidator
 * 
 * Testa a validação de assinatura conforme documentação oficial do Mercado Pago
 */
@ExtendWith(MockitoExtension.class)
class WebhookSignatureValidatorTest {
    
    @InjectMocks
    private WebhookSignatureValidator signatureValidator;
    
    private String webhookSecret;
    private String requestBody;
    private String validSignature;
    
    @BeforeEach
    void setUp() throws Exception {
        webhookSecret = "test_webhook_secret_12345";
        requestBody = "{\"id\":\"123456\",\"type\":\"payment\",\"data\":{\"id\":\"789012\"}}";
        
        // Gerar assinatura válida conforme algoritmo do Mercado Pago
        long timestamp = System.currentTimeMillis() / 1000;
        String signedPayload = timestamp + "." + requestBody;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hashBytes = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hash = new StringBuilder();
        for (byte b : hashBytes) {
            hash.append(String.format("%02x", b));
        }
        
        validSignature = "ts=" + timestamp + ",v1=" + hash.toString();
    }
    
    /**
     * Teste: Assinatura válida
     */
    @Test
    void testValidate_ValidSignature() {
        // Act
        boolean isValid = signatureValidator.validate(validSignature, requestBody, webhookSecret);
        
        // Assert
        assertTrue(isValid, "Assinatura válida deve ser aceita");
    }
    
    /**
     * Teste: Assinatura inválida (hash incorreto)
     */
    @Test
    void testValidate_InvalidHash() {
        // Arrange
        String invalidSignature = "ts=1700000000,v1=invalid_hash_abc123";
        
        // Act
        boolean isValid = signatureValidator.validate(invalidSignature, requestBody, webhookSecret);
        
        // Assert
        assertFalse(isValid, "Assinatura com hash inválido deve ser rejeitada");
    }
    
    /**
     * Teste: Assinatura com timestamp incorreto
     */
    @Test
    void testValidate_WrongTimestamp() throws Exception {
        // Arrange - usar timestamp diferente
        long wrongTimestamp = System.currentTimeMillis() / 1000 - 1000;
        String signedPayload = wrongTimestamp + "." + requestBody;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            webhookSecret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hashBytes = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hash = new StringBuilder();
        for (byte b : hashBytes) {
            hash.append(String.format("%02x", b));
        }
        
        String wrongSignature = "ts=" + wrongTimestamp + ",v1=" + hash.toString();
        
        // Act
        boolean isValid = signatureValidator.validate(wrongSignature, requestBody, webhookSecret);
        
        // Assert
        assertTrue(isValid, "Timestamp antigo não invalida assinatura quando hash está correto");
    }
    
    /**
     * Teste: Assinatura ausente
     */
    @Test
    void testValidate_MissingSignature() {
        // Act
        boolean isValid = signatureValidator.validate(null, requestBody, webhookSecret);
        
        // Assert
        assertFalse(isValid, "Assinatura ausente deve ser rejeitada");
    }
    
    /**
     * Teste: Assinatura vazia
     */
    @Test
    void testValidate_EmptySignature() {
        // Act
        boolean isValid = signatureValidator.validate("", requestBody, webhookSecret);
        
        // Assert
        assertFalse(isValid, "Assinatura vazia deve ser rejeitada");
    }
    
    /**
     * Teste: Request body ausente
     */
    @Test
    void testValidate_MissingRequestBody() {
        // Act
        boolean isValid = signatureValidator.validate(validSignature, null, webhookSecret);
        
        // Assert
        assertFalse(isValid, "Request body ausente deve ser rejeitado");
    }
    
    /**
     * Teste: Request body vazio
     */
    @Test
    void testValidate_EmptyRequestBody() {
        // Act
        boolean isValid = signatureValidator.validate(validSignature, "", webhookSecret);
        
        // Assert
        assertFalse(isValid, "Request body vazio deve ser rejeitado");
    }
    
    /**
     * Teste: Secret ausente
     */
    @Test
    void testValidate_MissingSecret() {
        // Act
        boolean isValid = signatureValidator.validate(validSignature, requestBody, null);
        
        // Assert
        assertFalse(isValid, "Secret ausente deve ser rejeitado");
    }
    
    /**
     * Teste: Secret vazio
     */
    @Test
    void testValidate_EmptySecret() {
        // Act
        boolean isValid = signatureValidator.validate(validSignature, requestBody, "");
        
        // Assert
        assertFalse(isValid, "Secret vazio deve ser rejeitado");
    }
    
    /**
     * Teste: Formato de assinatura inválido (sem ts)
     */
    @Test
    void testValidate_InvalidFormat_NoTimestamp() {
        // Arrange
        String invalidSignature = "v1=abc123";
        
        // Act
        boolean isValid = signatureValidator.validate(invalidSignature, requestBody, webhookSecret);
        
        // Assert
        assertFalse(isValid, "Assinatura sem timestamp deve ser rejeitada");
    }
    
    /**
     * Teste: Formato de assinatura inválido (sem v1)
     */
    @Test
    void testValidate_InvalidFormat_NoHash() {
        // Arrange
        String invalidSignature = "ts=1700000000";
        
        // Act
        boolean isValid = signatureValidator.validate(invalidSignature, requestBody, webhookSecret);
        
        // Assert
        assertFalse(isValid, "Assinatura sem hash deve ser rejeitada");
    }
    
    /**
     * Teste: Formato de assinatura inválido (formato incorreto)
     */
    @Test
    void testValidate_InvalidFormat_WrongFormat() {
        // Arrange
        String invalidSignature = "invalid_format";
        
        // Act
        boolean isValid = signatureValidator.validate(invalidSignature, requestBody, webhookSecret);
        
        // Assert
        assertFalse(isValid, "Formato de assinatura inválido deve ser rejeitado");
    }
    
    /**
     * Teste: Payload real do Mercado Pago (simulação)
     */
    @Test
    void testValidate_RealMercadoPagoPayload() throws Exception {
        // Arrange - Payload real conforme documentação
        String realRequestBody = "{\"id\":\"123456789\",\"type\":\"payment\",\"live_mode\":true,\"date_created\":\"2025-01-15T10:30:00Z\",\"data\":{\"id\":\"789012345\"}}";
        String realSecret = "real_secret_key_from_mercadopago_panel";
        
        long timestamp = 1705312200L; // Timestamp fixo para teste
        String signedPayload = timestamp + "." + realRequestBody;
        
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            realSecret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        mac.init(secretKeySpec);
        byte[] hashBytes = mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hash = new StringBuilder();
        for (byte b : hashBytes) {
            hash.append(String.format("%02x", b));
        }
        
        String realSignature = "ts=" + timestamp + ",v1=" + hash.toString();
        
        // Act
        boolean isValid = signatureValidator.validate(realSignature, realRequestBody, realSecret);
        
        // Assert
        assertTrue(isValid, "Payload real do Mercado Pago deve ser validado corretamente");
    }
}







