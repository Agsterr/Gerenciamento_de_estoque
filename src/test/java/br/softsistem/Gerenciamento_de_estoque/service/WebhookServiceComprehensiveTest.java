package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.config.MercadoPagoConfig;
import br.softsistem.Gerenciamento_de_estoque.config.WebhookConfig;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.model.WebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.repository.*;
import br.softsistem.Gerenciamento_de_estoque.service.webhook.WebhookEventHandler;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes abrangentes para WebhookService
 * 
 * Cobre:
 * - Testes unitários do service
 * - Testes de validação de assinatura
 * - Testes de idempotência
 * - Testes simulando eventos reais do Mercado Pago
 */
@ExtendWith(MockitoExtension.class)
class WebhookServiceComprehensiveTest {

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
    private WebhookConfig webhookConfig;

    @Mock
    private List<WebhookEventHandler> eventHandlers;

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

    // ==================== TESTES DE IDEMPOTÊNCIA ====================

    /**
     * Teste: Evento duplicado deve ser ignorado
     */
    @Test
    void testIdempotency_DuplicateEvent_ShouldBeIgnored() {
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
            // Pode lançar exceção ou retornar alreadyProcessed
        }

        // Assert
        verify(webhookEventRepository).existsByEventId(eventId);
        verify(webhookEventRepository, never()).save(any(WebhookEvent.class));
        verify(eventHandlers, never()).stream();
    }

    /**
     * Teste: Novo evento deve ser processado
     */
    @Test
    void testIdempotency_NewEvent_ShouldBeProcessed() throws Exception {
        // Arrange
        String eventId = "new_event_123";
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("payment")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("payment", new HashMap<>());
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("payment"))).thenReturn(notificationData);

        // Act
        assertDoesNotThrow(() -> {
            webhookService.handleMercadoPagoWebhook(
                    payload,
                    validSignature,
                    "request-123",
                    validRequestBody);
        });

        // Assert
        verify(webhookEventRepository).existsByEventId(eventId);
        verify(webhookEventRepository, atLeastOnce()).save(any(WebhookEvent.class));
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    /**
     * Teste: Race condition - evento inserido concorrentemente
     */
    @Test
    void testIdempotency_RaceCondition_ShouldHandleGracefully() {
        // Arrange
        String eventId = "race_condition_event";
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);
        when(webhookEventRepository.save(any(WebhookEvent.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate key"));

        // Act
        try {
            webhookService.handleMercadoPagoWebhook(
                    payload,
                    validSignature,
                    "request-123",
                    validRequestBody);
        } catch (Exception e) {
            // Esperado - deve tratar race condition
        }

        // Assert
        verify(webhookEventRepository).existsByEventId(eventId);
        verify(webhookEventRepository).save(any(WebhookEvent.class));
    }

    // ==================== TESTES DE EVENTOS REAIS ====================

    /**
     * Teste: Evento de pagamento aprovado (real)
     */
    @Test
    void testRealEvent_PaymentApproved() throws Exception {
        // Arrange
        String eventId = "789012345";
        Long paymentId = 789012345L;
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);

        // Mock Mercado Pago Payment - simplificado para evitar problemas com enum
        // O teste foca no fluxo de processamento, não nos detalhes do SDK
        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);
        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("payment")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("payment", new HashMap<>());
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("payment"))).thenReturn(notificationData);

        // Act
        webhookService.handleMercadoPagoWebhook(
                payload,
                validSignature,
                "request-123",
                validRequestBody);

        // Assert
        verify(mercadoPagoService).processWebhookNotification(eq(eventId), eq("payment"));
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    /**
     * Teste: Evento de pagamento pendente (real)
     */
    @Test
    void testRealEvent_PaymentPending() throws Exception {
        // Arrange
        String eventId = "pending_payment_123";
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("payment")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("payment", new HashMap<>());
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("payment"))).thenReturn(notificationData);

        // Act
        webhookService.handleMercadoPagoWebhook(
                payload,
                validSignature,
                "request-123",
                validRequestBody);

        // Assert
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    /**
     * Teste: Evento de chargeback (real)
     */
    @Test
    void testRealEvent_Chargeback() throws Exception {
        // Arrange
        String eventId = "chargeback_123456";
        Long paymentId = 789012L;
        Map<String, Object> payload = createChargebackWebhookPayload(eventId, paymentId);

        Payment existingPayment = new Payment();
        existingPayment.setId(1L);
        existingPayment.setMercadoPagoPaymentId(paymentId);
        existingPayment.setStatus(Payment.PaymentStatus.APPROVED);

        Subscription subscription = createMockSubscription();
        existingPayment.setSubscription(subscription);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("chargebacks")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

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
     * Teste: Evento de merchant_order (real)
     */
    @Test
    void testRealEvent_MerchantOrder() throws Exception {
        // Arrange
        String eventId = "merchant_order_123";
        Map<String, Object> payload = createMerchantOrderWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("merchant_order")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("merchant_order", new HashMap<>());
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("merchant_order")))
                .thenReturn(notificationData);

        // Act
        webhookService.handleMercadoPagoWebhook(
                payload,
                validSignature,
                "request-123",
                validRequestBody);

        // Assert
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    /**
     * Teste: Evento de subscription (real)
     */
    @Test
    void testRealEvent_Subscription() throws Exception {
        // Arrange
        String eventId = "subscription_123";
        Map<String, Object> payload = createSubscriptionWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("subscriptions")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("preapproval", new HashMap<>());
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("subscriptions")))
                .thenReturn(notificationData);

        // Act
        webhookService.handleMercadoPagoWebhook(
                payload,
                validSignature,
                "request-123",
                validRequestBody);

        // Assert
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    /**
     * Teste: Evento de card.updated (real)
     */
    @Test
    void testRealEvent_CardUpdated() throws Exception {
        // Arrange
        String eventId = "card_updated_123";
        Map<String, Object> payload = createCardUpdatedWebhookPayload(eventId);

        when(webhookEventRepository.existsByEventId(eventId)).thenReturn(false);
        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        WebhookEventHandler mockHandler = mock(WebhookEventHandler.class);
        when(mockHandler.canHandle("card.updated")).thenReturn(true);
        when(eventHandlers.stream()).thenReturn(java.util.stream.Stream.of(mockHandler));

        Map<String, Object> notificationData = new HashMap<>();
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("id", "card_123");
        cardData.put("customer_id", "customer_456");
        notificationData.put("card", cardData);
        when(mercadoPagoService.processWebhookNotification(eq(eventId), eq("card.updated")))
                .thenReturn(notificationData);

        // Act
        webhookService.handleMercadoPagoWebhook(
                payload,
                validSignature,
                "request-123",
                validRequestBody);

        // Assert
        verify(mockHandler).handle(eq(eventId), any(), any());
    }

    // ==================== TESTES DE VALIDAÇÃO ====================

    /**
     * Teste: Assinatura inválida deve rejeitar
     */
    @Test
    void testValidation_InvalidSignature_ShouldReject() {
        // Arrange
        String eventId = "test_event";
        Map<String, Object> payload = createPaymentWebhookPayload(eventId);
        String invalidSignature = "ts=12345,v1=invalid_hash";

        when(signatureValidator.validate(invalidSignature, validRequestBody, webhookSecret)).thenReturn(false);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            webhookService.handleMercadoPagoWebhook(
                    payload,
                    invalidSignature,
                    "request-123",
                    validRequestBody);
        });

        verify(signatureValidator).validate(invalidSignature, validRequestBody, webhookSecret);
        verify(webhookEventRepository, never()).save(any(WebhookEvent.class));
    }

    /**
     * Teste: Payload inválido deve rejeitar
     */
    @Test
    void testValidation_InvalidPayload_ShouldReject() {
        // Arrange
        Map<String, Object> invalidPayload = new HashMap<>();
        invalidPayload.put("type", "payment");
        // Sem campo "data"

        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            webhookService.handleMercadoPagoWebhook(
                    invalidPayload,
                    validSignature,
                    "request-123",
                    validRequestBody);
        });
    }

    /**
     * Teste: Event ID ausente deve rejeitar
     */
    @Test
    void testValidation_MissingEventId_ShouldReject() {
        // Arrange
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "payment");
        Map<String, Object> data = new HashMap<>();
        // Sem campo "id"
        payload.put("data", data);

        when(signatureValidator.validate(anyString(), anyString(), anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            webhookService.handleMercadoPagoWebhook(
                    payload,
                    validSignature,
                    "request-123",
                    validRequestBody);
        });
    }

    // ==================== HELPERS ====================

    private Map<String, Object> createPaymentWebhookPayload(String eventId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "webhook_" + System.currentTimeMillis());
        payload.put("type", "payment");
        payload.put("live_mode", true);
        payload.put("date_created", "2025-01-15T10:30:00Z");

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        payload.put("data", data);

        return payload;
    }

    private Map<String, Object> createChargebackWebhookPayload(String eventId, Long paymentId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "webhook_" + System.currentTimeMillis());
        payload.put("type", "chargebacks");
        payload.put("live_mode", true);
        payload.put("date_created", "2025-01-15T10:30:00Z");

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        payload.put("data", data);

        return payload;
    }

    private Map<String, Object> createMerchantOrderWebhookPayload(String eventId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "webhook_" + System.currentTimeMillis());
        payload.put("type", "merchant_order");
        payload.put("live_mode", true);
        payload.put("date_created", "2025-01-15T10:30:00Z");

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        payload.put("data", data);

        return payload;
    }

    private Map<String, Object> createSubscriptionWebhookPayload(String eventId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "webhook_" + System.currentTimeMillis());
        payload.put("type", "subscriptions");
        payload.put("live_mode", true);
        payload.put("date_created", "2025-01-15T10:30:00Z");

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        payload.put("data", data);

        return payload;
    }

    private Map<String, Object> createCardUpdatedWebhookPayload(String eventId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", "webhook_" + System.currentTimeMillis());
        payload.put("type", "card.updated");
        payload.put("live_mode", true);
        payload.put("date_created", "2025-01-15T10:30:00Z");

        Map<String, Object> data = new HashMap<>();
        data.put("id", eventId);
        payload.put("data", data);

        return payload;
    }

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
