package br.softsistem.Gerenciamento_de_estoque.service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.repository.PaymentRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;

/**
 * Processa webhooks do Asaas (pagamentos confirmados/recusados).
 */
@Service
@Transactional
public class AsaasWebhookService {

    private static final Logger log = LoggerFactory.getLogger(AsaasWebhookService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionService subscriptionService;
    private final AsaasService asaasService;

    public AsaasWebhookService(
            SubscriptionRepository subscriptionRepository,
            PaymentRepository paymentRepository,
            SubscriptionService subscriptionService,
            AsaasService asaasService) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentRepository = paymentRepository;
        this.subscriptionService = subscriptionService;
        this.asaasService = asaasService;
    }

    public void processEvent(Map<String, Object> payload) {
        if (payload == null) return;

        String event = payload.get("event") != null ? payload.get("event").toString() : null;
        log.info("Webhook Asaas recebido: event={}", event);

        if (event == null) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
        if (payment == null) return;

        switch (event) {
            case "PAYMENT_RECEIVED", "PAYMENT_CONFIRMED" -> handlePaymentSuccess(payment);
            case "PAYMENT_OVERDUE", "PAYMENT_DELETED", "PAYMENT_REFUNDED" -> handlePaymentFailure(payment, event);
            default -> log.debug("Evento Asaas ignorado: {}", event);
        }
    }

    private void handlePaymentSuccess(Map<String, Object> payment) {
        String paymentId = stringVal(payment.get("id"));
        String externalRef = stringVal(payment.get("externalReference"));
        String status = stringVal(payment.get("status"));

        log.info("Pagamento Asaas confirmado: id={} status={} externalReference={}", paymentId, status, externalRef);

        if (paymentId == null) return;

        if (status != null && !asaasService.isPaymentConfirmed(payment)) {
            log.info("Pagamento Asaas ignorado (status={}): aguardando confirmação", status);
            return;
        }

        Optional<Subscription> subOpt = findSubscription(externalRef, paymentId, payment);
        if (subOpt.isEmpty()) {
            log.warn("Assinatura não encontrada para externalReference={} paymentId={}", externalRef, paymentId);
            return;
        }

        Subscription subscription = subOpt.get();
        String asaasSubscriptionId = stringVal(payment.get("subscription"));
        if (asaasSubscriptionId != null && !asaasSubscriptionId.isBlank()) {
            subscription.setAsaasSubscriptionId(asaasSubscriptionId);
        }
        subscription.setAsaasPaymentId(paymentId);
        subscription.setPaymentProvider("ASAAS");
        subscription.setAccessBlocked(false);

        subscriptionService.activateFromAsaasPayment(subscription, payment);

        registerPaymentRecord(subscription, payment, Payment.PaymentStatus.APPROVED);
        log.info("Acesso liberado para user={} até {}", subscription.getUser().getId(), subscription.getCurrentPeriodEnd());
    }

    private void handlePaymentFailure(Map<String, Object> payment, String event) {
        String paymentId = stringVal(payment.get("id"));
        log.info("Pagamento Asaas não concluído: id={} event={}", paymentId, event);

        Optional<Subscription> subOpt = findSubscription(stringVal(payment.get("externalReference")), paymentId, payment);
        if (subOpt.isEmpty()) return;

        Subscription subscription = subOpt.get();
        registerPaymentRecord(subscription, payment, Payment.PaymentStatus.REJECTED);
    }

    private Optional<Subscription> findSubscription(String externalReference, String asaasPaymentId, Map<String, Object> payment) {
        String asaasSubscriptionId = stringVal(payment.get("subscription"));
        if (asaasSubscriptionId != null && !asaasSubscriptionId.isBlank()) {
            Optional<Subscription> byAsaasSub = subscriptionRepository.findByAsaasSubscriptionId(asaasSubscriptionId);
            if (byAsaasSub.isPresent()) {
                return byAsaasSub;
            }
        }

        if (externalReference != null && externalReference.startsWith("sub:")) {
            try {
                Long subId = Long.parseLong(externalReference.substring(4));
                return subscriptionRepository.findById(subId);
            } catch (NumberFormatException ignored) {
                // continua
            }
        }

        if (asaasPaymentId != null) {
            return subscriptionRepository.findByAsaasPaymentId(asaasPaymentId);
        }

        return Optional.empty();
    }

    private void registerPaymentRecord(Subscription subscription, Map<String, Object> payment, Payment.PaymentStatus status) {
        String paymentId = stringVal(payment.get("id"));
        if (paymentId == null) return;

        if (paymentRepository.findByAsaasPaymentId(paymentId).isPresent()) {
            return;
        }

        Payment record = new Payment();
        record.setSubscription(subscription);
        record.setAsaasPaymentId(paymentId);
        record.setAmount(extractBigDecimal(payment.get("value")));
        record.setStatus(status);
        record.setCurrency("BRL");
        record.setPaymentMethod(stringVal(payment.get("billingType")));
        if (status == Payment.PaymentStatus.APPROVED) {
            record.setPaidAt(LocalDateTime.now());
        } else {
            record.setFailedAt(LocalDateTime.now());
        }
        paymentRepository.save(record);
    }

    private static String stringVal(Object o) {
        return o != null ? o.toString() : null;
    }

    private static java.math.BigDecimal extractBigDecimal(Object value) {
        if (value instanceof Number n) {
            return java.math.BigDecimal.valueOf(n.doubleValue());
        }
        if (value != null) {
            return new java.math.BigDecimal(value.toString());
        }
        return java.math.BigDecimal.ZERO;
    }
}
