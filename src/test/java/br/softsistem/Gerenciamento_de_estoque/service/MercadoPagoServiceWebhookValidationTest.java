//package br.softsistem.Gerenciamento_de_estoque.service;
//
//import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import javax.crypto.Mac;
//import javax.crypto.spec.SecretKeySpec;
//import java.nio.charset.StandardCharsets;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.when;
//
///**
// * Testes unitários para validação de webhooks do Mercado Pago
// *
// * Testa o algoritmo de validação conforme documentação oficial:
// * - signedPayload = ts + "." + requestBody
// * - expectedHash = HMAC-SHA256(secret, signedPayload)
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("Validação de Webhook do Mercado Pago")
//class MercadoPagoServiceWebhookValidationTest {
//
//    @Mock
//    private MercadoPagoConfig mercadoPagoConfig;
//
//    private MercadoPagoService mercadoPagoService;
//
//    private static final String TEST_SECRET = "TEST_WEBHOOK_SECRET_123456";
//    private static final String TEST_REQUEST_BODY = "{\"type\":\"payment\",\"data\":{\"id\":\"123456789\"}}";
//    private static final String TEST_TIMESTAMP = "1700000000";
//
//    @BeforeEach
//    void setUp() {
//        mercadoPagoService = new MercadoPagoService(mercadoPagoConfig);
//        when(mercadoPagoConfig.isMercadoPagoConfigured()).thenReturn(true);
//    }
//
//    @Test
//    @DisplayName("Deve validar webhook com assinatura válida")
//    void deveValidarWebhookComAssinaturaValida() throws NoSuchAlgorithmException, InvalidKeyException {
//        // Arrange
//        String signedPayload = TEST_TIMESTAMP + "." + TEST_REQUEST_BODY;
//        String validHash = calculateHmacSha256(TEST_SECRET, signedPayload);
//        String signature = "ts=" + TEST_TIMESTAMP + ",v1=" + validHash;
//
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook(signature, TEST_REQUEST_BODY, TEST_SECRET);
//
//        // Assert
//        assertTrue(isValid, "Webhook com assinatura válida deve ser aceito");
//    }
////
//    @Test
//    @DisplayName("Deve rejeitar webhook com assinatura inválida")
//    void deveRejeitarWebhookComAssinaturaInvalida() {
//        // Arrange
//        String invalidHash = "invalid_hash_123456789";
//        String signature = "ts=" + TEST_TIMESTAMP + ",v1=" + invalidHash;
//
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook(signature, TEST_REQUEST_BODY, TEST_SECRET);
//
//        // Assert
//        assertFalse(isValid, "Webhook com assinatura inválida deve ser rejeitado");
//    }
//
//    @Test
//    @DisplayName("Deve rejeitar webhook sem header x-signature")
//    void deveRejeitarWebhookSemSignature() {
//        // Arrange
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook(null, TEST_REQUEST_BODY, TEST_SECRET);
//
//        // Assert
//        assertFalse(isValid, "Webhook sem signature deve ser rejeitado");
//    }
//
//    @Test
//    @DisplayName("Deve rejeitar webhook com signature vazia")
//    void deveRejeitarWebhookComSignatureVazia() {
//        // Arrange
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook("", TEST_REQUEST_BODY, TEST_SECRET);
//
//        // Assert
//        assertFalse(isValid, "Webhook com signature vazia deve ser rejeitado");
//    }
//
//    @Test
//    @DisplayName("Deve rejeitar webhook sem secret configurado")
//    void deveRejeitarWebhookSemSecret() {
//        // Arrange
//        String signature = "ts=" + TEST_TIMESTAMP + ",v1=hash";
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook(signature, TEST_REQUEST_BODY, null);
//
//        // Assert
//        assertFalse(isValid, "Webhook sem secret configurado deve ser rejeitado");
//    }
//
//    @Test
//    @DisplayName("Deve rejeitar webhook com request body vazio")
//    void deveRejeitarWebhookComBodyVazio() throws NoSuchAlgorithmException, InvalidKeyException {
//        // Arrange
//        String signedPayload = TEST_TIMESTAMP + ".";
//        String validHash = calculateHmacSha256(TEST_SECRET, signedPayload);
//        String signature = "ts=" + TEST_TIMESTAMP + ",v1=" + validHash;
//
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook(signature, "", TEST_SECRET);
//
//        // Assert
//        assertFalse(isValid, "Webhook com body vazio deve ser rejeitado");
//    }
//
//    @Test
//    @DisplayName("Deve rejeitar webhook com formato de signature inválido")
//    void deveRejeitarWebhookComFormatoInvalido() {
//        // Arrange
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        // Teste com signature sem ts
//        String signatureSemTs = "v1=hash123";
//        boolean isValid1 = mercadoPagoService.validateWebhook(signatureSemTs, TEST_REQUEST_BODY, TEST_SECRET);
//        assertFalse(isValid1, "Signature sem ts deve ser rejeitada");
//
//        // Teste com signature sem v1
//        String signatureSemV1 = "ts=1700000000";
//        boolean isValid2 = mercadoPagoService.validateWebhook(signatureSemV1, TEST_REQUEST_BODY, TEST_SECRET);
//        assertFalse(isValid2, "Signature sem v1 deve ser rejeitada");
//
//        // Teste com formato completamente inválido
//        String signatureInvalida = "invalid_format";
//        boolean isValid3 = mercadoPagoService.validateWebhook(signatureInvalida, TEST_REQUEST_BODY, TEST_SECRET);
//        assertFalse(isValid3, "Signature com formato inválido deve ser rejeitada");
//    }
//
//    @Test
//    @DisplayName("Deve validar webhook com diferentes timestamps")
//    void deveValidarWebhookComDiferentesTimestamps() throws NoSuchAlgorithmException, InvalidKeyException {
//        // Arrange
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        // Teste com timestamp diferente
//        String timestamp2 = "1700001000";
//        String signedPayload2 = timestamp2 + "." + TEST_REQUEST_BODY;
//        String validHash2 = calculateHmacSha256(TEST_SECRET, signedPayload2);
//        String signature2 = "ts=" + timestamp2 + ",v1=" + validHash2;
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook(signature2, TEST_REQUEST_BODY, TEST_SECRET);
//
//        // Assert
//        assertTrue(isValid, "Webhook com timestamp diferente mas hash válido deve ser aceito");
//    }
//
//    @Test
//    @DisplayName("Deve validar webhook com diferentes request bodies")
//    void deveValidarWebhookComDiferentesBodies() throws NoSuchAlgorithmException, InvalidKeyException {
//        // Arrange
//        when(mercadoPagoConfig.getWebhookSecret()).thenReturn(TEST_SECRET);
//
//        String differentBody = "{\"type\":\"preference\",\"data\":{\"id\":\"987654321\"}}";
//        String signedPayload = TEST_TIMESTAMP + "." + differentBody;
//        String validHash = calculateHmacSha256(TEST_SECRET, signedPayload);
//        String signature = "ts=" + TEST_TIMESTAMP + ",v1=" + validHash;
//
//        // Act
//        boolean isValid = mercadoPagoService.validateWebhook(signature, differentBody, TEST_SECRET);
//
//        // Assert
//        assertTrue(isValid, "Webhook com body diferente mas hash válido deve ser aceito");
//    }
//
//    /**
//     * Calcula HMAC-SHA256 conforme algoritmo do Mercado Pago
//     */
//    private String calculateHmacSha256(String secret, String data) throws NoSuchAlgorithmException, InvalidKeyException {
//        Mac mac = Mac.getInstance("HmacSHA256");
//        SecretKeySpec secretKeySpec = new SecretKeySpec(
//            secret.getBytes(StandardCharsets.UTF_8),
//            "HmacSHA256"
//        );
//        mac.init(secretKeySpec);
//        byte[] hashBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
//        return bytesToHex(hashBytes);
//    }
//
//    /**
//     * Converte bytes para hexadecimal
//     */
//    private String bytesToHex(byte[] bytes) {
//        StringBuilder result = new StringBuilder();
//        for (byte b : bytes) {
//            result.append(String.format("%02x", b));
//        }
//        return result.toString();
//    }
//}
//
//
//
//
//
//
//
