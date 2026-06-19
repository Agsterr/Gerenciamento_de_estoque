package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.model.WebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import br.softsistem.Gerenciamento_de_estoque.service.webhook.WebhookEventHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes obrigatórios para WebhookService
 * 
 * Framework: JUnit 5 + Mockito
 * 
 * Testes:
 * - Validação de assinatura
 * - Idempotência
 * - Processamento de pagamento aprovado
 * - Chargeback
 * 
 * Simula payloads reais da documentação do Mercado Pago
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private MercadoPagoConfig mercadoPagoConfig;

    @Mock
    private MercadoPagoService mercadoPagoService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private WebhookEventRepository webhookEventRepository;

    @Mock
    private WebhookSignatureValidator signatureValidator;

    @Mock
    private FailedWebhookEventService failedWebhookEventService;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private List<WebhookEventHandler> eventHandlers;

    @Mock
    private br.softsistem.Gerenciamento_de_estoque.config.WebhookConfig webhookConfig;

    @Mock
    private WebhookMonitoringService monitoringService;

    @Mock
    private WebhookAlertService alertService;

    @InjectMocks
    private WebhookService webhookService;

    private String validSignature;
    private String validRequestBody;
    private String webhookSecret;

    @BeforeEach
    void setUp() throws Exception {
        webhookSecret = "test_webhook_secret_12345";
        validRequestBody = "{\"id\":\"123456\",\"type\":\"payment\",\"data\":{\"id\":\"789012\"}}";

        // Gerar assinatura válida
        long timestamp = System.currentTimeMillis() / 1000;
        String signedPayload = timestamp + "." + validRequestBody;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                webhookSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hashBytes = mac.doFinal(signedPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        StringBuilder hash = new StringBuilder();
        for (byte b : hashBytes) {
            hash.append(String.format("%02x", b));
        }
        validSignature = "ts=" + timestamp + ",v1=" + hash.toString();

        lenient().when(mercadoPagoConfig.getWebhookSecret()).thenReturn(webhookSecret);
        lenient().when(webhookConfig.getProcessingTimeoutSeconds()).thenReturn(30);
        lenient().when(webhookConfig.getProcessingTimeoutMillis()).thenReturn(30000L);
    }

    /**
     * Teste: Validação de assinatura
     * 
     * Simula payload real do Mercado Pago conforme documentação oficial
     */
    @Test
    void testValidateWebhookSignature_ValidSignature() {
        // Arrange
        when(signatureValidator.validate(validSignature, validRequestBody, webhookSecret)).thenReturn(true);

        // Act
        boolean isValid = webhookService.validateWebhookSignature(validSignature, validRequestBody, webhookSecret);

        // Assert
        assertTrue(isValid);
        verify(signatureValidator).validate(validSignature, validRequestBody, webhookSecret);
    }

    @Test
    void testValidateWebhookSignature_InvalidSignature() {
        // Arrange
        String invalidSignature = "ts=12345,v1=invalid_hash";
        when(signatureValidator.validate(invalidSignature, validRequestBody, webhookSecret)).thenReturn(false);

        // Act
        boolean isValid = webhookService.validateWebhookSignature(invalidSignature, validRequestBody, webhookSecret);

        // Assert
        assertFalse(isValid);
        verify(signatureValidator).validate(invalidSignature, validRequestBody, webhookSecret);
    }

    @Test
    void testValidateWebhookSignature_MissingSignature() {
        // Arrange
        when(signatureValidator.validate(null, validRequestBody, webhookSecret)).thenReturn(false);

        // Act
        boolean isValid = webhookService.validateWebhookSignature(null, validRequestBody, webhookSecret);

        // Assert
        assertFalse(isValid);
    }

    /**
     * Teste: Idempotência
     * 
     * Garante que o mesmo evento não seja processado duas vezes
     */
    @Test
    void testIdempotency_DuplicateEvent() {
        // Arrange
        String eventId = "123456789";
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(true);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        // Act
        try {
            webhookService.handleMercadoPagoWebhook(
                    payload,
                    validSignature,
                    "request-123",
                    validRequestBody);
        } catch (Exception e) {
            // Esperado - evento duplicado pode lançar exceção ou retornar alreadyProcessed
        }

        // Assert
        verify(webhookEventRepository).existsByEventId(eventId);
        verify(webhookEventRepository, never()).save(any(WebhookEvent.class));
    }

    @Test
    void testIdempotency_NewEvent() {
        // Arrange
        String eventId = "123456789";
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        // Mock handler
        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("payment")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        // Mock processWebhookNotification
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("payment", new HashMap<>());
        try {
            when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("payment")))
                    .thenReturn(notificationData);
        } catch (Exception e) {
            // Ignorar
        }

        // Act & Assert
        assertDoesNotThrow(() -> {
            webhookService.handleMercadoPagoWebhook(
                    payload,
                    validSignature,
                    "request-123",
                    validRequestBody);
        });

        verify(webhookEventRepository).existsByEventId(eventId);
    }

    /**
     * Teste: Processamento de pagamento aprovado
     * 
     * Simula payload real de pagamento aprovado conforme documentação do Mercado
     * Pago
     */
    @Test
    void testProcessApprovedPayment() throws Exception {
        // Arrange
        String eventId = "789012";
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        // Mock handler
        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("payment")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        // Mock processWebhookNotification
        Map<String, Object> notificationData = new HashMap<>();
        Map<String, Object> paymentData = new HashMap<>();
        paymentData.put("id", "789012");
        notificationData.put("payment", paymentData);
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("payment"))).thenReturn(notificationData);

        // Act
        webhookService.handleMercadoPagoWebhook(
                payload,
                validSignature,
                "request-123",
                validRequestBody);

        // Assert
        verify(webhookEventRepository).existsByEventId(eventId);
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    /**
     * Teste: Chargeback
     * 
     * Simula payload real de chargeback conforme documentação do Mercado Pago
     */
    @Test
    void testProcessChargeback() throws Exception {
        // Arrange
        String eventId = "chargeback_123";
        Long paymentId = 789012L;
        Map<String, Object> payload = createChargebackWebhookPayload(eventId, paymentId);

        // Mock Payment existente
        Payment existingPayment = new Payment();
        existingPayment.setId(1L);
        existingPayment.setMercadoPagoPaymentId(paymentId);
        existingPayment.setStatus(Payment.PaymentStatus.APPROVED);

        Subscription subscription = createMockSubscription();
        existingPayment.setSubscription(subscription);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);
        lenient().when(paymentRepository.findByMercadoPagoPaymentId(paymentId)).thenReturn(Optional.of(existingPayment));

        // Mock handler
        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("chargebacks")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        // Mock processWebhookNotification
        Map<String, Object> notificationData = new HashMap<>();
        Map<String, Object> chargebackData = new HashMap<>();
        chargebackData.put("payment_id", paymentId.toString());
        notificationData.put("chargeback", chargebackData);
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("chargebacks")))
                .thenReturn(notificationData);

        // Act
        webhookService.handleMercadoPagoWebhook(
                payload,
                validSignature,
                "request-123",
                validRequestBody);

        // Assert
        verify(webhookEventRepository).existsByEventId(eventId);
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    /**
     * Cria payload de webhook de pagamento conforme documentação do Mercado Pago
     */
    private Map<String, Object> createPaymentWebhookPayload(String eventId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "webhook_" + System.currentTimeMillis());
        payload.put("type", "payment");
        payload.put("live_mode", "true");
        payload.put("date_created", "2025-01-15T10:30:00Z");

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        payload.put("data", data);

        return payload;
    }

    /**
     * Cria payload de webhook de chargeback conforme documentação do Mercado Pago
     */
    private Map<String, Object> createChargebackWebhookPayload(String eventId, Long paymentId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "webhook_" + System.currentTimeMillis());
        payload.put("type", "chargebacks");
        payload.put("live_mode", "true");
        payload.put("date_created", "2025-01-15T10:30:00Z");

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        payload.put("data", data);

        return payload;
    }

    /**
     * Cria Subscription mock
     */
    private Subscription createMockSubscription() {
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setMercadoPagoSubscriptionId("subscription_123");
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        Usuario user = new Usuario();
        user.setId(456L);
        subscription.setUser(user);

        Plan plan = new Plan();
        plan.setId(789L);
        subscription.setPlan(plan);

        return subscription;
    }
}
