package br.softsistem.Gerenciamento_de_estoque.controller;

import br.softsistem.Gerenciamento_de_estoque.enumeracao.SubscriptionStatus;
import br.softsistem.Gerenciamento_de_estoque.model.Payment;
import br.softsistem.Gerenciamento_de_estoque.model.Subscription;
import br.softsistem.Gerenciamento_de_estoque.repository.PaymentRepository;
import br.softsistem.Gerenciamento_de_estoque.repository.SubscriptionRepository;
import br.softsistem.Gerenciamento_de_estoque.service.EmailService;
import br.softsistem.Gerenciamento_de_estoque.service.StripeService;
import br.softsistem.Gerenciamento_de_estoque.service.SubscriptionService;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * Controller para processar webhooks do Stripe
 */
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks", description = "Processamento de webhooks do Stripe")
public class WebhookController {
    
    private final StripeService stripeService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final EmailService emailService;
    
    /**
     * Processa webhooks do Stripe
     */
    @PostMapping("/stripe")
    @Operation(summary = "Webhook Stripe", description = "Processa eventos enviados pelo Stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            Event event = stripeService.validateWebhook(payload, sigHeader);
            
            log.info("Webhook recebido: {} - {}", event.getType(), event.getId());
            
            switch (event.getType()) {
                case "customer.subscription.created":
                    handleSubscriptionCreated(event);
                    break;
                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;
                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;
                case "invoice.payment_succeeded":
                    handleInvoicePaymentSucceeded(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                case "payment_intent.succeeded":
                    handlePaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handlePaymentIntentFailed(event);
                    break;
                default:
                    log.info("Evento não processado: {}", event.getType());
            }
            
            return ResponseEntity.ok("Webhook processado com sucesso");
            
        } catch (StripeException e) {
            log.error("Erro ao validar webhook do Stripe", e);
            return ResponseEntity.badRequest().body("Webhook inválido");
        } catch (Exception e) {
            log.error("Erro ao processar webhook", e);
            return ResponseEntity.internalServerError().body("Erro interno");
        }
    }
    
