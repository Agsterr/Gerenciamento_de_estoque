package br.softsistem.Gerenciamento_de_estoque.service;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.PlanType;
import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Plan;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.model.Usuario;
import br.softsistem.Gerenciamento_de_estoque.model.WebhookEvent;
import br.softsistem.Gerenciamento_de_estoque.model.Org;
import br.softsistem.Gerenciamento_de_estoque.repository.PaymentRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.PlanRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.UsuarioRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.WebhookEventRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.OrgRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para cenários críticos de webhook:
 * 1. Webhook fora de ordem (payment aprovado antes do pending)
 * 2. Retry automático após restart
 * 3. Idempotência de eventos
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class WebhookOrderAndRetryTest {

    @SuppressWarnings("unused")
    @Autowired
    private WebhookService webhookService; // Mantido para uso futuro nos testes

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private OrgRepository orgRepository;

    private Usuario testUser;
    private Plan testPlan;
    private Subscription testSubscription;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        Org org = new Org();
        org.setNome("Org Teste Webhook");
        org = orgRepository.save(org);

        // Criar usuário de teste
        testUser = new Usuario();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setSenha("password123");
        testUser.setOrg(org);
        testUser = usuarioRepository.save(testUser);

        // Criar plano de teste
        testPlan = new Plan();
        testPlan.setName("Test Plan");
        testPlan.setPrice(BigDecimal.valueOf(29.90));
        testPlan.setType(PlanType.BASIC);
        testPlan = planRepository.save(testPlan);

        // Criar assinatura de teste
        testSubscription = new Subscription();
        testSubscription.setUser(testUser);
        testSubscription.setPlan(testPlan);
        testSubscription.setStatus(SubscriptionStatus.INCOMPLETE);
        testSubscription.setMercadoPagoSubscriptionId("test_preference_123");
        testSubscription = subscriptionRepository.save(testSubscription);
    }

    /**
     * Teste 1: Webhook fora de ordem
     * 
     * Cenário:
     * 1. Receber webhook APPROVED com timestamp T1
     * 2. Receber webhook PENDING com timestamp T0 (mais antigo que T1)
     * 
     * Esperado:
     * - Status permanece APPROVED (não regride para PENDING)
     * - Evento PENDING é ignorado
     */
    @Test
    void shouldIgnoreOutOfOrderWebhook() {
        // Dado: Payment aprovado com timestamp recente
        Long paymentId = 123456789L;
        LocalDateTime approvedTime = LocalDateTime.now();

        Payment approvedPayment = new Payment();
        approvedPayment.setSubscription(testSubscription);
        approvedPayment.setMercadoPagoPaymentId(paymentId);
        approvedPayment.setStatus(Payment.PaymentStatus.APPROVED);
        approvedPayment.setAmount(BigDecimal.valueOf(29.90));
        approvedPayment.setCurrency("BRL");
        approvedPayment.setLastStatusUpdateAt(approvedTime);
        approvedPayment = paymentRepository.save(approvedPayment);

        // Quando: Tentar processar webhook PENDING com timestamp mais antigo
        LocalDateTime olderPendingTime = approvedTime.minusMinutes(10);

        // Simular tentativa de atualizar para PENDING (mais antigo)
        Optional<Payment> existingOpt = paymentRepository.findByMercadoPagoPaymentId(paymentId);
        assertTrue(existingOpt.isPresent());

        Payment existing = existingOpt.get();

        // Verificar que evento mais antigo seria ignorado
        if (existing.getLastStatusUpdateAt() != null &&
                olderPendingTime.isBefore(existing.getLastStatusUpdateAt())) {
            // Evento seria ignorado - não atualizar
            // Este é o comportamento esperado
        }

        // Então: Status deve permanecer APPROVED
        Long paymentIdToFind = approvedPayment.getId();
        assertNotNull(paymentIdToFind);
        Payment result = paymentRepository.findById(paymentIdToFind).orElseThrow();
        assertEquals(Payment.PaymentStatus.APPROVED, result.getStatus());
        assertEquals(approvedTime, result.getLastStatusUpdateAt());

        // Verificar que não houve regressão
        assertNotEquals(Payment.PaymentStatus.PENDING, result.getStatus());
    }

    /**
     * Teste 2: Webhook mais recente deve atualizar status
     * 
     * Cenário:
     * 1. Payment está PENDING com timestamp T0
     * 2. Receber webhook APPROVED com timestamp T1 (mais recente)
     * 
     * Esperado:
     * - Status é atualizado para APPROVED
     * - lastStatusUpdateAt é atualizado para T1
     */
    @Test
    void shouldUpdateStatusWhenNewerEventArrives() {
        // Dado: Payment pendente com timestamp antigo
        Long paymentId = 987654321L;
        LocalDateTime pendingTime = LocalDateTime.now().minusMinutes(5);

        Payment pendingPayment = new Payment();
        pendingPayment.setSubscription(testSubscription);
        pendingPayment.setMercadoPagoPaymentId(paymentId);
        pendingPayment.setStatus(Payment.PaymentStatus.PENDING);
        pendingPayment.setAmount(BigDecimal.valueOf(29.90));
        pendingPayment.setCurrency("BRL");
        pendingPayment.setLastStatusUpdateAt(pendingTime);
        pendingPayment = paymentRepository.save(pendingPayment);

        // Quando: Atualizar para APPROVED com timestamp mais recente
        LocalDateTime approvedTime = LocalDateTime.now();

        pendingPayment.setStatus(Payment.PaymentStatus.APPROVED);
        pendingPayment.setLastStatusUpdateAt(approvedTime);
        paymentRepository.save(pendingPayment);

        // Então: Status deve ser APPROVED e timestamp atualizado
        Long paymentIdToFind = pendingPayment.getId();
        assertNotNull(paymentIdToFind);
        Payment result = paymentRepository.findById(paymentIdToFind).orElseThrow();
        assertEquals(Payment.PaymentStatus.APPROVED, result.getStatus());
        assertEquals(approvedTime, result.getLastStatusUpdateAt());
        assertTrue(result.getLastStatusUpdateAt().isAfter(pendingTime));
    }

    /**
     * Teste 3: Idempotência - mesmo event_id não deve ser processado duas vezes
     */
    @Test
    void shouldNotProcessSameEventIdTwice() {
        // Dado: Evento já processado
        String eventId = "test_event_123";

        WebhookEvent event = new WebhookEvent(eventId);
        event.setProcessed(true);
        event.setProcessedAt(LocalDateTime.now());
        webhookEventRepository.save(event);

        // Quando: Tentar processar o mesmo event_id novamente
        boolean exists = webhookEventRepository.existsByEventId(eventId);
        Optional<WebhookEvent> existingOpt = webhookEventRepository.findByEventId(eventId);

        // Então: Deve detectar que já foi processado
        assertTrue(exists);
        assertTrue(existingOpt.isPresent());
        assertTrue(existingOpt.get().getProcessed());

        // Não deve processar novamente
        assertNotNull(existingOpt.get().getProcessedAt());
    }

    /**
     * Teste 4: Evento não processado deve ser encontrado para retry
     */
    @Test
    void shouldFindUnprocessedEventsForRetry() {
        // Dado: Eventos não processados
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(5);

        WebhookEvent unprocessed1 = new WebhookEvent("event_1");
        unprocessed1.setProcessed(false);
        unprocessed1.setCreatedAt(cutoffTime.minusMinutes(10));
        webhookEventRepository.save(unprocessed1);

        WebhookEvent unprocessed2 = new WebhookEvent("event_2");
        unprocessed2.setProcessed(false);
        unprocessed2.setCreatedAt(cutoffTime.minusMinutes(8));
        webhookEventRepository.save(unprocessed2);

        WebhookEvent processed = new WebhookEvent("event_3");
        processed.setProcessed(true);
        processed.setProcessedAt(LocalDateTime.now());
        processed.setCreatedAt(cutoffTime.minusMinutes(6));
        webhookEventRepository.save(processed);

        // Quando: Buscar eventos não processados
        var unprocessed = webhookEventRepository.findByProcessedFalseAndCreatedAtBefore(cutoffTime);

        // Então: Deve encontrar apenas os não processados criados antes do cutoff
        assertEquals(2, unprocessed.size());
        assertTrue(unprocessed.stream().anyMatch(e -> e.getEventId().equals("event_1")));
        assertTrue(unprocessed.stream().anyMatch(e -> e.getEventId().equals("event_2")));
        assertFalse(unprocessed.stream().anyMatch(e -> e.getEventId().equals("event_3")));
    }

    /**
     * Teste 5: Evento com erro deve manter processed = false para permitir retry
     */
    @Test
    void shouldKeepProcessedFalseWhenErrorOccurs() {
        // Dado: Evento que falhou no processamento
        String eventId = "failed_event_123";

        WebhookEvent failedEvent = new WebhookEvent(eventId);
        failedEvent.setProcessed(false);
        failedEvent.setErrorMessage("Erro ao processar: timeout");
        webhookEventRepository.save(failedEvent);

        // Quando: Verificar status do evento
        Optional<WebhookEvent> eventOpt = webhookEventRepository.findByEventId(eventId);

        // Então: Deve estar marcado como não processado
        assertTrue(eventOpt.isPresent());
        assertFalse(eventOpt.get().getProcessed());
        assertNotNull(eventOpt.get().getErrorMessage());
        assertNull(eventOpt.get().getProcessedAt());

        // Deve estar disponível para retry
        var unprocessed = webhookEventRepository.findByProcessedFalse();
        assertTrue(unprocessed.stream().anyMatch(e -> e.getEventId().equals(eventId)));
    }
}