    /**
     * Processa criação de assinatura
     */
    private void handleSubscriptionCreated(Event event) {
        try {
            com.stripe.model.Subscription stripeSubscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (stripeSubscription != null) {
                String subscriptionId = stripeSubscription.getId();
                Optional<Subscription> localSubscription = subscriptionRepository.findByStripeSubscriptionId(subscriptionId);
                
                if (localSubscription.isPresent()) {
                    Subscription subscription = localSubscription.get();
                    subscription.setStripeSubscriptionId(subscriptionId);
                    
                    // Atualiza datas baseado no Stripe
                    subscription.setCurrentPeriodStart(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodStart()), 
                            ZoneId.systemDefault()
                        )
                    );
                    subscription.setCurrentPeriodEnd(
                        LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(stripeSubscription.getCurrentPeriodEnd()), 
                            ZoneId.systemDefault()
                        )
                    );
                    
                    subscriptionRepository.save(subscription);
                    log.info("Assinatura criada no Stripe: {}", subscriptionId);
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar criação de assinatura", e);
        }
    }
    
    /**
     * Processa atualização de assinatura
     */
    private void handleSubscriptionUpdated(Event event) {
        try {
            com.stripe.model.Subscription stripeSubscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (stripeSubscription != null) {
                String status = stripeSubscription.getStatus();
                SubscriptionStatus newStatus = mapStripeStatusToLocal(status);
                
                subscriptionService.updateSubscriptionFromStripe(stripeSubscription.getId(), newStatus);
                log.info("Assinatura atualizada: {} - Status: {}", stripeSubscription.getId(), status);
            }
        } catch (Exception e) {
            log.error("Erro ao processar atualização de assinatura", e);
        }
    }
    
    /**
     * Processa cancelamento de assinatura
     */
    private void handleSubscriptionDeleted(Event event) {
        try {
            com.stripe.model.Subscription stripeSubscription = 
                (com.stripe.model.Subscription) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (stripeSubscription != null) {
                subscriptionService.updateSubscriptionFromStripe(stripeSubscription.getId(), SubscriptionStatus.CANCELED);
                log.info("Assinatura cancelada: {}", stripeSubscription.getId());
            }
        } catch (Exception e) {
            log.error("Erro ao processar cancelamento de assinatura", e);
        }
    }
    
    /**
     * Processa pagamento de invoice bem-sucedido
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        try {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (invoice != null && invoice.getSubscription() != null) {
                Optional<Subscription> subscription = subscriptionRepository.findByStripeSubscriptionId(invoice.getSubscription());
                
                if (subscription.isPresent()) {
                    // Cria registro de pagamento
                    Payment payment = new Payment();
                    payment.setSubscription(subscription.get());
                    payment.setAmount(BigDecimal.valueOf(invoice.getAmountPaid()).divide(BigDecimal.valueOf(100))); // Converte de centavos
                    payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                    payment.setStripeInvoiceId(invoice.getId());
                    payment.setCurrency(invoice.getCurrency().toUpperCase());
                    payment.setPaidAt(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(invoice.getStatusTransitions().getPaidAt()), 
                        ZoneId.systemDefault()
                    ));
                    
                    paymentRepository.save(payment);
                    
                    // Atualiza status da assinatura se necessário
                    if (subscription.get().getStatus() == SubscriptionStatus.TRIAL) {
                        subscriptionService.updateSubscriptionFromStripe(invoice.getSubscription(), SubscriptionStatus.ACTIVE);
                    }
                    
                    log.info("Pagamento bem-sucedido processado: {}", invoice.getId());
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar pagamento bem-sucedido", e);
        }
    }
    
    /**
     * Processa falha de pagamento de invoice
     */
    private void handleInvoicePaymentFailed(Event event) {
        try {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (invoice != null && invoice.getSubscription() != null) {
                Optional<Subscription> subscription = subscriptionRepository.findByStripeSubscriptionId(invoice.getSubscription());
                
                if (subscription.isPresent()) {
                    // Cria registro de pagamento falhado
                    Payment payment = new Payment();
                    payment.setSubscription(subscription.get());
                    payment.setAmount(BigDecimal.valueOf(invoice.getAmountDue()).divide(BigDecimal.valueOf(100)));
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setStripeInvoiceId(invoice.getId());
                    payment.setCurrency(invoice.getCurrency().toUpperCase());
                    payment.setFailureReason("Falha no pagamento da invoice");
                    payment.setFailedAt(LocalDateTime.now());
                    
                    paymentRepository.save(payment);
                    
                    // Atualiza status da assinatura
                    subscriptionService.updateSubscriptionFromStripe(invoice.getSubscription(), SubscriptionStatus.PAST_DUE);
                    
                    // Envia email de falha de pagamento
                    emailService.sendPaymentFailedEmail(
                        subscription.get().getUser(), 
                        subscription.get().getPlan(), 
                        "Falha no pagamento da invoice"
                    );
                    
                    log.info("Falha de pagamento processada: {}", invoice.getId());
                }
            }
        } catch (Exception e) {
            log.error("Erro ao processar falha de pagamento", e);
        }
    }
    
    /**
     * Processa payment intent bem-sucedido
     */
    private void handlePaymentIntentSucceeded(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentIntent != null) {
                Optional<Payment> existingPayment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());
                
                if (existingPayment.isPresent()) {
                    Payment payment = existingPayment.get();
                    payment.setStatus(Payment.PaymentStatus.SUCCEEDED);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
                
                log.info("Payment Intent bem-sucedido: {}", paymentIntent.getId());
            }
        } catch (Exception e) {
            log.error("Erro ao processar payment intent bem-sucedido", e);
        }
    }
    
    /**
     * Processa falha de payment intent
     */
    private void handlePaymentIntentFailed(Event event) {
        try {
            PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
            
            if (paymentIntent != null) {
                Optional<Payment> existingPayment = paymentRepository.findByStripePaymentIntentId(paymentIntent.getId());
                
                if (existingPayment.isPresent()) {
                    Payment payment = existingPayment.get();
                    payment.setStatus(Payment.PaymentStatus.FAILED);
                    payment.setFailureReason(paymentIntent.getLastPaymentError() != null ? 
                        paymentIntent.getLastPaymentError().getMessage() : "Falha desconhecida");
                    payment.setFailedAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
                
                log.info("Payment Intent falhado: {}", paymentIntent.getId());
            }
        } catch (Exception e) {
            log.error("Erro ao processar falha de payment intent", e);
        }
    }
    
    /**
     * Mapeia status do Stripe para status local
     */
    private SubscriptionStatus mapStripeStatusToLocal(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> SubscriptionStatus.ACTIVE;
            case "trialing" -> SubscriptionStatus.TRIAL;
            case "canceled" -> SubscriptionStatus.CANCELED;
            case "past_due" -> SubscriptionStatus.PAST_DUE;
            case "incomplete" -> SubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" -> SubscriptionStatus.INCOMPLETE_EXPIRED;
            default -> SubscriptionStatus.EXPIRED;
        };
    }
}